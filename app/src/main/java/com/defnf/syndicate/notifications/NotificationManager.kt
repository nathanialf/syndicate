package com.defnf.syndicate.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.defnf.syndicate.R
import com.defnf.syndicate.data.local.entities.ArticleEntity
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.preferences.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts and dismisses new-article notifications.
 *
 * Each new article gets its own notification (tag `article:<id>`) that opens that article. A feed's
 * article notifications are bundled under a summary (tag `feed:<id>`) that opens the feed, and only
 * the summary alerts, so a sync makes at most one sound per feed. Group notifications use tag
 * `group:<id>` and open the group. Tags keep ids from colliding between feeds, groups and articles.
 */
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationPreferences: NotificationPreferences
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val systemNotificationManager: android.app.NotificationManager =
        context.getSystemService(android.app.NotificationManager::class.java)

    companion object {
        const val FEED_CHANNEL_ID = "feed_notifications"
        const val GROUP_CHANNEL_ID = "group_notifications"

        // Notifications are identified by tag; ids only distinguish the kind of notification
        private const val ARTICLE_NOTIFICATION_ID = 1
        private const val FEED_SUMMARY_NOTIFICATION_ID = 2
        private const val GROUP_NOTIFICATION_ID = 3

        // Newest articles from one sync that get their own notification; the rest are counted in the summary
        private const val MAX_ARTICLE_NOTIFICATIONS_PER_FEED = 5
        private const val MAX_SUMMARY_LINES = 5

        private const val ARTICLE_TAG_PREFIX = "article:"

        private fun articleTag(articleId: String) = "$ARTICLE_TAG_PREFIX$articleId"
        private fun feedTag(feedId: Long) = "feed:$feedId"
        private fun groupTag(groupId: Long) = "group:$groupId"
        private fun feedGroupKey(feedId: Long) = "com.defnf.syndicate.feed.$feedId"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val feedChannel = NotificationChannel(
            FEED_CHANNEL_ID,
            "Feed Notifications",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new articles from individual feeds"
        }

        val groupChannel = NotificationChannel(
            GROUP_CHANNEL_ID,
            "Group Notifications",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new articles from feed groups"
        }

        systemNotificationManager.createNotificationChannel(feedChannel)
        systemNotificationManager.createNotificationChannel(groupChannel)
    }

    /** Notifies about articles that a sync found for [feed]: one notification per article plus a feed summary. */
    suspend fun showNewArticlesNotification(feed: Feed, newArticles: List<ArticleEntity>) {
        if (newArticles.isEmpty() || !canPostNotifications()) return

        val groupKey = feedGroupKey(feed.id)
        val newestFirst = newArticles.sortedByDescending { it.publishedDate ?: it.fetchedAt }
        val notifiedArticles = newestFirst.take(MAX_ARTICLE_NOTIFICATIONS_PER_FEED)
        val notifiedTags: Set<String?> = notifiedArticles.map { articleTag(it.id) }.toSet()

        // Article notifications from earlier syncs that are still showing count towards the summary
        val earlierArticleCount = activeNotifications().count { sbn ->
            sbn.notification.group == groupKey && sbn.isArticleNotification() && sbn.tag !in notifiedTags
        }
        val totalCount = earlierArticleCount + newArticles.size

        Log.d("NotificationManager", "Posting ${notifiedArticles.size} of ${newArticles.size} new article notifications for ${feed.title}")

        notifiedArticles.forEach { article ->
            val notification = NotificationCompat.Builder(context, FEED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_news)
                .setContentTitle(feed.title)
                .setContentText(article.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(article.title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setWhen(article.publishedDate ?: article.fetchedAt)
                .setShowWhen(true)
                .setContentIntent(activityPendingIntent(NotificationIntents.openArticle(context, feed.id, article.id)))
                .addAction(
                    R.drawable.ic_check,
                    "Mark as Read",
                    broadcastPendingIntent(NotificationIntents.markArticleAsRead(context, article.id))
                )
                .setGroup(groupKey)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setAutoCancel(true)
                .build()
            notify(articleTag(article.id), ARTICLE_NOTIFICATION_ID, notification)
        }

        val inboxStyle = NotificationCompat.InboxStyle().setSummaryText(feed.title)
        newestFirst.take(MAX_SUMMARY_LINES).forEach { inboxStyle.addLine(it.title) }

        val summary = NotificationCompat.Builder(context, FEED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_news)
            .setContentTitle(feed.title)
            .setContentText(if (totalCount == 1) newestFirst.first().title else "$totalCount new articles")
            .setStyle(inboxStyle)
            .setNumber(totalCount)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(activityPendingIntent(NotificationIntents.openFeed(context, feed.id)))
            .addAction(
                R.drawable.ic_check,
                "Mark All as Read",
                broadcastPendingIntent(NotificationIntents.markFeedAsRead(context, feed.id))
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .build()
        notify(feedTag(feed.id), FEED_SUMMARY_NOTIFICATION_ID, summary)
    }

    /** Notifies about new articles in a feed group; tapping opens the group. */
    suspend fun showGroupNotification(
        groupId: Long,
        groupName: String,
        newArticleTitles: List<String>,
        unreadCount: Int
    ) {
        if (newArticleTitles.isEmpty() || !canPostNotifications()) return

        val newCount = newArticleTitles.size
        val contentText = when {
            newCount == 1 -> newArticleTitles.first()
            unreadCount > newCount -> "$newCount new articles ($unreadCount unread)"
            else -> "$newCount new articles"
        }
        val inboxStyle = NotificationCompat.InboxStyle().setSummaryText(groupName)
        newArticleTitles.take(MAX_SUMMARY_LINES).forEach { inboxStyle.addLine(it) }

        val notification = NotificationCompat.Builder(context, GROUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_news)
            .setContentTitle(groupName)
            .setContentText(contentText)
            .setStyle(inboxStyle)
            .setNumber(newCount)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(activityPendingIntent(NotificationIntents.openGroup(context, groupId)))
            .addAction(
                R.drawable.ic_check,
                if (newCount == 1) "Mark as Read" else "Mark All as Read",
                broadcastPendingIntent(NotificationIntents.markGroupAsRead(context, groupId))
            )
            .setAutoCancel(true)
            .build()
        notify(groupTag(groupId), GROUP_NOTIFICATION_ID, notification)
    }

    /** Removes an article's notification, and its feed summary if no other articles from that feed remain. */
    fun dismissArticleNotification(articleId: String) {
        val tag = articleTag(articleId)
        val active = activeNotifications()
        val dismissed = active.firstOrNull { it.tag == tag } ?: return
        notificationManager.cancel(tag, ARTICLE_NOTIFICATION_ID)

        val groupKey = dismissed.notification.group ?: return
        val othersRemain = active.any { sbn ->
            sbn.tag != tag && sbn.isArticleNotification() && sbn.notification.group == groupKey
        }
        if (!othersRemain) {
            active.filter { it.notification.group == groupKey }
                .forEach { notificationManager.cancel(it.tag, it.id) }
        }
    }

    /** Removes a feed's summary and all of its article notifications. */
    fun dismissFeedNotifications(feedId: Long) {
        val groupKey = feedGroupKey(feedId)
        activeNotifications()
            .filter { it.notification.group == groupKey }
            .forEach { notificationManager.cancel(it.tag, it.id) }
        notificationManager.cancel(feedTag(feedId), FEED_SUMMARY_NOTIFICATION_ID)
    }

    fun dismissGroupNotification(groupId: Long) {
        notificationManager.cancel(groupTag(groupId), GROUP_NOTIFICATION_ID)
    }

    fun dismissAllNotifications() {
        notificationManager.cancelAll()
    }

    private suspend fun canPostNotifications(): Boolean {
        val globalEnabled = notificationPreferences.notificationsEnabled.first()
        val systemEnabled = notificationManager.areNotificationsEnabled()
        if (!globalEnabled || !systemEnabled) {
            Log.w("NotificationManager", "Notifications disabled - global: $globalEnabled, system: $systemEnabled")
        }
        return globalEnabled && systemEnabled
    }

    private fun notify(tag: String, id: Int, notification: Notification) {
        try {
            notificationManager.notify(tag, id, notification)
        } catch (e: SecurityException) {
            // Notification permission was revoked between the check and posting
            Log.w("NotificationManager", "Unable to post notification $tag", e)
        }
    }

    private fun activeNotifications(): List<StatusBarNotification> = try {
        systemNotificationManager.activeNotifications.toList()
    } catch (e: Exception) {
        Log.w("NotificationManager", "Unable to read active notifications", e)
        emptyList()
    }

    private fun StatusBarNotification.isArticleNotification(): Boolean =
        tag?.startsWith(ARTICLE_TAG_PREFIX) == true

    private fun activityPendingIntent(intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun broadcastPendingIntent(intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
