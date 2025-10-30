package com.defnf.syndicate.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.defnf.syndicate.data.models.Group

@Entity(
    tableName = "groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "is_default", defaultValue = "0")
    val isDefault: Boolean = false,
    
    @ColumnInfo(name = "notifications_enabled", defaultValue = "0")
    val notificationsEnabled: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Group = Group(
        id = id,
        name = name,
        isDefault = isDefault,
        notificationsEnabled = notificationsEnabled,
        createdAt = createdAt
    )
}