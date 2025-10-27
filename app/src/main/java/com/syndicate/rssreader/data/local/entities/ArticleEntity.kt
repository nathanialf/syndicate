package com.syndicate.rssreader.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    indices = [Index(value = ["feed_id"]), Index(value = ["published_date"])],
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArticleEntity(
    @PrimaryKey
    val id: String, // GUID from RSS feed
    
    @ColumnInfo(name = "feed_id")
    val feedId: Long,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String?,
    
    @ColumnInfo(name = "url")
    val url: String,
    
    @ColumnInfo(name = "author")
    val author: String?,
    
    @ColumnInfo(name = "published_date")
    val publishedDate: Long?,
    
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,
    
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis()
)