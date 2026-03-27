package com.example.divvy.ui.home.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.ActivityRepository
import com.example.divvy.backend.AuthRepository
import com.example.divvy.models.ActivityFeedItem
import com.example.divvy.backend.DataResult
import com.example.divvy.backend.ExpensesRepository
import com.example.divvy.backend.GroupRepository
import com.example.divvy.components.GroupIcon
import com.example.divvy.models.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val groups: List<Group> = emptyList(),
    val totalOwedCents: Long = 0L,
    val totalOwingCents: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activityItems: List<ActivityFeedItem> = emptyList(),
    val showCreateGroupSheet: Boolean = false,
    val createName: String = "",
    val createIcon: GroupIcon = GroupIcon.Group,
    val isCreating: Boolean = false
) {
    val formattedOwed: String
        get() {
            val dollars = totalOwedCents / 100.0
            return "$${String.format("%.2f", dollars)}"
        }

    val formattedOwing: String
        get() {
            val dollars = totalOwingCents / 100.0
            return "$${String.format("%.2f", dollars)}"
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val expensesRepository: ExpensesRepository,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val myUserId = authRepository.getCurrentUserId()

    init {
        viewModelScope.launch {
            combine(
                groupRepository.listGroups(),
                expensesRepository.observeAllGroupExpenses()
            ) { groupsResult, expenses ->
                groupsResult to expenses
            }.collect { (result, allExpenses) ->
                _uiState.update { current ->
                    when (result) {
                        is DataResult.Loading -> current.copy(isLoading = true, errorMessage = null)
                        is DataResult.Error -> current.copy(isLoading = false, errorMessage = result.message)
                        is DataResult.Success -> {
                            val computedByGroup = balanceByGroupForUser(allExpenses, myUserId)
                            val groups = result.data.map { g ->
                                g.copy(balanceCents = computedByGroup[g.id] ?: 0L)
                            }
                            current.copy(
                                groups = groups,
                                totalOwedCents = groups.filter { g -> g.balanceCents > 0 }.sumOf { g -> g.balanceCents },
                                totalOwingCents = groups.filter { g -> g.balanceCents < 0 }.sumOf { g -> kotlin.math.abs(g.balanceCents) },
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            expensesRepository.refreshAllExpenses()
            activityRepository.refreshActivityFeed() // Initial fetch
            activityRepository.getGlobalActivityFeed().collect { result ->
                _uiState.update { current ->
                    when (result) {
                        is DataResult.Success -> current.copy(activityItems = result.data)
                        // Activity feed errors don't block the UI for now, or could show a snackbar
                        else -> current
                    }
                }
            }
        }
    }

    fun onRetry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            groupRepository.refreshGroups()
            activityRepository.refreshActivityFeed()
        }
    }

    fun onCreateGroupClick() {
        _uiState.update { it.copy(showCreateGroupSheet = true, createName = "", createIcon = GroupIcon.Group) }
    }

    fun onCreateGroupDismiss() {
        _uiState.update { it.copy(showCreateGroupSheet = false) }
    }

    fun onCreateNameChange(value: String) { _uiState.update { it.copy(createName = value) } }
    fun onCreateIconSelected(icon: GroupIcon) { _uiState.update { it.copy(createIcon = icon) } }

    fun submitCreateGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            groupRepository.createGroup(_uiState.value.createName.trim(), _uiState.value.createIcon)
            _uiState.update { it.copy(isCreating = false, showCreateGroupSheet = false) }
        }
    }

    private fun balanceByGroupForUser(
        allExpenses: List<com.example.divvy.models.GroupExpense>,
        userId: String
    ): Map<String, Long> {
        return allExpenses.groupBy { it.groupId }.mapValues { (_, expenses) ->
            expenses.sumOf { expense ->
                val paid = if (expense.paidByUserId == userId) expense.amountCents else 0L
                val share = expense.splits.find { it.userId == userId }?.amountCents ?: 0L
                paid - share
            }
        }
    }
}
