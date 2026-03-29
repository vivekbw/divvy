package com.example.divvy.ui.home.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.ActivityRepository
import com.example.divvy.models.ActivityFeedItem
import com.example.divvy.backend.DataResult
import com.example.divvy.backend.GroupRepository
import com.example.divvy.components.GroupIcon
import com.example.divvy.models.Group
import com.example.divvy.models.formatAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val groups: List<Group> = emptyList(),
    val totalOwedCents: Long = 0L,
    val totalOwingCents: Long = 0L,
    val owedByCurrency: Map<String, Long> = emptyMap(),
    val owingByCurrency: Map<String, Long> = emptyMap(),
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
            if (owedByCurrency.isEmpty()) return formatAmount(totalOwedCents, "USD")
            return owedByCurrency.entries
                .filter { it.value > 0 }
                .joinToString(", ") { formatAmount(it.value, it.key) }
                .ifEmpty { formatAmount(0L, "USD") }
        }

    val formattedOwing: String
        get() {
            if (owingByCurrency.isEmpty()) return formatAmount(totalOwingCents, "USD")
            return owingByCurrency.entries
                .filter { it.value > 0 }
                .joinToString(", ") { formatAmount(it.value, it.key) }
                .ifEmpty { formatAmount(0L, "USD") }
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.refreshGroups() // Explicitly trigger groups refresh on startup
            groupRepository.listGroups().collect { result ->
                _uiState.update { current ->
                    when (result) {
                        is DataResult.Loading -> current.copy(isLoading = true, errorMessage = null)
                        is DataResult.Error -> current.copy(isLoading = false, errorMessage = result.message)
                        is DataResult.Success -> {
                            val groups = result.data
                            // Compute per-currency owed/owing totals
                            val owed = mutableMapOf<String, Long>()
                            val owing = mutableMapOf<String, Long>()
                            for (group in groups) {
                                for (bal in group.balances) {
                                    if (bal.balanceCents > 0) {
                                        owed[bal.currency] = (owed[bal.currency] ?: 0L) + bal.balanceCents
                                    } else if (bal.balanceCents < 0) {
                                        owing[bal.currency] = (owing[bal.currency] ?: 0L) + kotlin.math.abs(bal.balanceCents)
                                    }
                                }
                            }
                            current.copy(
                                groups = groups,
                                totalOwedCents = owed.values.sum(),
                                totalOwingCents = owing.values.sum(),
                                owedByCurrency = owed,
                                owingByCurrency = owing,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            activityRepository.refreshActivityFeed() // Initial fetch
            activityRepository.getGlobalActivityFeed().collect { result ->
                _uiState.update { current ->
                    when (result) {
                        is DataResult.Success -> current.copy(activityItems = result.data)
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
}
