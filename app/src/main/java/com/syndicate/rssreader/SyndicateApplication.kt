package com.syndicate.rssreader

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.syndicate.rssreader.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SyndicateApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var syncScheduler: SyncScheduler
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager with Hilt configuration since auto-init is disabled
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Schedule periodic background sync
        syncScheduler.schedulePeriodSync()
        
        // Trigger initial sync on app launch
        syncScheduler.triggerLaunchSync()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}