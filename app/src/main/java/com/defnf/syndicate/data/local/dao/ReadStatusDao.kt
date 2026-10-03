package com.defnf.syndicate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.defnf.syndicate.data.local.entities.ReadStatusEntity

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
    
    // The mark-all queries only write rows for articles that are still unread, so already read
    // articles keep their original read time and large databases don't rewrite every row
    @Query("""
        INSERT OR REPLACE INTO read_status (article_id, is_read, read_at)
        SELECT a.id, 1, :readAt FROM articles a
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE a.feed_id = :feedId AND COALESCE(rs.is_read, 0) = 0
    """)
    suspend fun markAllAsReadForFeed(feedId: Long, readAt: Long)
    
    @Query("""
        INSERT OR REPLACE INTO read_status (article_id, is_read, read_at)
        SELECT a.id, 1, :readAt FROM articles a
        INNER JOIN feed_group_cross_ref fgcr ON a.feed_id = fgcr.feed_id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE fgcr.group_id = :groupId AND COALESCE(rs.is_read, 0) = 0
    """)
    suspend fun markAllAsReadForGroup(groupId: Long, readAt: Long)
    
    @Query("""
        INSERT OR REPLACE INTO read_status (article_id, is_read, read_at)
        SELECT a.id, 1, :readAt FROM articles a
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE COALESCE(rs.is_read, 0) = 0
    """)
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