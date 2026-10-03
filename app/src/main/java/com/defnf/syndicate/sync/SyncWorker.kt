package com.defnf.syndicate.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.repository.RssRepository
import com.defnf.syndicate.notifications.NotificationManager
import com.defnf.syndicate.data.local.entities.ArticleEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: RssRepository,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val feedId = inputData.getLong("feedId", -1L)
            val groupId = inputData.getLong("groupId", -1L)
            val isManualSync = inputData.getBoolean("isManualSync", false)
            val isLaunchSync = inputData.getBoolean("isLaunchSync", false)
            val shouldSendNotifications = !isManualSync && !isLaunchSync
            
            Log.d("SyncWorker", "=== SYNC WORKER START ===")
            Log.d("SyncWorker", "Input data - Manual: $isManualSync, Launch: $isLaunchSync")
            Log.d("SyncWorker", "Calculated shouldSendNotifications: $shouldSendNotifications")
            Log.d("SyncWorker", "FeedId: $feedId, GroupId: $groupId")
            
            when {
                feedId != -1L -> {
                    Log.d("SyncWorker", "Starting targeted sync for feed: $feedId")
                    syncSingleFeed(feedId, shouldSendNotifications)
                    Log.d("SyncWorker", "Targeted feed sync completed successfully")
                }
                groupId != -1L -> {
                    Log.d("SyncWorker", "Starting targeted sync for group: $groupId")
                    syncFeedsInGroup(groupId, shouldSendNotifications)
                    Log.d("SyncWorker", "Targeted group sync completed successfully")
                }
                else -> {
                    val syncType = when {
                        isManualSync -> "manual"
                        isLaunchSync -> "launch"
                        else -> "background"
                    }
                    Log.d("SyncWorker", "Starting $syncType sync for all feeds")
                    syncAllFeeds(shouldSendNotifications)
                    Log.d("SyncWorker", "${syncType.replaceFirstChar { it.uppercase() }} sync completed successfully")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncAllFeeds(shouldSendNotifications: Boolean = true) {
        // Get ALL feeds - sync regardless of notification settings
        val feeds = repository.getAllFeeds().first()
        
        Log.d("SyncWorker", "Found ${feeds.size} total feeds to sync")

        val newArticlesByFeed = mutableMapOf<Long, List<ArticleEntity>>()
        for (feed in feeds) {
            try {
                newArticlesByFeed[feed.id] = syncFeed(feed, shouldSendNotifications)
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync feed: ${feed.title} - ${e.message}", e)
                // Continue with other feeds if one fails
            }
        }

        // Handle group notifications separately - only for background sync
        if (shouldSendNotifications) {
            notifyGroups(newArticlesByFeed)
        } else {
            Log.d("SyncWorker", "Skipping group notifications - manual/launch sync")
        }
    }
    
    private suspend fun syncSingleFeed(feedId: Long, shouldSendNotifications: Boolean = true) {
        try {
            val feed = repository.getFeedById(feedId)
            if (feed == null) {
                Log.w("SyncWorker", "Feed not found: $feedId")
                return
            }
            syncFeed(feed, shouldSendNotifications)
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync feed: $feedId - ${e.message}", e)
        }
    }
    
    private suspend fun syncFeedsInGroup(groupId: Long, shouldSendNotifications: Boolean = true) {
        try {
            val feedsInGroup = repository.getFeedsByGroup(groupId).first()
            Log.d("SyncWorker", "Syncing ${feedsInGroup.size} feeds in group: $groupId")
            
            val newArticlesByFeed = mutableMapOf<Long, List<ArticleEntity>>()
            for (feed in feedsInGroup) {
                try {
                    newArticlesByFeed[feed.id] = syncFeed(feed, shouldSendNotifications)
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync feed in group: ${feed.title} - ${e.message}", e)
                    // Continue with other feeds if one fails
                }
            }
            
            if (shouldSendNotifications) {
                notifyGroups(newArticlesByFeed)
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync feeds in group: $groupId - ${e.message}", e)
        }
    }
    
    /**
     * Refreshes one feed and, if allowed, notifies about its new articles.
     * Returns the newly inserted articles (empty if the refresh failed).
     */
    private suspend fun syncFeed(feed: Feed, shouldSendNotifications: Boolean): List<ArticleEntity> {
        Log.d("SyncWorker", "Syncing feed: ${feed.title}")
        val newArticles = repository.refreshFeedAndGetNewArticles(feed.id).getOrElse { error ->
            Log.e("SyncWorker", "Failed to sync feed: ${feed.title} - ${error.message}")
            return emptyList()
        }
        Log.d("SyncWorker", "Found ${newArticles.size} new articles for feed: ${feed.title}")
        
        if (newArticles.isEmpty()) return newArticles
        
        // Send notifications ONLY if enabled for this feed AND this is not a manual/launch sync
        if (shouldSendNotifications && feed.notificationsEnabled) {
            notificationManager.showNewArticlesNotification(feed, newArticles)
        } else {
            val reason = if (!shouldSendNotifications) "manual/launch sync" else "notifications disabled for feed"
            Log.d("SyncWorker", "Skipping ${newArticles.size} notifications - reason: $reason")
        }
        return newArticles
    }
    
    /** Notifies groups with notifications enabled that received new articles in this sync. */
    private suspend fun notifyGroups(newArticlesByFeed: Map<Long, List<ArticleEntity>>) {
        if (newArticlesByFeed.values.all { it.isEmpty() }) return
        
        val notificationEnabledGroups = repository.getAllGroups().first().filter { it.notificationsEnabled }
        for (group in notificationEnabledGroups) {
            try {
                val newInGroup = repository.getFeedsForGroup(group.id)
                    .flatMap { feed -> newArticlesByFeed[feed.id].orEmpty() }
                    .sortedByDescending { it.publishedDate ?: it.fetchedAt }
                if (newInGroup.isEmpty()) continue
                
                notificationManager.showGroupNotification(
                    groupId = group.id,
                    groupName = group.name,
                    newArticleTitles = newInGroup.map { it.title },
                    unreadCount = repository.getUnreadCountForGroup(group.id)
                )
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to notify group: ${group.name} - ${e.message}", e)
                // Continue with other groups if one fails
            }
        }
    }
}
