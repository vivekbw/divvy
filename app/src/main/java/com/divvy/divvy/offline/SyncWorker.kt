package com.divvy.divvy.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: OfflineSyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("SyncWorker: starting queue drain")
            syncManager.drainQueue()
            Timber.d("SyncWorker: queue drain complete")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
