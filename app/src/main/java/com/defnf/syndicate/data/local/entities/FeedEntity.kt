package com.defnf.syndicate.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.defnf.syndicate.data.models.Feed

@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)]
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "url")
    val url: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String?,
    
    @ColumnInfo(name = "site_url")
    val siteUrl: String?,
    
    @ColumnInfo(name = "favicon_url")
    val faviconUrl: String?,
    
    @ColumnInfo(name = "last_fetched")
    val lastFetched: Long?,
    
    @ColumnInfo(name = "is_available", defaultValue = "1")
    val isAvailable: Boolean = true,
    
    @ColumnInfo(name = "notifications_enabled", defaultValue = "0")
    val notificationsEnabled: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Feed = Feed(
        id = id,
        url = url,
        title = title,
        description = description,
        siteUrl = siteUrl,
        faviconUrl = faviconUrl,
        lastFetched = lastFetched,
        isAvailable = isAvailable,
        notificationsEnabled = notificationsEnabled,
        createdAt = createdAt
    )
}