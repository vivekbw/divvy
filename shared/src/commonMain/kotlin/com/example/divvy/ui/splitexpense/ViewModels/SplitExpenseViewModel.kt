package com.example.divvy.ui.splitexpense.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.CURRENT_USER_ID
import com.example.divvy.backend.GroupRepository
import com.example.divvy.models.Group
import com.example.divvy.models.GroupExpense
import com.example.divvy.models.splitEqually
import com.example.divvy.randomUuidString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

enum class SplitMethod(val title: String, val subtitle: String) {
    Equally("Split Equally", "Everyone pays the same amount"),
    ByPercentage("By Percentage", "Custom percentage for each person"),
    ByItems("By Items", "Assign individual items to people")
}

data class SplitExpenseUiState(
    val amount: String = "",
    val description: String = "",
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val splitMethod: SplitMethod = SplitMethod.Equally,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false
)

class SplitExpenseViewModel(
    private val scannedAmount: String,
    private val scannedDescription: String,
    private val preselectedGroupId: String,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitExpenseUiState(isLoading = true, amount = scannedAmount, description = scannedDescription))
    val uiState: StateFlow<SplitExpenseUiState> = _uiState.asStateFlow()

    sealed interface SplitEvent {
        data object Created : SplitEvent
        data class GoToAssignItems(val groupId: String, val amount: String, val description: String) : SplitEvent
        data class GoToSplitByPercentage(val groupId: String, val amount: String, val description: String) : SplitEvent
    }

    private val _events = Channel<SplitEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            groupRepository.listGroups().collect { groups ->
                _uiState.update { current ->
                    current.copy(groups = groups,
                        selectedGroupId = current.selectedGroupId ?: preselectedGroupId.takeIf { it.isNotEmpty() } ?: groups.firstOrNull()?.id,
                        isLoading = false)
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { c -> c.isDigit() || c == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return
        _uiState.update { it.copy(amount = filtered) }
    }

    fun onDescriptionChange(value: String) { _uiState.update { it.copy(description = value) } }
    fun onGroupSelected(groupId: String) { _uiState.update { it.copy(selectedGroupId = groupId) } }
    fun onSplitMethodSelected(method: SplitMethod) { _uiState.update { it.copy(splitMethod = method) } }

    fun onCreateSplit() {
        val state = _uiState.value
        val groupId = state.selectedGroupId ?: return
        if (state.amount.toDoubleOrNull() == null) return
        val desc = state.description.trim().ifBlank { "Expense" }
        val amountCents = (state.amount.toDouble() * 100).toLong()

        when (state.splitMethod) {
            SplitMethod.ByItems -> { viewModelScope.launch { _events.send(SplitEvent.GoToAssignItems(groupId, state.amount, desc)) }; return }
            SplitMethod.ByPercentage -> { viewModelScope.launch { _events.send(SplitEvent.GoToSplitByPercentage(groupId, state.amount, desc)) }; return }
            SplitMethod.Equally -> Unit
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            val members = groupRepository.getMembers(groupId).first()
            val allUserIds = listOf(CURRENT_USER_ID) + members.map { it.userId }
            val splits = splitEqually(amountCents, allUserIds)
            groupRepository.addExpense(GroupExpense(
                id = randomUuidString(), groupId = groupId, title = desc, amountCents = amountCents,
                paidByUserId = CURRENT_USER_ID, splits = splits,
                createdAt = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            ))
            _uiState.update { it.copy(isCreating = false) }
            _events.send(SplitEvent.Created)
        }
    }
}
