package com.syndicate.rssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.syndicate.rssreader.data.local.entities.ReadStatusEntity

@Dao
interface ReadStatusDao {
    
    @Query("SELECT * FROM read_status WHERE article_id = :articleId")
    suspend fun getReadStatus(articleId: String): ReadStatusEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadStatus(readStatus: ReadStatusEntity)
    
    @Query("UPDATE read_status SET is_read = :isRead, read_at = :readAt WHERE article_id = :articleId")
    suspend fun updateReadStatus(articleId: String, isRead: Boolean, readAt: Long?)
    
    @Query("""
        INSERT OR REPLACE INTO read_status (article_id, is_read, read_at) 
        VALUES (:articleId, :isRead, :readAt)
    """)
    suspend fun setReadStatus(articleId: String, isRead: Boolean, readAt: Long?)
    
    @Query("DELETE FROM read_status WHERE article_id = :articleId")
    suspend fun deleteReadStatus(articleId: String)
    
    @Query("""
        UPDATE read_status SET is_read = 1, read_at = :readAt 
        WHERE article_id IN (
            SELECT a.id FROM articles a 
            INNER JOIN feeds f ON a.feed_id = f.id 
            WHERE f.id = :feedId
        )
    """)
    suspend fun markAllAsReadForFeed(feedId: Long, readAt: Long)
    
    @Query("""
        UPDATE read_status SET is_read = 1, read_at = :readAt 
        WHERE article_id IN (
            SELECT a.id FROM articles a 
            INNER JOIN feeds f ON a.feed_id = f.id 
            INNER JOIN feed_group_cross_ref fgcr ON f.id = fgcr.feed_id 
            WHERE fgcr.group_id = :groupId
        )
    """)
    suspend fun markAllAsReadForGroup(groupId: Long, readAt: Long)
    
    @Query("UPDATE read_status SET is_read = 1, read_at = :readAt")
    suspend fun markAllAsRead(readAt: Long)
    
    @Query("""
        SELECT COUNT(*) FROM articles a 
        LEFT JOIN read_status rs ON a.id = rs.article_id 
        WHERE COALESCE(rs.is_read, 0) = 0
    """)
    suspend fun getUnreadCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM articles a 
        LEFT JOIN read_status rs ON a.id = rs.article_id 
        WHERE a.feed_id = :feedId AND COALESCE(rs.is_read, 0) = 0
    """)
    suspend fun getUnreadCountForFeed(feedId: Long): Int
}