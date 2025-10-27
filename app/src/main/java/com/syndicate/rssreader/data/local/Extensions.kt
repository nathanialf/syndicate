package com.syndicate.rssreader.data.local

import com.syndicate.rssreader.data.local.dao.ArticleWithReadStatus
import com.syndicate.rssreader.data.models.Article

fun ArticleWithReadStatus.toDomain(): Article = Article(
    id = id,
    feedId = feedId,
    feedTitle = feedTitle,
    feedFaviconUrl = feedFaviconUrl,
    title = title,
    description = description,
    url = url,
    author = author,
    publishedDate = publishedDate,
    thumbnailUrl = thumbnailUrl,
    isRead = isRead,
    readAt = readAt
)