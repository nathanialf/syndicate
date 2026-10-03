package com.defnf.syndicate.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.defnf.syndicate.data.local.entities.ArticleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Columns for article list queries. The description is truncated because list rows only show a
 * short preview, which keeps cursor windows and memory small as the database grows; the article
 * detail screen loads the full content via [ArticleDao.getArticleById].
 */
private const val ARTICLE_LIST_COLUMNS = """
    SELECT a.id, a.feed_id, a.title, substr(a.description, 1, 2000) AS description, a.url, a.author,
           a.published_date, a.thumbnail_url, a.fetched_at,
           f.title AS feed_title, f.favicon_url AS feed_favicon_url,
           COALESCE(rs.is_read, 0) AS is_read, rs.read_at
"""

/** Read state filter applied in SQL: [READ_STATE_ANY], [READ_STATE_UNREAD] or [READ_STATE_READ]. */
private const val READ_STATE_CONDITION = "(:readState < 0 OR COALESCE(rs.is_read, 0) = :readState)"

const val READ_STATE_ANY = -1
const val READ_STATE_UNREAD = 0
const val READ_STATE_READ = 1

@Dao
interface ArticleDao {
    
    @Query("""
        $ARTICLE_LIST_COLUMNS
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE $READ_STATE_CONDITION
        ORDER BY a.published_date DESC
    """)
    fun getAllArticles(readState: Int): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        $ARTICLE_LIST_COLUMNS
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE a.feed_id = :feedId AND $READ_STATE_CONDITION
        ORDER BY a.published_date DESC
    """)
    fun getArticlesByFeed(feedId: Long, readState: Int): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        $ARTICLE_LIST_COLUMNS
        FROM articles a
        INNER JOIN feed_group_cross_ref fgcr ON a.feed_id = fgcr.feed_id
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE fgcr.group_id = :groupId AND $READ_STATE_CONDITION
        ORDER BY a.published_date DESC
    """)
    fun getArticlesByGroup(groupId: Long, readState: Int): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        $ARTICLE_LIST_COLUMNS
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE (a.title LIKE '%' || :query || '%' OR a.description LIKE '%' || :query || '%')
            AND $READ_STATE_CONDITION
        ORDER BY a.published_date DESC
    """)
    fun searchArticles(query: String, readState: Int): Flow<List<ArticleWithReadStatus>>
    
    @Query("""
        SELECT a.*, f.title as feed_title, f.favicon_url as feed_favicon_url,
               COALESCE(rs.is_read, 0) as is_read, rs.read_at
        FROM articles a
        INNER JOIN feeds f ON a.feed_id = f.id
        LEFT JOIN read_status rs ON a.id = rs.article_id
        WHERE a.id = :articleId
    """)
    suspend fun getArticleById(articleId: String): ArticleWithReadStatus?
    
    /**
     * Inserts articles that are not stored yet and leaves existing ones untouched, so their read
     * status is preserved (REPLACE would delete the old row and cascade-delete its read status).
     * Returns the row id for each article, or -1 for articles that already existed.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>): List<Long>
    
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