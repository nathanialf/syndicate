package com.syndicate.rssreader.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syndicate.rssreader.data.repository.RssRepository
import com.syndicate.rssreader.notifications.NotificationManager
import com.syndicate.rssreader.data.local.entities.ArticleEntity
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
            
            when {
                feedId != -1L -> {
                    Log.d("SyncWorker", "Starting targeted sync for feed: $feedId")
                    syncSingleFeed(feedId)
                    Log.d("SyncWorker", "Targeted feed sync completed successfully")
                }
                groupId != -1L -> {
                    Log.d("SyncWorker", "Starting targeted sync for group: $groupId")
                    syncFeedsInGroup(groupId)
                    Log.d("SyncWorker", "Targeted group sync completed successfully")
                }
                else -> {
                    Log.d("SyncWorker", "Starting background sync for all feeds")
                    syncAllFeeds()
                    Log.d("SyncWorker", "Background sync completed successfully")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncAllFeeds() {
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
                    
                    // Send notifications for new articles ONLY if notifications are enabled for this feed
                    if (feed.notificationsEnabled && newArticles.isNotEmpty()) {
                        Log.d("SyncWorker", "Feed has notifications enabled, sending ${newArticles.size} notifications")
                        for (articleEntity in newArticles) {
                            val article = com.syndicate.rssreader.data.models.Article(
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
                        Log.d("SyncWorker", "Feed has notifications disabled, skipping ${newArticles.size} notifications")
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync feed: ${feed.title} - ${e.message}", e)
                // Continue with other feeds if one fails
            }
        }

        // Handle group notifications separately
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
    }
    
    private suspend fun syncSingleFeed(feedId: Long) {
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
                
                // Only send notifications if the feed has notifications enabled
                if (feed.notificationsEnabled) {
                    // Send notifications for new articles
                    for (articleEntity in newArticles) {
                        val article = com.syndicate.rssreader.data.models.Article(
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
    
    private suspend fun syncFeedsInGroup(groupId: Long) {
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
                        
                        // Only send notifications if the feed has notifications enabled
                        if (feed.notificationsEnabled) {
                            // Send notifications for new articles
                            for (articleEntity in newArticles) {
                                val article = com.syndicate.rssreader.data.models.Article(
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