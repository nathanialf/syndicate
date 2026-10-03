package com.defnf.syndicate.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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
        
        /** Adds a composite index for per-feed article lists ordered by date. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_articles_feed_id_published_date` " +
                        "ON `articles` (`feed_id`, `published_date`)"
                )
            }
        }
        
        fun getDatabase(context: Context): RssDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RssDatabase::class.java,
                    "rss_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}