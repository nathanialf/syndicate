package com.defnf.syndicate.data.models

import com.defnf.syndicate.data.local.entities.GroupEntity

data class Group(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val notificationsEnabled: Boolean = false,
    val createdAt: Long,
    val feedCount: Int = 0,
    val unreadCount: Int = 0
) {
    fun toEntity(): GroupEntity = GroupEntity(
        id = id,
        name = name,
        isDefault = isDefault,
        notificationsEnabled = notificationsEnabled,
        createdAt = createdAt
    )
}