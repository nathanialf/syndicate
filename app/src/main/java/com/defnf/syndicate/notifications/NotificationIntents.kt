package com.defnf.syndicate.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.defnf.syndicate.ui.MainActivity
import com.defnf.syndicate.ui.NotificationData

/**
 * Builds and parses the intents behind notification taps and actions.
 *
 * Every intent carries a data URI unique to its target. PendingIntents are matched by
 * [Intent.filterEquals], which ignores extras, so without a distinct URI all notifications
 * would share one PendingIntent and tapping any of them would open whichever target was posted last.
 */
object NotificationIntents {
    private const val SCHEME = "syndicate"
    private const val HOST_ARTICLE = "article"
    private const val HOST_FEED = "feed"
    private const val HOST_GROUP = "group"

    const val EXTRA_ARTICLE_ID = "articleId"
    const val EXTRA_FEED_ID = "feedId"
    const val EXTRA_GROUP_ID = "groupId"

    fun openArticle(context: Context, feedId: Long, articleId: String): Intent =
        activityIntent(context, targetUri(HOST_ARTICLE, articleId))
            .putExtra(EXTRA_FEED_ID, feedId)
            .putExtra(EXTRA_ARTICLE_ID, articleId)

    fun openFeed(context: Context, feedId: Long): Intent =
        activityIntent(context, targetUri(HOST_FEED, feedId.toString()))
            .putExtra(EXTRA_FEED_ID, feedId)

    fun openGroup(context: Context, groupId: Long): Intent =
        activityIntent(context, targetUri(HOST_GROUP, groupId.toString()))
            .putExtra(EXTRA_GROUP_ID, groupId)

    fun markArticleAsRead(context: Context, articleId: String): Intent =
        receiverIntent(context, NotificationActionReceiver.ACTION_MARK_AS_READ, targetUri(HOST_ARTICLE, articleId))
            .putExtra(EXTRA_ARTICLE_ID, articleId)

    fun markFeedAsRead(context: Context, feedId: Long): Intent =
        receiverIntent(context, NotificationActionReceiver.ACTION_MARK_FEED_AS_READ, targetUri(HOST_FEED, feedId.toString()))
            .putExtra(EXTRA_FEED_ID, feedId)

    fun markGroupAsRead(context: Context, groupId: Long): Intent =
        receiverIntent(context, NotificationActionReceiver.ACTION_MARK_GROUP_AS_READ, targetUri(HOST_GROUP, groupId.toString()))
            .putExtra(EXTRA_GROUP_ID, groupId)

    /** Returns the navigation target of a notification tap, or null if [intent] did not come from one. */
    fun parse(intent: Intent?): NotificationData? {
        if (intent == null) return null
        val articleId = intent.getStringExtra(EXTRA_ARTICLE_ID)
        val feedId = intent.getLongExtra(EXTRA_FEED_ID, -1L)
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        return when {
            articleId != null && feedId != -1L -> NotificationData.Article(articleId = articleId, feedId = feedId)
            feedId != -1L -> NotificationData.Feed(feedId = feedId)
            groupId != -1L -> NotificationData.Group(groupId = groupId)
            else -> null
        }
    }

    private fun activityIntent(context: Context, data: Uri): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.data = data
            // Reuse the running activity (delivered to onNewIntent) instead of tearing down the task
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    private fun receiverIntent(context: Context, action: String, data: Uri): Intent =
        Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            this.data = data
        }

    private fun targetUri(host: String, id: String): Uri =
        Uri.Builder().scheme(SCHEME).authority(host).appendPath(id).build()
}
