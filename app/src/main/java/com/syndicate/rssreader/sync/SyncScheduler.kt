package com.syndicate.rssreader.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager by lazy { WorkManager.getInstance(context) }
    
    companion object {
        private const val SYNC_WORK_NAME = "syndicate_sync_work"
        private const val MANUAL_SYNC_WORK_NAME = "syndicate_manual_sync_work"
        private const val LAUNCH_SYNC_WORK_NAME = "syndicate_launch_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 5L // Temporary for testing
    }
    
    fun schedulePeriodSync() {
        Log.d("SyncScheduler", "Scheduling periodic sync every $SYNC_INTERVAL_MINUTES minutes")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Respect battery optimization
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            syncWorkRequest
        )
        
        Log.d("SyncScheduler", "Periodic sync scheduled successfully")
    }
    
    fun cancelSync() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
    }
    
    fun triggerManualSync() {
        Log.d("SyncScheduler", "Triggering manual sync for all feeds")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val manualSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniqueWork(
            MANUAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any existing manual sync
            manualSyncRequest
        )
        
        Log.d("SyncScheduler", "Manual sync triggered")
    }
    
    fun triggerManualSyncForFeed(feedId: Long) {
        Log.d("SyncScheduler", "Triggering manual sync for feed: $feedId")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val manualSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                androidx.work.Data.Builder()
                    .putLong("feedId", feedId)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "${MANUAL_SYNC_WORK_NAME}_feed_$feedId",
            ExistingWorkPolicy.REPLACE, // Replace any existing manual sync for this feed
            manualSyncRequest
        )
        
        Log.d("SyncScheduler", "Manual sync triggered for feed: $feedId")
    }
    
    fun triggerManualSyncForGroup(groupId: Long) {
        Log.d("SyncScheduler", "Triggering manual sync for group: $groupId")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val manualSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                androidx.work.Data.Builder()
                    .putLong("groupId", groupId)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "${MANUAL_SYNC_WORK_NAME}_group_$groupId",
            ExistingWorkPolicy.REPLACE, // Replace any existing manual sync for this group
            manualSyncRequest
        )
        
        Log.d("SyncScheduler", "Manual sync triggered for group: $groupId")
    }
    
    fun triggerLaunchSync() {
        Log.d("SyncScheduler", "Triggering launch sync")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val launchSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniqueWork(
            LAUNCH_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any existing launch sync
            launchSyncRequest
        )
        
        Log.d("SyncScheduler", "Launch sync triggered")
    }
}