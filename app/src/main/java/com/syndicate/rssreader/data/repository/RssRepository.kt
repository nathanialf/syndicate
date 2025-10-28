package com.syndicate.rssreader.data.repository

import com.syndicate.rssreader.data.local.dao.ArticleDao
import com.syndicate.rssreader.data.local.dao.FeedDao
import com.syndicate.rssreader.data.local.dao.GroupDao
import com.syndicate.rssreader.data.local.dao.ReadStatusDao
import com.syndicate.rssreader.data.local.entities.FeedEntity
import com.syndicate.rssreader.data.local.entities.FeedGroupCrossRef
import com.syndicate.rssreader.data.local.entities.GroupEntity
import com.syndicate.rssreader.data.local.entities.ReadStatusEntity
import com.syndicate.rssreader.data.local.toDomain
import com.syndicate.rssreader.data.models.Article
import com.syndicate.rssreader.data.models.ArticleFilter
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.models.Group
import com.syndicate.rssreader.data.models.OpmlImportResult
import com.syndicate.rssreader.data.models.OpmlFeed
import com.syndicate.rssreader.data.remote.RssFetcher
import com.syndicate.rssreader.data.remote.RssParser
import com.syndicate.rssreader.data.remote.OpmlParser
import java.io.InputStream
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
    private val rssParser: RssParser,
    private val opmlParser: OpmlParser
) {
    
    fun getAllFeeds(): Flow<List<Feed>> = 
        feedDao.getAllFeeds().map { entities -> entities.map { it.toDomain() } }
    
    fun getAllGroups(): Flow<List<Group>> = 
        groupDao.getAllGroups().map { entities -> entities.map { it.toDomain() } }
    
    suspend fun getDefaultGroup(): Group? = 
        groupDao.getDefaultGroup()?.toDomain()
    
    fun getFeedsByGroup(groupId: Long): Flow<List<Feed>> = 
        feedDao.getFeedsByGroup(groupId).map { entities -> entities.map { it.toDomain() } }
    
    suspend fun getFeedById(feedId: Long): Feed? = 
        feedDao.getFeedById(feedId)?.toDomain()
    
    suspend fun getFeedByUrl(url: String): Feed? = 
        feedDao.getFeedByUrl(url)?.toDomain()
    
    suspend fun getGroupById(groupId: Long): Group? = 
        groupDao.getGroupById(groupId)?.toDomain()
    
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
    
    suspend fun getArticleById(articleId: String): Article? {
        return articleDao.getArticleById(articleId)?.toDomain()
    }
    
    suspend fun getUnreadCount(): Int = readStatusDao.getUnreadCount()
    
    suspend fun getUnreadCountForFeed(feedId: Long): Int = 
        readStatusDao.getUnreadCountForFeed(feedId)
    
    suspend fun getUnreadCountForGroup(groupId: Long): Int = 
        groupDao.getUnreadCountForGroup(groupId)
    
    suspend fun getFeedCountForGroup(groupId: Long): Int = 
        groupDao.getFeedCountForGroup(groupId)
    
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
            
            // Parse articles and only insert new ones to preserve read status
            val fetchedArticles = rssParser.parseArticles(syndFeed, feedId)
            val existingArticleIds = articleDao.getArticleIdsForFeed(feedId).toSet()
            val newArticles = fetchedArticles.filter { it.id !in existingArticleIds }
            
            if (newArticles.isNotEmpty()) {
                articleDao.insertArticles(newArticles)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateFeedAvailability(feedId, false)
            Result.failure(e)
        }
    }
    
    suspend fun refreshFeedAndGetNewArticles(feedId: Long): Result<List<com.syndicate.rssreader.data.local.entities.ArticleEntity>> {
        return try {
            val feed = getFeedById(feedId) ?: return Result.failure(Exception("Feed not found"))
            android.util.Log.d("RssRepository", "Refreshing feed: ${feed.title} (${feed.url})")
            
            val syndFeedResult = rssFetcher.fetchFeed(feed.url)
            if (syndFeedResult.isFailure) {
                android.util.Log.e("RssRepository", "Failed to fetch feed: ${feed.title} - ${syndFeedResult.exceptionOrNull()?.message}")
                updateFeedAvailability(feedId, false)
                return Result.failure(syndFeedResult.exceptionOrNull() ?: Exception("Failed to fetch feed"))
            }
            
            val syndFeed = syndFeedResult.getOrThrow()
            android.util.Log.d("RssRepository", "Successfully fetched feed: ${syndFeed.title} with ${syndFeed.entries?.size ?: 0} total entries")
            
            // Update feed info
            val updatedFeed = feed.copy(
                title = syndFeed.title ?: feed.title,
                description = syndFeed.description ?: feed.description,
                lastFetched = System.currentTimeMillis(),
                isAvailable = true
            )
            updateFeed(updatedFeed)
            
            // Parse articles and check which ones are new
            val fetchedArticles = rssParser.parseArticles(syndFeed, feedId)
            val existingArticleIds = articleDao.getArticleIdsForFeed(feedId).toSet()
            
            val newArticles = fetchedArticles.filter { it.id !in existingArticleIds }
            android.util.Log.d("RssRepository", "Feed ${feed.title}: Found ${fetchedArticles.size} total articles, ${newArticles.size} are new")
            
            // Only insert new articles to preserve read status of existing ones
            if (newArticles.isNotEmpty()) {
                articleDao.insertArticles(newArticles)
                android.util.Log.d("RssRepository", "Inserted ${newArticles.size} new articles for feed: ${feed.title}")
            }
            
            Result.success(newArticles)
        } catch (e: Exception) {
            updateFeedAvailability(feedId, false)
            Result.failure(e)
        }
    }
    
    suspend fun createGroup(name: String, isDefault: Boolean = false, notificationsEnabled: Boolean = false): Long {
        val group = GroupEntity(
            name = name,
            isDefault = false, // Initially set to false
            notificationsEnabled = notificationsEnabled
        )
        val groupId = groupDao.insertGroup(group)
        
        // If this group should be default, use the transaction method
        if (isDefault) {
            groupDao.setDefaultGroup(groupId)
        }
        
        return groupId
    }
    
    suspend fun updateGroup(groupId: Long, name: String, isDefault: Boolean, notificationsEnabled: Boolean) {
        val existingGroup = groupDao.getGroupById(groupId)
        existingGroup?.let { group ->
            val updatedGroup = group.copy(
                name = name,
                isDefault = false, // Initially set to false
                notificationsEnabled = notificationsEnabled
            )
            groupDao.updateGroup(updatedGroup)
            
            // If this group should be default, use the transaction method
            if (isDefault) {
                groupDao.setDefaultGroup(groupId)
            }
        }
    }
    
    suspend fun deleteGroup(groupId: Long) {
        // Check if this is the default group before deleting
        val groupToDelete = groupDao.getGroupById(groupId)
        val wasDefault = groupToDelete?.isDefault == true
        
        // Remove all feed-group relationships (preserves the feeds themselves)
        groupDao.removeAllFeedsFromGroup(groupId)
        
        // Delete the group
        groupDao.deleteGroupById(groupId)
        
        // If the deleted group was default, clear all defaults (revert to "All Feeds")
        if (wasDefault) {
            groupDao.clearDefaultGroup()
        }
    }
    
    suspend fun getFeedsForGroup(groupId: Long): List<FeedEntity> {
        return feedDao.getFeedsForGroup(groupId)
    }
    
    suspend fun updateFeedGroups(feedIds: List<Long>, groupIds: List<Long>) {
        feedIds.forEach { feedId ->
            groupDao.updateFeedGroups(feedId, groupIds)
        }
    }
    
    suspend fun updateGroupFeeds(groupId: Long, feedIds: List<Long>) {
        groupDao.removeAllFeedsFromGroup(groupId)
        val crossRefs = feedIds.map { FeedGroupCrossRef(it, groupId) }
        groupDao.insertFeedGroupCrossRefs(crossRefs)
    }
    
    suspend fun updateFeedNotifications(feedId: Long, enabled: Boolean) {
        feedDao.updateFeedNotifications(feedId, enabled)
    }
    
    suspend fun updateGroupNotifications(groupId: Long, enabled: Boolean) {
        groupDao.updateGroupNotifications(groupId, enabled)
    }
    
    fun getFeedsWithNotificationsEnabled() = feedDao.getFeedsWithNotificationsEnabled()
    
    fun getGroupsWithNotificationsEnabled() = groupDao.getGroupsWithNotificationsEnabled()
    
    suspend fun importOpml(inputStream: InputStream): OpmlImportResult {
        val parseResult = opmlParser.parseOpml(inputStream)
        
        // Check for existing feeds to identify duplicates
        val existingFeeds = feedDao.getAllFeedUrls()
        val normalizedExistingFeeds = existingFeeds.map { normalizeUrl(it) }
        
        val duplicates = parseResult.feedsToImport.filter { opmlFeed ->
            val normalizedOpmlUrl = normalizeUrl(opmlFeed.url)
            normalizedExistingFeeds.any { it.equals(normalizedOpmlUrl, ignoreCase = true) }
        }.map { it.url }
        
        return parseResult.copy(duplicateFeeds = duplicates)
    }
    
    private fun normalizeUrl(url: String): String {
        // Remove protocol to treat http and https as the same
        return url.removePrefix("https://").removePrefix("http://")
    }
    
    suspend fun executeOpmlImport(importResult: OpmlImportResult, skipDuplicates: Boolean = true): Result<String> {
        return try {
            val groupIdMap = mutableMapOf<String, Long>()
            val failedFeeds = mutableListOf<String>()
            
            // Create groups first
            for (groupName in importResult.groupsToCreate) {
                val existingGroup = groupDao.getGroupByName(groupName)
                
                if (existingGroup != null) {
                    groupIdMap[groupName] = existingGroup.id
                } else {
                    val groupId = groupDao.insertGroup(
                        GroupEntity(
                            name = groupName,
                            isDefault = false,
                            notificationsEnabled = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    groupIdMap[groupName] = groupId
                }
            }
            
            var successCount = 0
            var skipCount = 0
            var errorCount = 0
            
            // Import feeds
            for (opmlFeed in importResult.feedsToImport) {
                try {
                    // Skip duplicates if requested
                    if (skipDuplicates && importResult.duplicateFeeds.contains(opmlFeed.url)) {
                        skipCount++
                        continue
                    }
                    
                    // Try to fetch and validate the feed
                    val feedContentResult = rssFetcher.fetchFeed(opmlFeed.url)
                    if (feedContentResult.isFailure) {
                        errorCount++
                        failedFeeds.add("${opmlFeed.title} (${opmlFeed.url}): ${feedContentResult.exceptionOrNull()?.message ?: "Failed to fetch"}")
                        continue
                    }
                    
                    val feedContent = feedContentResult.getOrThrow()
                    val parsedFeed = rssParser.parseFeed(feedContent, opmlFeed.url)
                    
                    // Create feed entity
                    val feedEntity = FeedEntity(
                        url = opmlFeed.url,
                        title = parsedFeed.title.ifBlank { opmlFeed.title },
                        description = parsedFeed.description ?: opmlFeed.description,
                        siteUrl = parsedFeed.siteUrl,
                        faviconUrl = parsedFeed.faviconUrl,
                        lastFetched = System.currentTimeMillis(),
                        isAvailable = true,
                        notificationsEnabled = false,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val feedId = feedDao.insertFeed(feedEntity)
                    
                    // Assign to group if specified
                    opmlFeed.groupName?.let { groupName ->
                        groupIdMap[groupName]?.let { groupId ->
                            groupDao.insertFeedGroupCrossRef(
                                FeedGroupCrossRef(feedId, groupId)
                            )
                        }
                    }
                    
                    // Parse and store articles
                    val articles = rssParser.parseArticles(feedContent, feedId)
                    if (articles.isNotEmpty()) {
                        articleDao.insertArticles(articles)
                    }
                    
                    successCount++
                } catch (e: Exception) {
                    errorCount++
                    failedFeeds.add("${opmlFeed.title} (${opmlFeed.url}): ${e.message ?: "Unknown error"}")
                }
            }
            
            val message = buildString {
                append("Import completed: ")
                append("$successCount feeds imported")
                if (skipCount > 0) append(", $skipCount duplicates skipped")
                if (errorCount > 0) {
                    append(", $errorCount failed")
                    if (failedFeeds.isNotEmpty()) {
                        append("\n\nFailed feeds:")
                        failedFeeds.take(10).forEach { failedFeed ->
                            append("\n• $failedFeed")
                        }
                        if (failedFeeds.size > 10) {
                            append("\n... and ${failedFeeds.size - 10} more")
                        }
                    }
                }
            }
            
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}