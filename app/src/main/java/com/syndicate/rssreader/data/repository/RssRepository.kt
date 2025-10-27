package com.syndicate.rssreader.data.repository

import com.syndicate.rssreader.data.local.dao.ArticleDao
import com.syndicate.rssreader.data.local.dao.FeedDao
import com.syndicate.rssreader.data.local.dao.GroupDao
import com.syndicate.rssreader.data.local.dao.ReadStatusDao
import com.syndicate.rssreader.data.local.entities.ReadStatusEntity
import com.syndicate.rssreader.data.local.toDomain
import com.syndicate.rssreader.data.models.Article
import com.syndicate.rssreader.data.models.ArticleFilter
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.models.Group
import com.syndicate.rssreader.data.remote.RssFetcher
import com.syndicate.rssreader.data.remote.RssParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val groupDao: GroupDao,
    private val articleDao: ArticleDao,
    private val readStatusDao: ReadStatusDao,
    private val rssFetcher: RssFetcher,
    private val rssParser: RssParser
) {
    
    fun getAllFeeds(): Flow<List<Feed>> = 
        feedDao.getAllFeeds().map { entities -> entities.map { it.toDomain() } }
    
    fun getAllGroups(): Flow<List<Group>> = 
        groupDao.getAllGroups().map { entities -> entities.map { it.toDomain() } }
    
    fun getFeedsByGroup(groupId: Long): Flow<List<Feed>> = 
        feedDao.getFeedsByGroup(groupId).map { entities -> entities.map { it.toDomain() } }
    
    suspend fun getFeedById(feedId: Long): Feed? = 
        feedDao.getFeedById(feedId)?.toDomain()
    
    suspend fun getFeedByUrl(url: String): Feed? = 
        feedDao.getFeedByUrl(url)?.toDomain()
    
    suspend fun getGroupById(groupId: Long): Group? = 
        groupDao.getGroupById(groupId)?.toDomain()
    
    suspend fun getDefaultGroup(): Group? = 
        groupDao.getDefaultGroup()?.toDomain()
    
    suspend fun insertFeed(feed: Feed): Long = 
        feedDao.insertFeed(feed.toEntity())
    
    suspend fun insertGroup(group: Group): Long = 
        groupDao.insertGroup(group.toEntity())
    
    suspend fun updateFeed(feed: Feed) = 
        feedDao.updateFeed(feed.toEntity())
    
    suspend fun updateGroup(group: Group) = 
        groupDao.updateGroup(group.toEntity())
    
    suspend fun deleteFeed(feed: Feed) = 
        feedDao.deleteFeed(feed.toEntity())
    
    suspend fun deleteGroup(group: Group) = 
        groupDao.deleteGroup(group.toEntity())
    
    suspend fun updateLastFetched(feedId: Long, timestamp: Long) = 
        feedDao.updateLastFetched(feedId, timestamp)
    
    suspend fun updateFeedAvailability(feedId: Long, isAvailable: Boolean) = 
        feedDao.updateFeedAvailability(feedId, isAvailable)
    
    fun getArticles(filter: ArticleFilter): Flow<List<Article>> {
        return when {
            filter.searchQuery != null -> {
                articleDao.searchArticles(filter.searchQuery).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
            filter.feedId != null -> {
                articleDao.getArticlesByFeed(filter.feedId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
            filter.groupId != null -> {
                articleDao.getArticlesByGroup(filter.groupId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
            filter.unreadOnly -> {
                articleDao.getUnreadArticles().map { entities ->
                    entities.map { it.toDomain() }
                }
            }
            else -> {
                articleDao.getAllArticles().map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }
    }
    
    suspend fun markAsRead(articleId: String, isRead: Boolean = true) {
        val timestamp = if (isRead) System.currentTimeMillis() else null
        readStatusDao.setReadStatus(articleId, isRead, timestamp)
    }
    
    suspend fun markAllAsReadForFeed(feedId: Long) {
        readStatusDao.markAllAsReadForFeed(feedId, System.currentTimeMillis())
    }
    
    suspend fun markAllAsReadForGroup(groupId: Long) {
        readStatusDao.markAllAsReadForGroup(groupId, System.currentTimeMillis())
    }
    
    suspend fun markAllAsRead() {
        readStatusDao.markAllAsRead(System.currentTimeMillis())
    }
    
    suspend fun getUnreadCount(): Int = readStatusDao.getUnreadCount()
    
    suspend fun getUnreadCountForFeed(feedId: Long): Int = 
        readStatusDao.getUnreadCountForFeed(feedId)
    
    suspend fun getUnreadCountForGroup(groupId: Long): Int = 
        groupDao.getUnreadCountForGroup(groupId)
    
    suspend fun updateFeedGroups(feedId: Long, groupIds: List<Long>) {
        groupDao.updateFeedGroups(feedId, groupIds)
    }
    
    suspend fun setDefaultGroup(groupId: Long) {
        groupDao.setDefaultGroup(groupId)
    }
    
    fun getGroupsForFeed(feedId: Long): Flow<List<Group>> = 
        groupDao.getGroupsForFeed(feedId).map { entities -> entities.map { it.toDomain() } }
    
    suspend fun addFeedFromUrl(url: String): Result<Long> {
        return try {
            // Check if feed already exists
            val existingFeed = getFeedByUrl(url)
            if (existingFeed != null) {
                return Result.failure(Exception("Feed already exists"))
            }
            
            // Fetch and parse feed
            val syndFeedResult = rssFetcher.fetchFeed(url)
            if (syndFeedResult.isFailure) {
                return Result.failure(syndFeedResult.exceptionOrNull() ?: Exception("Failed to fetch feed"))
            }
            
            val syndFeed = syndFeedResult.getOrThrow()
            val feedEntity = rssParser.parseFeed(syndFeed, url)
            
            // Insert feed and get ID
            val feedId = insertFeed(feedEntity.toDomain())
            
            // Parse and insert articles
            val articles = rssParser.parseArticles(syndFeed, feedId)
            articleDao.insertArticles(articles)
            
            Result.success(feedId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun refreshFeed(feedId: Long): Result<Unit> {
        return try {
            val feed = getFeedById(feedId) ?: return Result.failure(Exception("Feed not found"))
            
            val syndFeedResult = rssFetcher.fetchFeed(feed.url)
            if (syndFeedResult.isFailure) {
                updateFeedAvailability(feedId, false)
                return Result.failure(syndFeedResult.exceptionOrNull() ?: Exception("Failed to fetch feed"))
            }
            
            val syndFeed = syndFeedResult.getOrThrow()
            
            // Update feed info
            val updatedFeed = feed.copy(
                title = syndFeed.title ?: feed.title,
                description = syndFeed.description ?: feed.description,
                lastFetched = System.currentTimeMillis(),
                isAvailable = true
            )
            updateFeed(updatedFeed)
            
            // Parse and insert new articles
            val articles = rssParser.parseArticles(syndFeed, feedId)
            articleDao.insertArticles(articles)
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateFeedAvailability(feedId, false)
            Result.failure(e)
        }
    }
}