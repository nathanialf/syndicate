package com.defnf.syndicate.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.defnf.syndicate.R
import com.defnf.syndicate.data.models.Article
import com.defnf.syndicate.data.preferences.NotificationPreferences
import com.defnf.syndicate.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationPreferences: NotificationPreferences
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    
    companion object {
        const val FEED_CHANNEL_ID = "feed_notifications"
        const val GROUP_CHANNEL_ID = "group_notifications"
        
        private const val FEED_NOTIFICATION_BASE_ID = 10000
        private const val GROUP_NOTIFICATION_BASE_ID = 20000
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val feedChannel = NotificationChannel(
                FEED_CHANNEL_ID,
                "Feed Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new articles from individual feeds"
            }
            
            val groupChannel = NotificationChannel(
                GROUP_CHANNEL_ID,
                "Group Notifications", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new articles from feed groups"
            }
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(feedChannel)
            systemNotificationManager.createNotificationChannel(groupChannel)
        }
    }
    
    fun showFeedNotification(feedTitle: String, article: Article) {
        Log.d("NotificationManager", "Attempting to show notification for: $feedTitle - ${article.title}")
        
        // Check global notification setting
        val globalEnabled = runBlocking { notificationPreferences.notificationsEnabled.first() }
        Log.d("NotificationManager", "Global notifications enabled: $globalEnabled")
        Log.d("NotificationManager", "System notifications enabled: ${notificationManager.areNotificationsEnabled()}")
        
        if (!globalEnabled || !notificationManager.areNotificationsEnabled()) {
            Log.w("NotificationManager", "Notifications disabled - global: $globalEnabled, system: ${notificationManager.areNotificationsEnabled()}")
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("feedId", article.feedId)
            putExtra("articleId", article.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val markAsReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_READ
            putExtra("articleId", article.id)
        }
        
        val markAsReadPendingIntent = PendingIntent.getBroadcast(
            context,
            article.id.hashCode(),
            markAsReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, FEED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_news)
            .setContentTitle(feedTitle)
            .setContentText(article.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(article.title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Mark as Read",
                markAsReadPendingIntent
            )
            .setAutoCancel(true)
            .build()
        
        val notificationId = FEED_NOTIFICATION_BASE_ID + article.feedId.toInt()
        Log.d("NotificationManager", "Posting notification with ID: $notificationId")
        notificationManager.notify(notificationId, notification)
        Log.d("NotificationManager", "Notification posted successfully")
    }
    
    fun showGroupNotification(groupName: String, groupId: Long, articleCount: Int, sampleArticle: Article? = null) {
        // Check global notification setting
        val globalEnabled = runBlocking { notificationPreferences.notificationsEnabled.first() }
        if (!globalEnabled || !notificationManager.areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("groupId", groupId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val markGroupAsReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_GROUP_AS_READ
            putExtra("groupId", groupId)
        }
        
        val markGroupAsReadPendingIntent = PendingIntent.getBroadcast(
            context,
            groupId.toInt(),
            markGroupAsReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = if (articleCount == 1 && sampleArticle != null) {
            // Single article notification
            NotificationCompat.Builder(context, GROUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_news)
                .setContentTitle(groupName)
                .setContentText(sampleArticle.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(sampleArticle.title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_check,
                    "Mark as Read",
                    markGroupAsReadPendingIntent
                )
                .setAutoCancel(true)
                .build()
        } else {
            // Multiple articles notification
            NotificationCompat.Builder(context, GROUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_news)
                .setContentTitle(groupName)
                .setContentText("$articleCount new articles")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_check,
                    "Mark All as Read",
                    markGroupAsReadPendingIntent
                )
                .setAutoCancel(true)
                .build()
        }
        
        val notificationId = GROUP_NOTIFICATION_BASE_ID + groupId.toInt()
        notificationManager.notify(notificationId, notification)
    }
    
    fun dismissFeedNotification(feedId: Long) {
        val notificationId = FEED_NOTIFICATION_BASE_ID + feedId.toInt()
        notificationManager.cancel(notificationId)
    }
    
    fun dismissGroupNotification(groupId: Long) {
        val notificationId = GROUP_NOTIFICATION_BASE_ID + groupId.toInt()
        notificationManager.cancel(notificationId)
    }
}