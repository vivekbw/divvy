package com.example.divvy.ui.ledger.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.CURRENT_USER_ID
import com.example.divvy.backend.GroupRepository
import com.example.divvy.backend.dateLabel
import com.example.divvy.formatCents
import com.example.divvy.models.Group
import com.example.divvy.models.LedgerEntry
import com.example.divvy.models.LedgerEntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LedgerFilter { ALL, EXPENSES, SETTLEMENTS }

data class LedgerUiState(
    val allEntries: List<LedgerEntry> = emptyList(),
    val filteredEntries: List<LedgerEntry> = emptyList(),
    val filter: LedgerFilter = LedgerFilter.ALL,
    val selectedGroupId: String? = null,
    val groupOptions: List<Group> = emptyList(),
    val isLoading: Boolean = true
) {
    val netBalanceCents: Long get() {
        val earned = allEntries.filter { it.paidByCurrentUser && it.type == LedgerEntryType.EXPENSE }.sumOf { it.amountCents }
        val owed = allEntries.filter { !it.paidByCurrentUser && it.type == LedgerEntryType.EXPENSE }.sumOf { it.amountCents }
        return earned - owed
    }
    val formattedNetBalance: String get() = formatCents(netBalanceCents)
    val isNetPositive: Boolean get() = netBalanceCents >= 0
}

class LedgerViewModel(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(groupRepository.listGroups(), groupRepository.getAllExpenses()) { groups, allExpenses -> groups to allExpenses }
                .collect { (groups, allExpenses) ->
                    val groupNameMap = groups.associate { it.id to it.name }
                    val memberNameMap = mutableMapOf<String, String>()
                    memberNameMap[CURRENT_USER_ID] = "You"
                    for (group in groups) {
                        val members = groupRepository.getMembers(group.id).first()
                        for (member in members) memberNameMap[member.userId] = member.name
                    }
                    val entries = allExpenses.map { expense ->
                        val isSettlement = expense.title == "Settlement" && expense.splits.size == 1
                        val toUserId = if (isSettlement) expense.splits.first().userId else ""
                        LedgerEntry(id = expense.id,
                            type = if (isSettlement) LedgerEntryType.SETTLEMENT else LedgerEntryType.EXPENSE,
                            title = expense.title, amountCents = expense.amountCents, groupId = expense.groupId,
                            paidByUserId = expense.paidByUserId, dateLabel = dateLabel(expense.createdAt),
                            toUserId = toUserId, groupName = groupNameMap[expense.groupId] ?: "",
                            paidByName = memberNameMap[expense.paidByUserId] ?: "Unknown",
                            toName = if (toUserId.isNotBlank()) memberNameMap[toUserId] ?: "Unknown" else "",
                            paidByCurrentUser = expense.paidByUserId == CURRENT_USER_ID)
                    }.sortedByDescending { it.dateLabel }
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(allEntries = entries,
                        filteredEntries = applyFilters(entries, currentState.filter, currentState.selectedGroupId),
                        groupOptions = groups, isLoading = false)
                }
        }
    }

    fun onFilterSelected(filter: LedgerFilter) { _uiState.update { state -> state.copy(filter = filter, filteredEntries = applyFilters(state.allEntries, filter, state.selectedGroupId)) } }
    fun onGroupSelected(groupId: String?) { _uiState.update { state -> state.copy(selectedGroupId = groupId, filteredEntries = applyFilters(state.allEntries, state.filter, groupId)) } }

    private fun applyFilters(entries: List<LedgerEntry>, filter: LedgerFilter, groupId: String?): List<LedgerEntry> {
        var result = entries
        when (filter) {
            LedgerFilter.EXPENSES -> result = result.filter { it.type == LedgerEntryType.EXPENSE }
            LedgerFilter.SETTLEMENTS -> result = result.filter { it.type == LedgerEntryType.SETTLEMENT }
            LedgerFilter.ALL -> {}
        }
        if (groupId != null) result = result.filter { it.groupId == groupId }
        return result
    }
}
