package com.defnf.syndicate.data.local

import com.defnf.syndicate.data.local.dao.ArticleWithReadStatus
import com.defnf.syndicate.data.models.Article

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