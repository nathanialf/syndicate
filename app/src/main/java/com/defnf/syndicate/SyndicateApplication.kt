package com.defnf.syndicate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.defnf.syndicate.sync.SyncScheduler
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
        
        // Trigger initial sync on app launch first
        syncScheduler.triggerLaunchSync()
        
        // Schedule periodic background sync after a delay to avoid conflicts
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            syncScheduler.schedulePeriodSync()
        }, 5000) // 5 second delay
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}