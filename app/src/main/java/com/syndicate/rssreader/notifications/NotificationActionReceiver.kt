package com.syndicate.rssreader.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.syndicate.rssreader.data.repository.RssRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var repository: RssRepository
    
    @Inject
    lateinit var notificationManager: NotificationManager
    
    companion object {
        const val ACTION_MARK_AS_READ = "com.syndicate.rssreader.MARK_AS_READ"
        const val ACTION_MARK_GROUP_AS_READ = "com.syndicate.rssreader.MARK_GROUP_AS_READ"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_AS_READ -> {
                val articleId = intent.getStringExtra("articleId")
                if (articleId != null) {
                    markArticleAsRead(articleId)
                }
            }
            ACTION_MARK_GROUP_AS_READ -> {
                val groupId = intent.getLongExtra("groupId", -1)
                if (groupId != -1L) {
                    markGroupAsRead(groupId)
                }
            }
        }
    }
    
    private fun markArticleAsRead(articleId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.markAsRead(articleId)
                // We need to get the feedId to dismiss the notification
                // Since we don't have a direct method, we'll just mark as read
                // The notification will be updated on next sync or manually dismissed
            } catch (e: Exception) {
                // Silently handle error as per requirements
            }
        }
    }
    
    private fun markGroupAsRead(groupId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.markAllAsReadForGroup(groupId)
                // Dismiss the notification for this group
                notificationManager.dismissGroupNotification(groupId)
            } catch (e: Exception) {
                // Silently handle error as per requirements
            }
        }
    }
}