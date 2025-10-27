package com.syndicate.rssreader.di

import android.content.Context
import androidx.room.Room
import com.syndicate.rssreader.data.local.RssDatabase
import com.syndicate.rssreader.data.local.dao.ArticleDao
import com.syndicate.rssreader.data.local.dao.FeedDao
import com.syndicate.rssreader.data.local.dao.GroupDao
import com.syndicate.rssreader.data.local.dao.ReadStatusDao
import com.syndicate.rssreader.data.preferences.ThemePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RssDatabase {
        return Room.databaseBuilder(
            context,
            RssDatabase::class.java,
            "rss_database"
        ).build()
    }
    
    @Provides
    fun provideFeedDao(database: RssDatabase): FeedDao = database.feedDao()
    
    @Provides
    fun provideGroupDao(database: RssDatabase): GroupDao = database.groupDao()
    
    @Provides
    fun provideArticleDao(database: RssDatabase): ArticleDao = database.articleDao()
    
    @Provides
    fun provideReadStatusDao(database: RssDatabase): ReadStatusDao = database.readStatusDao()
    
    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }
}