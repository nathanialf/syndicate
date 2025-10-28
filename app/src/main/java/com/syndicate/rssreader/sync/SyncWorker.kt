package com.syndicate.rssreader.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syndicate.rssreader.data.repository.RssRepository
import com.syndicate.rssreader.notifications.NotificationManager
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
            syncAllFeeds()
            Result.success()
        } catch (e: Exception) {
            // Silently handle errors as per requirements
            Result.retry()
        }
    }

    private suspend fun syncAllFeeds() {
        // Get all feeds with notifications enabled
        val feeds = repository.getAllFeeds().first()
        val notificationEnabledFeeds = feeds.filter { it.notificationsEnabled }

        for (feed in notificationEnabledFeeds) {
            try {
                val refreshResult = repository.refreshFeed(feed.id)
                if (refreshResult.isSuccess) {
                    // Note: We would need to track which articles are new
                    // For now, we'll skip showing individual article notifications during sync
                    // This would need to be enhanced to track new articles properly
                }
            } catch (e: Exception) {
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
}