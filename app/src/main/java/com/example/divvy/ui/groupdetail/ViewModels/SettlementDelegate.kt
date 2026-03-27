package com.example.divvy.ui.groupdetail.ViewModels

import com.example.divvy.backend.BalanceRepository
import com.example.divvy.backend.ExpensesRepository
import com.example.divvy.backend.GroupRepository
import com.example.divvy.models.ExpenseSplit
import com.example.divvy.models.MemberBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettlementState(
    val expandedMemberId: String? = null,
    val settleMode: SettleMode? = null,
    val settleAmount: String = "",
    val isSettling: Boolean = false,
    val errorMessage: String? = null
)

class SettlementDelegate(
    private val groupId: String,
    private val myUserId: String,
    private val scope: CoroutineScope,
    private val expensesRepository: ExpensesRepository,
    private val balanceRepository: BalanceRepository,
    private val groupRepository: GroupRepository,
    private val getMemberBalances: () -> List<MemberBalance>
) {
    private val _state = MutableStateFlow(SettlementState())
    val state: StateFlow<SettlementState> = _state.asStateFlow()

    fun onMemberClick(userId: String) {
        _state.update { s ->
            if (s.expandedMemberId == userId)
                s.copy(expandedMemberId = null, settleMode = null, settleAmount = "", errorMessage = null)
            else
                s.copy(expandedMemberId = userId, settleMode = null, settleAmount = "", errorMessage = null)
        }
    }

    fun onSettleModeSelected(mode: SettleMode) {
        _state.update { s ->
            val amount = if (mode == SettleMode.Fully) {
                val balance = getMemberBalances()
                    .find { it.userId == s.expandedMemberId }?.balanceCents ?: 0L
                String.format("%.2f", kotlin.math.abs(balance) / 100.0)
            } else ""
            s.copy(settleMode = mode, settleAmount = amount, errorMessage = null)
        }
    }

    fun onSettleAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dot = filtered.indexOf('.')
        if (dot != -1 && filtered.length - dot - 1 > 2) return
        _state.update { it.copy(settleAmount = filtered) }
    }

    fun onConfirmSettle(userId: String) {
        val s = _state.value
        if (s.isSettling) return
        if (s.settleMode == null) return
        val amountCents = (s.settleAmount.toDoubleOrNull() ?: return).let {
            (it * 100).toLong()
        }
        if (amountCents <= 0) return
        val balance = getMemberBalances().find { it.userId == userId }?.balanceCents ?: return
        if (amountCents > kotlin.math.abs(balance)) {
            _state.update { it.copy(errorMessage = "Amount cannot exceed the current balance.") }
            return
        }

        // Settlement is represented as "payer paid for receiver" for the settled amount.
        // If I owe them -> I pay, split assigned to them.
        // If they owe me -> they pay, split assigned to me.
        val (paidBy, splitUserId) = if (balance < 0) {
            myUserId to userId
        } else {
            userId to myUserId
        }

        scope.launch {
            _state.update { it.copy(isSettling = true, errorMessage = null) }
            try {
                expensesRepository.createExpenseWithSplits(
                    groupId = groupId,
                    description = "Settlement",
                    amountCents = amountCents,
                    currency = "USD",
                    splitMethod = "SETTLEMENT",
                    paidByUserId = paidBy,
                    splits = listOf(ExpenseSplit(splitUserId, amountCents))
                )
                balanceRepository.refreshBalances(groupId)
                expensesRepository.refreshGroupExpenses(groupId)
                groupRepository.refreshGroups()
                _state.update { SettlementState() }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isSettling = false,
                        errorMessage = "Could not complete settlement. Please try again."
                    )
                }
            }
        }
    }
}
