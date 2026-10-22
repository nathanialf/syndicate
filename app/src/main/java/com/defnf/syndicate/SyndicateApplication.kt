package com.defnf.syndicate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.defnf.syndicate.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SyndicateApplication :
    Application(),
    Configuration.Provider,
    ImageLoaderFactory {

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

    // Favicons and article images rarely change, and many servers send no-cache headers for them;
    // keeping them on disk regardless means they show at once on the next launch instead of popping in
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .respectCacheHeaders(false)
        .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
