package com.defnf.syndicate.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "read_status",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadStatusEntity(
    @PrimaryKey
    @ColumnInfo(name = "article_id")
    val articleId: String,
    
    @ColumnInfo(name = "is_read", defaultValue = "0")
    val isRead: Boolean = false,
    
    @ColumnInfo(name = "read_at")
    val readAt: Long? = null
)