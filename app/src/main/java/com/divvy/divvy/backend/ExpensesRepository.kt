package com.divvy.divvy.backend

import com.divvy.divvy.models.Expense
import com.divvy.divvy.models.ExpenseSplit
import com.divvy.divvy.models.GroupExpense
import com.divvy.divvy.models.ReceiptItemRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import java.util.NoSuchElementException
import java.util.UUID
import javax.inject.Inject

interface ExpensesRepository {
    suspend fun listExpenses(): List<Expense>
    suspend fun listExpensesByGroup(groupId: String): List<Expense>
    suspend fun listGroupExpenses(groupId: String): List<GroupExpense>
    suspend fun createExpense(
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        paidByUserId: String
    ): Expense
    suspend fun createExpenseWithSplits(
        groupId: String,
        description: String,
        amountCents: Long,
        currency: String,
        splitMethod: String,
        paidByUserId: String,
        splits: List<ExpenseSplit>
    ): GroupExpense
    suspend fun getExpenseById(expenseId: String): Expense?
    suspend fun getGroupExpenseById(expenseId: String): GroupExpense?
    suspend fun updateExpense(
        expenseId: String,
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        currency: String
    ): Expense
    suspend fun updateExpenseSplits(expenseId: String, splits: List<ExpenseSplit>)
    suspend fun deleteExpense(expenseId: String)
    suspend fun saveReceiptItems(items: List<ReceiptItemRow>)

    fun observeGroupExpenses(groupId: String): Flow<List<GroupExpense>>
    fun observeAllGroupExpenses(): Flow<List<GroupExpense>>
    suspend fun refreshGroupExpenses(groupId: String)
    suspend fun refreshAllExpenses()
}

class StubExpensesRepository @Inject constructor() : ExpensesRepository {

    private val expenses = mutableListOf<Expense>()
    private val splits = mutableMapOf<String, List<ExpenseSplit>>()
    private val _cache = MutableStateFlow<Map<String, List<GroupExpense>>>(emptyMap())

    override suspend fun listExpenses(): List<Expense> =
        expenses.toList()

    override suspend fun listExpensesByGroup(groupId: String): List<Expense> =
        expenses.filter { it.groupId == groupId }

    override suspend fun listGroupExpenses(groupId: String): List<GroupExpense> =
        expenses.filter { it.groupId == groupId }.map { it.toGroupExpense() }

    override suspend fun createExpense(
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        paidByUserId: String
    ): Expense {
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            merchant = description,
            amountCents = amountCents,
            splitMethod = splitMethod,
            currency = "USD",
            paidByUserId = paidByUserId,
            createdAt = Clock.System.now().toString()
        )
        expenses.add(expense)
        return expense
    }

    override suspend fun createExpenseWithSplits(
        groupId: String,
        description: String,
        amountCents: Long,
        currency: String,
        splitMethod: String,
        paidByUserId: String,
        splits: List<ExpenseSplit>
    ): GroupExpense {
        val expense = createExpense(groupId, description, amountCents, splitMethod, paidByUserId)
        this.splits[expense.id] = splits
        val groupExpense = expense.toGroupExpense()
        _cache.update { map ->
            map + (groupId to ((map[groupId] ?: emptyList()) + groupExpense))
        }
        return groupExpense
    }

    override suspend fun getExpenseById(expenseId: String): Expense? =
        expenses.find { it.id == expenseId }

    override suspend fun getGroupExpenseById(expenseId: String): GroupExpense? =
        expenses.find { it.id == expenseId }?.toGroupExpense()

    override suspend fun updateExpense(
        expenseId: String,
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        currency: String
    ): Expense {
        val index = expenses.indexOfFirst { it.id == expenseId }
        if (index == -1) throw NoSuchElementException("Expense with ID $expenseId not found")
        val updated = expenses[index].copy(
            groupId = groupId,
            merchant = description,
            amountCents = amountCents,
            splitMethod = splitMethod,
            currency = currency
        )
        expenses[index] = updated
        return updated
    }

    override suspend fun updateExpenseSplits(expenseId: String, splits: List<ExpenseSplit>) {
        if (expenses.none { it.id == expenseId })
            throw NoSuchElementException("Expense with ID $expenseId not found")
        this.splits[expenseId] = splits
    }

    override suspend fun deleteExpense(expenseId: String) {
        expenses.removeIf { it.id == expenseId }
        splits.remove(expenseId)
    }

    override suspend fun saveReceiptItems(items: List<ReceiptItemRow>) { }

    override fun observeGroupExpenses(groupId: String): Flow<List<GroupExpense>> =
        _cache.map { it[groupId] ?: emptyList() }

    override fun observeAllGroupExpenses(): Flow<List<GroupExpense>> =
        _cache.map { it.values.flatten() }

    override suspend fun refreshGroupExpenses(groupId: String) {
        val groupExpenses = listGroupExpenses(groupId)
        _cache.update { it + (groupId to groupExpenses) }
    }

    override suspend fun refreshAllExpenses() {
        val all = expenses.map { it.toGroupExpense() }
        _cache.value = all.groupBy { it.groupId }
    }

    private fun Expense.toGroupExpense() = GroupExpense(
        id = id,
        groupId = groupId,
        title = merchant,
        amountCents = amountCents,
        paidByUserId = paidByUserId,
        splits = splits[id] ?: emptyList(),
        createdAt = createdAt
    )
}
