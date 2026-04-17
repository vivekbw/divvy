package com.divvy.divvy.offline

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.divvy.divvy.backend.SupabaseExpensesRepository
import com.divvy.divvy.backend.SupabaseGroupRepository
import com.divvy.divvy.backend.SupabaseBalanceRepository
import com.divvy.divvy.models.ExpenseSplit
import com.divvy.divvy.offline.PendingOperationPayloads.CreateExpenseWithSplitsPayload
import com.divvy.divvy.offline.PendingOperationPayloads.DeleteExpensePayload
import com.divvy.divvy.offline.db.dao.CachedExpenseDao
import com.divvy.divvy.offline.db.dao.CachedExpenseSplitDao
import com.divvy.divvy.offline.db.dao.PendingOperationDao
import com.divvy.divvy.offline.db.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSyncManager @Inject constructor(
    private val pendingOperationDao: PendingOperationDao,
    private val expensesRepository: SupabaseExpensesRepository,
    private val groupRepository: SupabaseGroupRepository,
    private val balanceRepository: SupabaseBalanceRepository,
    private val cachedExpenseDao: CachedExpenseDao,
    private val cachedExpenseSplitDao: CachedExpenseSplitDao,
    private val workManager: WorkManager,
    private val networkMonitor: NetworkMonitor
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val SYNC_WORK_NAME = "divvy_offline_sync"
        private const val MAX_RETRIES = 5
    }

    init {
        networkMonitor.registerSyncTrigger { triggerSync() }
    }

    val pendingCount: Flow<Int> = pendingOperationDao.getPendingCount()

    suspend fun enqueuePendingOperation(type: String, payloadJson: String) {
        pendingOperationDao.insert(
            PendingOperationEntity(
                operationType = type,
                payloadJson = payloadJson
            )
        )
        if (networkMonitor.isOnline.value) {
            triggerSync()
        }
    }

    fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    suspend fun drainQueue() {
        val pendingOps = pendingOperationDao.getAllPending()
        Timber.d("Draining sync queue: ${pendingOps.size} pending operations")

        for (op in pendingOps) {
            try {
                when (op.operationType) {
                    "CREATE_EXPENSE" -> processCreateExpense(op)
                    "DELETE_EXPENSE" -> processDeleteExpense(op)
                    else -> {
                        Timber.w("Unknown operation type: ${op.operationType}")
                        pendingOperationDao.delete(op.id)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to process pending operation ${op.id}")
                pendingOperationDao.incrementRetryCount(op.id)
                if (op.retryCount + 1 >= MAX_RETRIES) {
                    Timber.w("Operation ${op.id} exceeded max retries, marking FAILED")
                    pendingOperationDao.updateStatus(op.id, "FAILED")
                }
            }
        }

        // After draining, trigger full refresh
        try {
            groupRepository.refreshGroups()
        } catch (e: Exception) {
            Timber.w(e, "Post-sync group refresh failed")
        }
    }

    private suspend fun processCreateExpense(op: PendingOperationEntity) {
        val payload = json.decodeFromString<CreateExpenseWithSplitsPayload>(op.payloadJson)

        val splits = payload.splits.map {
            ExpenseSplit(userId = it.userId, amountCents = it.amountCents, isCoveredBy = it.isCoveredBy)
        }

        val serverExpense = expensesRepository.createExpenseWithSplits(
            groupId = payload.groupId,
            description = payload.description,
            amountCents = payload.amountCents,
            currency = payload.currency,
            splitMethod = payload.splitMethod,
            paidByUserId = payload.paidByUserId,
            splits = splits
        )

        // Replace temp local ID with server ID in cache
        cachedExpenseDao.replaceTempId(payload.tempLocalId, serverExpense.id)
        cachedExpenseSplitDao.replaceTempExpenseId(payload.tempLocalId, serverExpense.id)

        // Refresh balances for this group
        try {
            balanceRepository.refreshBalances(payload.groupId)
        } catch (e: Exception) {
            Timber.w(e, "Post-create balance refresh failed for group ${payload.groupId}")
        }

        pendingOperationDao.delete(op.id)
        Timber.d("Synced expense: ${payload.tempLocalId} → ${serverExpense.id}")
    }

    private suspend fun processDeleteExpense(op: PendingOperationEntity) {
        val payload = json.decodeFromString<DeleteExpensePayload>(op.payloadJson)
        expensesRepository.deleteExpense(payload.expenseId)
        pendingOperationDao.delete(op.id)
        Timber.d("Synced deletion: ${payload.expenseId}")
    }
}
