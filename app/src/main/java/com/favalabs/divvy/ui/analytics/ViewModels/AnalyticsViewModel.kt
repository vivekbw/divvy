package com.favalabs.divvy.ui.analytics.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.AuthRepository
import com.favalabs.divvy.backend.DataResult
import com.favalabs.divvy.backend.ExpensesRepository
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.components.GroupIcon
import com.favalabs.divvy.models.formatAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupSpending(
    val groupId: String,
    val groupName: String,
    val groupIcon: GroupIcon,
    val yourShareCents: Long,
    val totalCents: Long
)

data class AnalyticsUiState(
    val totalSpentCents: Long = 0L,
    val yourShareCents: Long = 0L,
    val youPaidCents: Long = 0L,
    val expenseCount: Int = 0,
    val groupSpending: List<GroupSpending> = emptyList(),
    val isLoading: Boolean = true,
    val totalSpentByCurrency: Map<String, Long> = emptyMap(),
    val yourShareByCurrency: Map<String, Long> = emptyMap(),
    val youPaidByCurrency: Map<String, Long> = emptyMap()
) {
    val formattedTotalSpent: String
        get() = formatCurrencyMap(totalSpentByCurrency, totalSpentCents)

    val formattedYourShare: String
        get() = formatCurrencyMap(yourShareByCurrency, yourShareCents)

    val formattedYouPaid: String
        get() = formatCurrencyMap(youPaidByCurrency, youPaidCents)

    val overpaidCents: Long
        get() = youPaidCents - yourShareCents

    val formattedOverpaid: String
        get() = formatAmount(kotlin.math.abs(overpaidCents), "USD")

    val isOverpaying: Boolean
        get() = overpaidCents > 0

    val maxGroupSpendingCents: Long
        get() = groupSpending.maxOfOrNull { it.yourShareCents } ?: 1L

    private fun formatCurrencyMap(map: Map<String, Long>, fallbackCents: Long): String {
        if (map.isEmpty()) return formatAmount(fallbackCents, "USD")
        return map.entries
            .filter { it.value > 0 }
            .joinToString(", ") { formatAmount(it.value, it.key) }
            .ifEmpty { formatAmount(0L, "USD") }
    }
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val expensesRepository: ExpensesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val myUserId = authRepository.getCurrentUserId()

    init {
        viewModelScope.launch { expensesRepository.refreshAllExpenses() }

        viewModelScope.launch {
            combine(
                groupRepository.listGroups(),
                expensesRepository.observeAllGroupExpenses()
            ) { groupsResult, expenses ->
                groupsResult to expenses
            }.collect { (groupsResult, allExpenses) ->
                val groups = (groupsResult as? DataResult.Success)?.data ?: return@collect
                val realExpenses = allExpenses.filter { it.title != "Settlement" || it.splits.size > 1 }
                val groupMap = groups.associateBy { it.id }

                var totalSpent = 0L
                var yourShare = 0L
                var youPaid = 0L
                val totalSpentByCurrency = mutableMapOf<String, Long>()
                val yourShareByCurrency = mutableMapOf<String, Long>()
                val youPaidByCurrency = mutableMapOf<String, Long>()
                val perGroup = mutableMapOf<String, Long>()
                val perGroupTotal = mutableMapOf<String, Long>()

                for (expense in realExpenses) {
                    val cur = expense.currency
                    totalSpent += expense.amountCents
                    totalSpentByCurrency[cur] = (totalSpentByCurrency[cur] ?: 0L) + expense.amountCents

                    val myShare = expense.splits
                        .find { it.userId == myUserId }
                        ?.amountCents ?: 0L
                    yourShare += myShare
                    yourShareByCurrency[cur] = (yourShareByCurrency[cur] ?: 0L) + myShare

                    if (expense.paidByUserId == myUserId) {
                        youPaid += expense.amountCents
                        youPaidByCurrency[cur] = (youPaidByCurrency[cur] ?: 0L) + expense.amountCents
                    }

                    perGroup[expense.groupId] = (perGroup[expense.groupId] ?: 0L) + myShare
                    perGroupTotal[expense.groupId] = (perGroupTotal[expense.groupId] ?: 0L) + expense.amountCents
                }

                val groupSpending = perGroup.entries
                    .sortedByDescending { it.value }
                    .map { (gId, share) ->
                        val group = groupMap[gId]
                        GroupSpending(
                            groupId = gId,
                            groupName = group?.name ?: "Unknown",
                            groupIcon = group?.icon ?: GroupIcon.Group,
                            yourShareCents = share,
                            totalCents = perGroupTotal[gId] ?: 0L
                        )
                    }

                _uiState.value = AnalyticsUiState(
                    totalSpentCents = totalSpent,
                    yourShareCents = yourShare,
                    youPaidCents = youPaid,
                    expenseCount = realExpenses.size,
                    groupSpending = groupSpending,
                    isLoading = false,
                    totalSpentByCurrency = totalSpentByCurrency,
                    yourShareByCurrency = yourShareByCurrency,
                    youPaidByCurrency = youPaidByCurrency
                )
            }
        }
    }
}
