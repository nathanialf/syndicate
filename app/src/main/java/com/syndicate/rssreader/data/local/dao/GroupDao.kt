package com.syndicate.rssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.syndicate.rssreader.data.local.entities.FeedGroupCrossRef
import com.syndicate.rssreader.data.local.entities.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): GroupEntity?
    
    @Query("SELECT * FROM groups WHERE is_default = 1")
    suspend fun getDefaultGroup(): GroupEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long
    
    @Update
    suspend fun updateGroup(group: GroupEntity)
    
    @Delete
    suspend fun deleteGroup(group: GroupEntity)
    
    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedGroupCrossRef(crossRef: FeedGroupCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedGroupCrossRefs(crossRefs: List<FeedGroupCrossRef>)
    
    @Query("DELETE FROM feed_group_cross_ref WHERE feed_id = :feedId AND group_id = :groupId")
    suspend fun deleteFeedFromGroup(feedId: Long, groupId: Long)
    
    @Query("DELETE FROM feed_group_cross_ref WHERE feed_id = :feedId")
    suspend fun removeFeedFromAllGroups(feedId: Long)
    
    @Query("DELETE FROM feed_group_cross_ref WHERE group_id = :groupId")
    suspend fun removeAllFeedsFromGroup(groupId: Long)
    
    @Query("""
        SELECT g.* FROM groups g
        INNER JOIN feed_group_cross_ref fgcr ON g.id = fgcr.group_id
        WHERE fgcr.feed_id = :feedId
        ORDER BY g.name ASC
    """)
    fun getGroupsForFeed(feedId: Long): Flow<List<GroupEntity>>
    
    @Query("""
        SELECT COUNT(*) FROM feed_group_cross_ref 
        WHERE group_id = :groupId
    """)
    suspend fun getFeedCountForGroup(groupId: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        INNER JOIN feed_group_cross_ref fgcr ON f.id = fgcr.feed_id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE fgcr.group_id = :groupId AND COALESCE(rs.is_read, 0) = 0
    """)
    suspend fun getUnreadCountForGroup(groupId: Long): Int
    
    @Transaction
    suspend fun updateFeedGroups(feedId: Long, groupIds: List<Long>) {
        removeFeedFromAllGroups(feedId)
        val crossRefs = groupIds.map { FeedGroupCrossRef(feedId, it) }
        insertFeedGroupCrossRefs(crossRefs)
    }
    
    @Query("UPDATE groups SET is_default = 0")
    suspend fun clearDefaultGroup()
    
    @Transaction
    suspend fun setDefaultGroup(groupId: Long) {
        clearDefaultGroup()
        val group = getGroupById(groupId)
        group?.let {
            updateGroup(it.copy(isDefault = true))
        }
    }
    
    @Query("SELECT * FROM groups WHERE notifications_enabled = 1")
    fun getGroupsWithNotificationsEnabled(): Flow<List<GroupEntity>>
    
    @Query("UPDATE groups SET notifications_enabled = :enabled WHERE id = :groupId")
    suspend fun updateGroupNotifications(groupId: Long, enabled: Boolean)
    
    @Query("SELECT * FROM groups WHERE name = :name LIMIT 1")
    suspend fun getGroupByName(name: String): GroupEntity?
}