package com.divvy.divvy.offline.repository

import com.divvy.divvy.backend.ExpensesRepository
import com.divvy.divvy.backend.SupabaseExpensesRepository
import com.divvy.divvy.models.Expense
import com.divvy.divvy.models.ExpenseSplit
import com.divvy.divvy.models.GroupExpense
import com.divvy.divvy.models.ReceiptItemRow
import com.divvy.divvy.offline.NetworkMonitor
import com.divvy.divvy.offline.OfflineSyncManager
import com.divvy.divvy.offline.PendingOperationPayloads.CreateExpenseWithSplitsPayload
import com.divvy.divvy.offline.PendingOperationPayloads.SplitPayload
import com.divvy.divvy.offline.db.dao.CachedExpenseDao
import com.divvy.divvy.offline.db.dao.CachedExpenseSplitDao
import com.divvy.divvy.offline.db.entity.CachedExpenseEntity
import com.divvy.divvy.offline.db.entity.CachedExpenseSplitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineExpensesRepository @Inject constructor(
    private val remote: SupabaseExpensesRepository,
    private val expenseDao: CachedExpenseDao,
    private val splitDao: CachedExpenseSplitDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: OfflineSyncManager
) : ExpensesRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    init {
        scope.launch { refreshAllExpenses() }
    }

    override suspend fun listExpenses(): List<Expense> {
        return if (networkMonitor.isOnline.value) {
            remote.listExpenses()
        } else {
            emptyList()
        }
    }

    override suspend fun listExpensesByGroup(groupId: String): List<Expense> {
        return if (networkMonitor.isOnline.value) {
            remote.listExpensesByGroup(groupId)
        } else {
            emptyList()
        }
    }

    override suspend fun listGroupExpenses(groupId: String): List<GroupExpense> {
        return if (networkMonitor.isOnline.value) {
            try {
                val expenses = remote.listGroupExpenses(groupId)
                cacheExpenses(groupId, expenses)
                expenses
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch group expenses, falling back to cache")
                getCachedGroupExpenses(groupId)
            }
        } else {
            getCachedGroupExpenses(groupId)
        }
    }

    override suspend fun getExpenseById(expenseId: String): Expense? {
        return if (networkMonitor.isOnline.value) {
            remote.getExpenseById(expenseId)
        } else {
            null
        }
    }

    override suspend fun getGroupExpenseById(expenseId: String): GroupExpense? {
        return if (networkMonitor.isOnline.value) {
            remote.getGroupExpenseById(expenseId)
        } else {
            val entity = expenseDao.getById(expenseId) ?: return null
            val splits = splitDao.getByExpenseId(expenseId)
            entity.toGroupExpense(splits)
        }
    }

    override suspend fun createExpense(
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        paidByUserId: String
    ): Expense = remote.createExpense(groupId, description, amountCents, splitMethod, paidByUserId)

    override suspend fun createExpenseWithSplits(
        groupId: String,
        description: String,
        amountCents: Long,
        currency: String,
        splitMethod: String,
        paidByUserId: String,
        splits: List<ExpenseSplit>
    ): GroupExpense {
        if (networkMonitor.isOnline.value) {
            return try {
                val expense = remote.createExpenseWithSplits(
                    groupId, description, amountCents, currency, splitMethod, paidByUserId, splits
                )
                // Cache the newly created expense
                expenseDao.insert(expense.toEntity(isPending = false))
                splitDao.deleteByExpenseId(expense.id)
                splitDao.insertAll(expense.splits.map { it.toEntity(expense.id) })
                expense
            } catch (e: Exception) {
                Timber.w(e, "Online create failed, queuing offline")
                createExpenseOffline(groupId, description, amountCents, currency, splitMethod, paidByUserId, splits)
            }
        } else {
            return createExpenseOffline(groupId, description, amountCents, currency, splitMethod, paidByUserId, splits)
        }
    }

    private suspend fun createExpenseOffline(
        groupId: String,
        description: String,
        amountCents: Long,
        currency: String,
        splitMethod: String,
        paidByUserId: String,
        splits: List<ExpenseSplit>
    ): GroupExpense {
        val tempId = "local_${UUID.randomUUID()}"
        val now = java.time.Instant.now().toString()

        val groupExpense = GroupExpense(
            id = tempId,
            groupId = groupId,
            title = description,
            amountCents = amountCents,
            paidByUserId = paidByUserId,
            splits = splits,
            createdAt = now,
            currency = currency
        )

        // Cache locally with pending flag
        expenseDao.insert(groupExpense.toEntity(isPending = true))
        splitDao.insertAll(splits.map { it.toEntity(tempId) })

        // Enqueue for sync
        val payload = CreateExpenseWithSplitsPayload(
            tempLocalId = tempId,
            groupId = groupId,
            description = description,
            amountCents = amountCents,
            currency = currency,
            splitMethod = splitMethod,
            paidByUserId = paidByUserId,
            splits = splits.map { SplitPayload(it.userId, it.amountCents, it.isCoveredBy) }
        )
        syncManager.enqueuePendingOperation("CREATE_EXPENSE", json.encodeToString(payload))

        return groupExpense
    }

    override suspend fun updateExpense(
        expenseId: String,
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        currency: String
    ): Expense = remote.updateExpense(expenseId, groupId, description, amountCents, splitMethod, currency)

    override suspend fun updateExpenseSplits(expenseId: String, splits: List<ExpenseSplit>) {
        remote.updateExpenseSplits(expenseId, splits)
    }

    override suspend fun deleteExpense(expenseId: String) {
        if (networkMonitor.isOnline.value) {
            remote.deleteExpense(expenseId)
        } else {
            val payload = kotlinx.serialization.json.buildJsonObject {
                put("expenseId", kotlinx.serialization.json.JsonPrimitive(expenseId))
            }.toString()
            syncManager.enqueuePendingOperation("DELETE_EXPENSE", payload)
        }
        // Remove from local cache immediately
        expenseDao.deleteById(expenseId)
        splitDao.deleteByExpenseId(expenseId)
    }

    override suspend fun saveReceiptItems(items: List<ReceiptItemRow>) {
        remote.saveReceiptItems(items)
    }

    override fun observeGroupExpenses(groupId: String): Flow<List<GroupExpense>> =
        expenseDao.getByGroupId(groupId).map { entities ->
            entities.map { entity ->
                val splits = splitDao.getByExpenseId(entity.id)
                entity.toGroupExpense(splits)
            }
        }

    override fun observeAllGroupExpenses(): Flow<List<GroupExpense>> =
        expenseDao.getAll().map { entities ->
            entities.map { entity ->
                val splits = splitDao.getByExpenseId(entity.id)
                entity.toGroupExpense(splits)
            }
        }

    override suspend fun refreshGroupExpenses(groupId: String) {
        if (!networkMonitor.isOnline.value) return
        try {
            remote.refreshGroupExpenses(groupId)
            val expenses = remote.listGroupExpenses(groupId)
            cacheExpenses(groupId, expenses)
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh expenses for group $groupId")
        }
    }

    override suspend fun refreshAllExpenses() {
        if (!networkMonitor.isOnline.value) return
        try {
            remote.refreshAllExpenses()
            val allExpenses = remote.observeAllGroupExpenses().first()
            for ((groupId, expenses) in allExpenses.groupBy { it.groupId }) {
                cacheExpenses(groupId, expenses)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh all expenses")
        }
    }

    private suspend fun cacheExpenses(groupId: String, expenses: List<GroupExpense>) {
        expenseDao.deleteByGroupId(groupId)
        expenseDao.insertAll(expenses.map { it.toEntity(isPending = false) })
        expenses.forEach { expense ->
            splitDao.deleteByExpenseId(expense.id)
            splitDao.insertAll(expense.splits.map { it.toEntity(expense.id) })
        }
    }

    private suspend fun getCachedGroupExpenses(groupId: String): List<GroupExpense> {
        val entities = mutableListOf<CachedExpenseEntity>()
        expenseDao.getByGroupId(groupId).collect { entities.addAll(it); return@collect }
        return entities.map { entity ->
            val splits = splitDao.getByExpenseId(entity.id)
            entity.toGroupExpense(splits)
        }
    }

    private fun CachedExpenseEntity.toGroupExpense(splits: List<CachedExpenseSplitEntity>): GroupExpense =
        GroupExpense(
            id = id,
            groupId = groupId,
            title = title,
            amountCents = amountCents,
            paidByUserId = paidByUserId,
            splits = splits.map { ExpenseSplit(it.userId, it.amountCents, it.isCoveredBy) },
            createdAt = createdAt,
            currency = currency
        )

    private fun GroupExpense.toEntity(isPending: Boolean): CachedExpenseEntity =
        CachedExpenseEntity(
            id = id,
            groupId = groupId,
            title = title,
            amountCents = amountCents,
            currency = currency,
            paidByUserId = paidByUserId,
            createdAt = createdAt,
            isPending = isPending
        )

    private fun ExpenseSplit.toEntity(expenseId: String): CachedExpenseSplitEntity =
        CachedExpenseSplitEntity(
            expenseId = expenseId,
            userId = userId,
            amountCents = amountCents,
            isCoveredBy = isCoveredBy
        )
}
