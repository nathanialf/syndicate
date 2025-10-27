package com.syndicate.rssreader.data.models

import com.syndicate.rssreader.data.local.entities.GroupEntity

data class Group(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val feedCount: Int = 0,
    val unreadCount: Int = 0
) {
    fun toEntity(): GroupEntity = GroupEntity(
        id = id,
        name = name,
        isDefault = isDefault,
        createdAt = createdAt
    )
}