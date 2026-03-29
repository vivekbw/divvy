package com.example.divvy.ui.groupdetail.ViewModels

import com.example.divvy.backend.BalanceRepository
import com.example.divvy.backend.ExpensesRepository
import com.example.divvy.backend.GroupRepository
import com.example.divvy.models.MemberBalance
import com.example.divvy.models.SimplifiedPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettlementState(
    val expandedMemberId: String? = null,
    val expandedCurrency: String? = null,
    val settleMode: SettleMode? = null,
    val settleAmount: String = "",
    val isSettling: Boolean = false,
    val currency: String = "USD",
    val expandedFromUserId: String? = null,
    val expandedToUserId: String? = null,
    val expandedAmountCents: Long = 0L
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

    /**
     * Called when a SimplifiedPaymentCard is tapped.
     * fromUserId/toUserId/amountCents come directly from the SimplifiedPayment —
     * direction is always correct, no re-derivation needed.
     */
    fun onMemberClick(
        userId: String,
        currency: String,
        simplifiedPayments: List<SimplifiedPayment>
    ) {
        _state.update { s ->
            if (s.expandedMemberId == userId && s.expandedCurrency == currency) {
                SettlementState()
            } else {
                val payment = simplifiedPayments.firstOrNull { p ->
                    p.currency == currency && (p.fromUserId == userId || p.toUserId == userId)
                }
                s.copy(
                    expandedMemberId   = userId,
                    expandedCurrency   = currency,
                    expandedFromUserId = payment?.fromUserId,
                    expandedToUserId   = payment?.toUserId,
                    expandedAmountCents = payment?.amountCents ?: 0L,
                    currency           = currency,
                    settleMode         = null,
                    settleAmount       = ""
                )
            }
        }
    }

    fun onSettleModeSelected(mode: SettleMode, currency: String = "USD") {
        _state.update { s ->
            val amount = if (mode == SettleMode.Fully) {
                String.format("%.2f", s.expandedAmountCents / 100.0)
            } else ""
            s.copy(settleMode = mode, settleAmount = amount, currency = currency)
        }
    }

    fun onSettleAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dot = filtered.indexOf('.')
        if (dot != -1 && filtered.length - dot - 1 > 2) return
        _state.update { it.copy(settleAmount = filtered) }
    }

    fun onConfirmSettle() {
        val s = _state.value
        val fromUserId  = s.expandedFromUserId ?: return
        val toUserId    = s.expandedToUserId   ?: return
        val amountCents = (s.settleAmount.toDoubleOrNull() ?: return).let { (it * 100).toLong() }
        if (amountCents <= 0) return

        scope.launch {
            _state.update { it.copy(isSettling = true) }
            expensesRepository.createSettlement(
                groupId     = groupId,
                payerId     = fromUserId,
                payeeId     = toUserId,
                amountCents = amountCents
            )
            balanceRepository.clearCache(groupId)
            balanceRepository.refreshBalances(groupId)
            groupRepository.refreshGroups()
            _state.update { SettlementState() }
        }
    }
}