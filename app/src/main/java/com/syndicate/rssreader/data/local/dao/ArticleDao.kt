package com.syndicate.rssreader.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.syndicate.rssreader.data.local.entities.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    
    @Query("""
        SELECT a.*, f.title as feed_title, f.favicon_url as feed_favicon_url,
               COALESCE(rs.is_read, 0) as is_read, rs.read_at
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        ORDER BY a.published_date DESC
    """)
    fun getAllArticles(): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        SELECT a.*, f.title as feed_title, f.favicon_url as feed_favicon_url,
               COALESCE(rs.is_read, 0) as is_read, rs.read_at
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE a.feed_id = :feedId
        ORDER BY a.published_date DESC
    """)
    fun getArticlesByFeed(feedId: Long): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        SELECT a.*, f.title as feed_title, f.favicon_url as feed_favicon_url,
               COALESCE(rs.is_read, 0) as is_read, rs.read_at
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        INNER JOIN feed_group_cross_ref fgcr ON f.id = fgcr.feed_id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE fgcr.group_id = :groupId
        ORDER BY a.published_date DESC
    """)
    fun getArticlesByGroup(groupId: Long): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        SELECT a.*, f.title as feed_title, f.favicon_url as feed_favicon_url,
               COALESCE(rs.is_read, 0) as is_read, rs.read_at
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE COALESCE(rs.is_read, 0) = 0
        ORDER BY a.published_date DESC
    """)
    fun getUnreadArticles(): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        SELECT a.*, f.title as feed_title, f.favicon_url as feed_favicon_url,
               COALESCE(rs.is_read, 0) as is_read, rs.read_at
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE a.title LIKE '%' || :query || '%' OR a.description LIKE '%' || :query || '%'
        ORDER BY a.published_date DESC
    """)
    fun searchArticles(query: String): Flow<List<ArticleWithReadStatus>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)
    
    @Query("DELETE FROM articles WHERE feed_id = :feedId AND fetched_at < :cutoffTime")
    suspend fun deleteOldArticlesForFeed(feedId: Long, cutoffTime: Long)
    
    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticle(articleId: String)
    
    @Query("SELECT COUNT(*) FROM articles WHERE feed_id = :feedId")
    suspend fun getArticleCountForFeed(feedId: Long): Int
}

data class ArticleWithReadStatus(
    val id: String,
    @ColumnInfo(name = "feed_id") val feedId: Long,
    val title: String,
    val description: String?,
    val url: String,
    val author: String?,
    @ColumnInfo(name = "published_date") val publishedDate: Long?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    @ColumnInfo(name = "feed_title") val feedTitle: String,
    @ColumnInfo(name = "feed_favicon_url") val feedFaviconUrl: String?,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "read_at") val readAt: Long?
)