package com.example.divvy.ui.ledger.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.LedgerRepository
import com.example.divvy.models.LedgerEntry
import com.example.divvy.models.LedgerEntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LedgerFilter { ALL, EXPENSES, SETTLEMENTS }

data class LedgerUiState(
    val allEntries: List<LedgerEntry> = emptyList(),
    val filteredEntries: List<LedgerEntry> = emptyList(),
    val filter: LedgerFilter = LedgerFilter.ALL,
    val selectedGroupId: String? = null,
    val groupOptions: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = true
) {
    val netBalanceCents: Long
        get() {
            val earned = allEntries
                .filter { it.paidByCurrentUser && it.type == LedgerEntryType.EXPENSE }
                .sumOf { it.amountCents }
            val owed = allEntries
                .filter { !it.paidByCurrentUser && it.type == LedgerEntryType.EXPENSE }
                .sumOf { it.amountCents }
            return earned - owed
        }

    val formattedNetBalance: String
        get() {
            val dollars = kotlin.math.abs(netBalanceCents) / 100.0
            return "$${String.format("%.2f", dollars)}"
        }

    val isNetPositive: Boolean get() = netBalanceCents >= 0
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val entries = ledgerRepository.listEntries()
            val groups = ledgerRepository.getGroupNames()
            _uiState.update {
                it.copy(
                    allEntries = entries,
                    filteredEntries = entries,
                    groupOptions = groups,
                    isLoading = false
                )
            }
        }
    }

    fun onFilterSelected(filter: LedgerFilter) {
        _uiState.update { state ->
            state.copy(filter = filter, filteredEntries = applyFilters(state.allEntries, filter, state.selectedGroupId))
        }
    }

    fun onGroupSelected(groupId: String?) {
        _uiState.update { state ->
            state.copy(selectedGroupId = groupId, filteredEntries = applyFilters(state.allEntries, state.filter, groupId))
        }
    }

    private fun applyFilters(
        entries: List<LedgerEntry>,
        filter: LedgerFilter,
        groupId: String?
    ): List<LedgerEntry> {
        var result = entries
        when (filter) {
            LedgerFilter.EXPENSES -> result = result.filter { it.type == LedgerEntryType.EXPENSE }
            LedgerFilter.SETTLEMENTS -> result = result.filter { it.type == LedgerEntryType.SETTLEMENT }
            LedgerFilter.ALL -> {}
        }
        if (groupId != null) {
            result = result.filter { it.groupId == groupId }
        }
        return result
    }
}
