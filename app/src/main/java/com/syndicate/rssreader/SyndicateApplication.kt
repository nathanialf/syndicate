package com.syndicate.rssreader

import android.app.Application
import com.syndicate.rssreader.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SyndicateApplication : Application() {
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    override fun onCreate() {
        super.onCreate()
        // Schedule periodic background sync
        syncScheduler.schedulePeriodSync()
    }
}