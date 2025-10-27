package com.syndicate.rssreader.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "feed_group_cross_ref",
    primaryKeys = ["feed_id", "group_id"],
    indices = [Index(value = ["group_id"])],
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FeedGroupCrossRef(
    @ColumnInfo(name = "feed_id")
    val feedId: Long,
    
    @ColumnInfo(name = "group_id")
    val groupId: Long
)