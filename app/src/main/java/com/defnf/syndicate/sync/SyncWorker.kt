package com.defnf.syndicate.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        for (feed in feeds) {
            try {
                Log.d("SyncWorker", "Syncing feed: ${feed.title}")
                val newArticlesResult = repository.refreshFeedAndGetNewArticles(feed.id)
                if (newArticlesResult.isSuccess) {
                    val newArticles = newArticlesResult.getOrThrow()
                    Log.d("SyncWorker", "Found ${newArticles.size} new articles for feed: ${feed.title}")
                    
                    // Send notifications for new articles ONLY if notifications are enabled for this feed AND this is not a manual/launch sync
                    if (shouldSendNotifications && feed.notificationsEnabled && newArticles.isNotEmpty()) {
                        Log.d("SyncWorker", "Feed has notifications enabled, sending ${newArticles.size} notifications")
                        for (articleEntity in newArticles) {
                            val article = com.defnf.syndicate.data.models.Article(
                                id = articleEntity.id,
                                feedId = articleEntity.feedId,
                                feedTitle = feed.title,
                                feedFaviconUrl = feed.faviconUrl,
                                title = articleEntity.title,
                                description = articleEntity.description,
                                url = articleEntity.url,
                                author = articleEntity.author,
                                publishedDate = articleEntity.publishedDate,
                                thumbnailUrl = articleEntity.thumbnailUrl,
                                isRead = false, // New articles are unread
                                readAt = null
                            )
                            
                            Log.d("SyncWorker", "Sending notification for article: ${article.title}")
                            notificationManager.showFeedNotification(
                                feedTitle = feed.title,
                                article = article
                            )
                        }
                    } else if (newArticles.isNotEmpty()) {
                        val reason = when {
                            !shouldSendNotifications -> "manual/launch sync"
                            !feed.notificationsEnabled -> "notifications disabled for feed"
                            else -> "unknown reason"
                        }
                        Log.d("SyncWorker", "Skipping ${newArticles.size} notifications - reason: $reason")
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync feed: ${feed.title} - ${e.message}", e)
                // Continue with other feeds if one fails
            }
        }

        // Handle group notifications separately - only for background sync
        if (shouldSendNotifications) {
            val groups = repository.getAllGroups().first()
            val notificationEnabledGroups = groups.filter { it.notificationsEnabled }

            for (group in notificationEnabledGroups) {
                try {
                    // Use the existing method that gets unread count for the entire group
                    val totalUnreadCount = repository.getUnreadCountForGroup(group.id)

                    // Show group notification if there are unread articles
                    if (totalUnreadCount > 0) {
                        notificationManager.showGroupNotification(
                            groupName = group.name,
                            groupId = group.id,
                            articleCount = totalUnreadCount,
                            sampleArticle = null // We don't have easy access to sample articles
                        )
                    }
                } catch (e: Exception) {
                    // Continue with other groups if one fails
                }
            }
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
            
            Log.d("SyncWorker", "Syncing single feed: ${feed.title}")
            val newArticlesResult = repository.refreshFeedAndGetNewArticles(feed.id)
            if (newArticlesResult.isSuccess) {
                val newArticles = newArticlesResult.getOrThrow()
                Log.d("SyncWorker", "Found ${newArticles.size} new articles for feed: ${feed.title}")
                
                // Only send notifications if the feed has notifications enabled AND this is not a manual/launch sync
                if (shouldSendNotifications && feed.notificationsEnabled) {
                    // Send notifications for new articles
                    for (articleEntity in newArticles) {
                        val article = com.defnf.syndicate.data.models.Article(
                            id = articleEntity.id,
                            feedId = articleEntity.feedId,
                            feedTitle = feed.title,
                            feedFaviconUrl = feed.faviconUrl,
                            title = articleEntity.title,
                            description = articleEntity.description,
                            url = articleEntity.url,
                            author = articleEntity.author,
                            publishedDate = articleEntity.publishedDate,
                            thumbnailUrl = articleEntity.thumbnailUrl,
                            isRead = false, // New articles are unread
                            readAt = null
                        )
                        
                        Log.d("SyncWorker", "Sending notification for article: ${article.title}")
                        notificationManager.showFeedNotification(
                            feedTitle = feed.title,
                            article = article
                        )
                    }
                }
            } else {
                Log.e("SyncWorker", "Failed to sync feed: ${feed.title} - ${newArticlesResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync feed: $feedId - ${e.message}", e)
        }
    }
    
    private suspend fun syncFeedsInGroup(groupId: Long, shouldSendNotifications: Boolean = true) {
        try {
            val feedsInGroup = repository.getFeedsByGroup(groupId).first()
            Log.d("SyncWorker", "Syncing ${feedsInGroup.size} feeds in group: $groupId")
            
            for (feed in feedsInGroup) {
                try {
                    Log.d("SyncWorker", "Syncing feed in group: ${feed.title}")
                    val newArticlesResult = repository.refreshFeedAndGetNewArticles(feed.id)
                    if (newArticlesResult.isSuccess) {
                        val newArticles = newArticlesResult.getOrThrow()
                        Log.d("SyncWorker", "Found ${newArticles.size} new articles for feed: ${feed.title}")
                        
                        // Only send notifications if the feed has notifications enabled AND this is not a manual/launch sync
                        if (shouldSendNotifications && feed.notificationsEnabled) {
                            // Send notifications for new articles
                            for (articleEntity in newArticles) {
                                val article = com.defnf.syndicate.data.models.Article(
                                    id = articleEntity.id,
                                    feedId = articleEntity.feedId,
                                    feedTitle = feed.title,
                                    feedFaviconUrl = feed.faviconUrl,
                                    title = articleEntity.title,
                                    description = articleEntity.description,
                                    url = articleEntity.url,
                                    author = articleEntity.author,
                                    publishedDate = articleEntity.publishedDate,
                                    thumbnailUrl = articleEntity.thumbnailUrl,
                                    isRead = false, // New articles are unread
                                    readAt = null
                                )
                                
                                Log.d("SyncWorker", "Sending notification for article: ${article.title}")
                                notificationManager.showFeedNotification(
                                    feedTitle = feed.title,
                                    article = article
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync feed in group: ${feed.title} - ${e.message}", e)
                    // Continue with other feeds if one fails
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync feeds in group: $groupId - ${e.message}", e)
        }
    }
}