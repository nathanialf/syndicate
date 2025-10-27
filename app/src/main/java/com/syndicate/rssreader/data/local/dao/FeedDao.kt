package com.syndicate.rssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.syndicate.rssreader.data.local.entities.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    
    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun getAllFeeds(): Flow<List<FeedEntity>>
    
    @Query("SELECT * FROM feeds WHERE id = :feedId")
    suspend fun getFeedById(feedId: Long): FeedEntity?
    
    @Query("SELECT * FROM feeds WHERE url = :url")
    suspend fun getFeedByUrl(url: String): FeedEntity?
    
    @Query("""
        SELECT f.* FROM feeds f
        INNER JOIN feed_group_cross_ref fgcr ON f.id = fgcr.feed_id
        WHERE fgcr.group_id = :groupId
        ORDER BY f.title ASC
    """)
    fun getFeedsByGroup(groupId: Long): Flow<List<FeedEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: FeedEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeds(feeds: List<FeedEntity>)
    
    @Update
    suspend fun updateFeed(feed: FeedEntity)
    
    @Delete
    suspend fun deleteFeed(feed: FeedEntity)
    
    @Query("DELETE FROM feeds WHERE id = :feedId")
    suspend fun deleteFeedById(feedId: Long)
    
    @Query("UPDATE feeds SET last_fetched = :timestamp WHERE id = :feedId")
    suspend fun updateLastFetched(feedId: Long, timestamp: Long)
    
    @Query("UPDATE feeds SET is_available = :isAvailable WHERE id = :feedId")
    suspend fun updateFeedAvailability(feedId: Long, isAvailable: Boolean)
    
    @Query("SELECT * FROM feeds WHERE notifications_enabled = 1")
    fun getFeedsWithNotificationsEnabled(): Flow<List<FeedEntity>>
    
    @Query("UPDATE feeds SET notifications_enabled = :enabled WHERE id = :feedId")
    suspend fun updateFeedNotifications(feedId: Long, enabled: Boolean)
    
    @Query("""
        SELECT f.* FROM feeds f
        INNER JOIN feed_group_cross_ref fgcr ON f.id = fgcr.feed_id
        WHERE fgcr.group_id = :groupId
        ORDER BY f.title ASC
    """)
    suspend fun getFeedsForGroup(groupId: Long): List<FeedEntity>
    
    @Query("SELECT url FROM feeds")
    suspend fun getAllFeedUrls(): List<String>
}