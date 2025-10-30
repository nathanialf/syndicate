package com.defnf.syndicate.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.defnf.syndicate.data.local.dao.ArticleDao
import com.defnf.syndicate.data.local.dao.FeedDao
import com.defnf.syndicate.data.local.dao.GroupDao
import com.defnf.syndicate.data.local.dao.ReadStatusDao
import com.defnf.syndicate.data.local.entities.ArticleEntity
import com.defnf.syndicate.data.local.entities.FeedEntity
import com.defnf.syndicate.data.local.entities.FeedGroupCrossRef
import com.defnf.syndicate.data.local.entities.GroupEntity
import com.defnf.syndicate.data.local.entities.ReadStatusEntity

@Database(
    entities = [
        FeedEntity::class,
        GroupEntity::class,
        ArticleEntity::class,
        ReadStatusEntity::class,
        FeedGroupCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RssDatabase : RoomDatabase() {
    
    abstract fun feedDao(): FeedDao
    abstract fun groupDao(): GroupDao
    abstract fun articleDao(): ArticleDao
    abstract fun readStatusDao(): ReadStatusDao
    
    companion object {
        @Volatile
        private var INSTANCE: RssDatabase? = null
        
        
        fun getDatabase(context: Context): RssDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RssDatabase::class.java,
                    "rss_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}