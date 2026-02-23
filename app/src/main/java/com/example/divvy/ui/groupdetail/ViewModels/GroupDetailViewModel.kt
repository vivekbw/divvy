package com.example.divvy.ui.groupdetail.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.CURRENT_USER_ID
import com.example.divvy.backend.GroupRepository
import com.example.divvy.models.ActivityItem
import com.example.divvy.models.ExpenseSplit
import com.example.divvy.models.Group
import com.example.divvy.models.GroupExpense
import com.example.divvy.models.MemberBalance
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class SettleMode { Fully, Partially }

data class GroupDetailUiState(
    val group: Group = Group(id = "", name = ""),
    val memberBalances: List<MemberBalance> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = true,
    val expandedMemberId: String? = null,
    val settleMode: SettleMode? = null,
    val settleAmount: String = "",
    val isSettling: Boolean = false,
    val showManagePanel: Boolean = false,
    val leftGroup: Boolean = false
)

@HiltViewModel(assistedFactory = GroupDetailViewModel.Factory::class)
class GroupDetailViewModel @AssistedInject constructor(
    @Assisted private val groupId: String,
    private val repo: GroupRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(groupId: String): GroupDetailViewModel
    }

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.getGroup(groupId),
                repo.getMemberBalances(groupId),
                repo.getActivity(groupId)
            ) { group, balances, activity ->
                val sortedActivity = activity.sortedByDescending { it.timestamp }
                Triple(group, balances, sortedActivity)
            }.collect { (group, balances, activity) ->
                _uiState.update { current ->
                    current.copy(
                        group = group,
                        memberBalances = balances,
                        activity = activity,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onMemberClick(userId: String) {
        _uiState.update { state ->
            if (state.expandedMemberId == userId)
                state.copy(expandedMemberId = null, settleMode = null, settleAmount = "")
            else
                state.copy(expandedMemberId = userId, settleMode = null, settleAmount = "")
        }
    }

    fun onSettleModeSelected(mode: SettleMode) {
        _uiState.update { state ->
            val amount = if (mode == SettleMode.Fully) {
                val balance = state.memberBalances
                    .find { it.userId == state.expandedMemberId }?.balanceCents ?: 0L
                String.format("%.2f", kotlin.math.abs(balance) / 100.0)
            } else ""
            state.copy(settleMode = mode, settleAmount = amount)
        }
    }

    fun onSettleAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dot = filtered.indexOf('.')
        if (dot != -1 && filtered.length - dot - 1 > 2) return
        _uiState.update { it.copy(settleAmount = filtered) }
    }

    fun onGearClick() {
        _uiState.update { it.copy(showManagePanel = !it.showManagePanel) }
    }

    fun onLeaveGroup() {
        viewModelScope.launch {
            repo.leaveGroup(groupId)
            _uiState.update { it.copy(leftGroup = true) }
        }
    }

    fun onConfirmSettle(userId: String) {
        val state = _uiState.value
        val amountCents = (state.settleAmount.toDoubleOrNull() ?: return).let {
            (it * 100).toLong()
        }
        if (amountCents <= 0) return
        val balance = state.memberBalances.find { it.userId == userId }?.balanceCents ?: return

        val (paidBy, splitUserId) = if (balance < 0)
            Pair(CURRENT_USER_ID, userId)
        else
            Pair(userId, CURRENT_USER_ID)

        val expense = GroupExpense(
            id           = UUID.randomUUID().toString(),
            groupId      = groupId,
            title        = "Settlement",
            amountCents  = amountCents,
            paidByUserId = paidBy,
            splits       = listOf(ExpenseSplit(splitUserId, amountCents)),
            createdAt    = LocalDate.now().toString()
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSettling = true) }
            repo.addExpense(expense)
            _uiState.update {
                it.copy(isSettling = false, expandedMemberId = null,
                        settleMode = null, settleAmount = "")
            }
        }
    }
}
