package com.favalabs.divvy.backend

import com.favalabs.divvy.models.Expense
import com.favalabs.divvy.models.ExpenseSplit
import com.favalabs.divvy.models.GroupExpense
import com.favalabs.divvy.models.ReceiptItemRow
import io.github.jan.supabase.SupabaseClient
import io.sentry.Sentry
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseExpensesRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : ExpensesRepository {

    companion object {
        private const val CACHE_TTL_MS = 30_000L
    }

    private val _expenses = MutableStateFlow<Map<String, List<GroupExpense>>>(emptyMap())
    @Volatile private var _lastAllRefreshMs = 0L
    private val _lastGroupRefreshMs = ConcurrentHashMap<String, Long>()

    override suspend fun listExpenses(): List<Expense> =
        supabaseClient.from("expenses").select().decodeList()

    override suspend fun listExpensesByGroup(groupId: String): List<Expense> =
        supabaseClient.from("expenses")
            .select { filter { eq("group_id", groupId) } }
            .decodeList()

    override suspend fun getExpenseById(expenseId: String): Expense? =
        supabaseClient.from("expenses")
            .select { filter { eq("id", expenseId) } }
            .decodeSingleOrNull()

    override suspend fun listGroupExpenses(groupId: String): List<GroupExpense> =
        supabaseClient.from("group_expenses_with_splits")
            .select { filter { eq("group_id", groupId) } }
            .decodeList()

    override suspend fun getGroupExpenseById(expenseId: String): GroupExpense? {
        val expense = getExpenseById(expenseId) ?: return null
        val splits = supabaseClient.from("expense_splits")
            .select { filter { eq("expense_id", expenseId) } }
            .decodeList<ExpenseSplit>()
        return GroupExpense(
            id = expense.id,
            groupId = expense.groupId,
            title = expense.merchant,
            amountCents = expense.amountCents,
            paidByUserId = expense.paidByUserId,
            splits = splits,
            createdAt = expense.createdAt,
            currency = expense.currency
        )
    }

    override suspend fun createExpense(
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        paidByUserId: String
    ): Expense {
        val expense = Expense(
            groupId = groupId,
            merchant = description,
            amountCents = amountCents,
            splitMethod = splitMethod,
            currency = "USD",
            paidByUserId = paidByUserId
        )
        return supabaseClient.from("expenses")
            .insert(expense) { select() }
            .decodeSingle()
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
        val params = buildJsonObject {
            put("p_group_id", groupId)
            put("p_merchant", description)
            put("p_amount_cents", amountCents)
            put("p_currency", currency)
            put("p_split_method", splitMethod)
            put("p_paid_by", paidByUserId)
            putJsonArray("p_splits") {
                splits.forEach { split ->
                    add(buildJsonObject {
                        put("user_id", split.userId)
                        put("amount_cents", split.amountCents)
                        split.isCoveredBy?.let { put("is_covered_by", it) }
                    })
                }
            }
        }

        val expenseId = supabaseClient.postgrest
            .rpc("create_expense_with_splits", params)
            .decodeAs<String>()

        val groupExpense = getGroupExpenseById(expenseId)
            ?: error("Failed to fetch expense after creation: $expenseId")

        _expenses.update { map ->
            map + (groupId to ((map[groupId] ?: emptyList()) + groupExpense))
        }
        _lastAllRefreshMs = 0L
        _lastGroupRefreshMs.remove(groupId)
        return groupExpense
    }

    override suspend fun updateExpense(
        expenseId: String,
        groupId: String,
        description: String,
        amountCents: Long,
        splitMethod: String,
        currency: String
    ): Expense =
        supabaseClient.from("expenses")
            .update({
                set("group_id", groupId)
                set("merchant", description)
                set("amount_cents", amountCents)
                set("split_method", splitMethod)
                set("currency", currency)
            }) {
                select()
                filter { eq("id", expenseId) }
            }
            .decodeSingle()

    override suspend fun updateExpenseSplits(expenseId: String, splits: List<ExpenseSplit>) {
        val params = buildJsonObject {
            put("p_expense_id", expenseId)
            putJsonArray("p_splits") {
                splits.forEach { split ->
                    add(buildJsonObject {
                        put("user_id", split.userId)
                        put("amount_cents", split.amountCents)
                        split.isCoveredBy?.let { put("is_covered_by", it) }
                    })
                }
            }
        }
        supabaseClient.postgrest.rpc("update_expense_splits", params)
        _lastAllRefreshMs = 0L
        _lastGroupRefreshMs.clear()
    }

    override suspend fun deleteExpense(expenseId: String) {
        supabaseClient.from("expenses")
            .delete { filter { eq("id", expenseId) } }
        _lastAllRefreshMs = 0L
        _lastGroupRefreshMs.clear()
    }

    override suspend fun saveReceiptItems(items: List<ReceiptItemRow>) {
        if (items.isEmpty()) return
        supabaseClient.from("receipt_items").insert(items)
    }

    override fun observeGroupExpenses(groupId: String): Flow<List<GroupExpense>> =
        _expenses.map { it[groupId] ?: emptyList() }

    override fun observeAllGroupExpenses(): Flow<List<GroupExpense>> =
        _expenses.map { it.values.flatten() }

    override suspend fun refreshGroupExpenses(groupId: String) {
        val now = System.currentTimeMillis()
        val lastRefresh = _lastGroupRefreshMs[groupId] ?: 0L
        if (now - lastRefresh < CACHE_TTL_MS && _expenses.value.containsKey(groupId)) return

        val expenses = listGroupExpenses(groupId)
        _expenses.update { it + (groupId to expenses) }
        _lastGroupRefreshMs[groupId] = now
    }

    override suspend fun refreshAllExpenses() {
        val now = System.currentTimeMillis()
        if (now - _lastAllRefreshMs < CACHE_TTL_MS && _expenses.value.isNotEmpty()) return

        try {
            val all = supabaseClient.from("group_expenses_with_splits")
                .select()
                .decodeList<GroupExpense>()
            _expenses.value = all.groupBy { it.groupId }
            _lastAllRefreshMs = now
            _expenses.value.keys.forEach { _lastGroupRefreshMs[it] = now }
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
    }
}
