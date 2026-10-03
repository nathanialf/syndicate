package com.defnf.syndicate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.defnf.syndicate.data.repository.RssRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the "Mark as Read" actions on notifications. Marking articles read through the
 * repository also dismisses their notifications.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: RssRepository

    companion object {
        const val ACTION_MARK_AS_READ = "com.defnf.syndicate.MARK_AS_READ"
        const val ACTION_MARK_FEED_AS_READ = "com.defnf.syndicate.MARK_FEED_AS_READ"
        const val ACTION_MARK_GROUP_AS_READ = "com.defnf.syndicate.MARK_GROUP_AS_READ"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_MARK_AS_READ && action != ACTION_MARK_FEED_AS_READ && action != ACTION_MARK_GROUP_AS_READ) return
        
        // Keep the receiver alive until the database write finishes
        val pendingResult = goAsync()
        scope.launch {
            try {
                handleAction(action, intent)
            } catch (e: Exception) {
                Log.e("NotificationActionReceiver", "Failed to handle $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun handleAction(action: String?, intent: Intent) {
        when (action) {
            ACTION_MARK_AS_READ -> {
                intent.getStringExtra(NotificationIntents.EXTRA_ARTICLE_ID)?.let { articleId ->
                    repository.markAsRead(articleId)
                }
            }
            ACTION_MARK_FEED_AS_READ -> {
                val feedId = intent.getLongExtra(NotificationIntents.EXTRA_FEED_ID, -1L)
                if (feedId != -1L) repository.markAllAsReadForFeed(feedId)
            }
            ACTION_MARK_GROUP_AS_READ -> {
                val groupId = intent.getLongExtra(NotificationIntents.EXTRA_GROUP_ID, -1L)
                if (groupId != -1L) repository.markAllAsReadForGroup(groupId)
            }
        }
    }
}
