package com.favalabs.divvy.ui.assignitems.ViewModels

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.ActivityRepository
import com.favalabs.divvy.backend.AuthRepository
import com.favalabs.divvy.backend.BalanceRepository
import com.favalabs.divvy.backend.ExpensesRepository
import com.favalabs.divvy.backend.FriendsRepository
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.MemberRepository
import com.favalabs.divvy.backend.ScannedReceiptStore
import com.favalabs.divvy.models.ParsedReceipt
import com.favalabs.divvy.models.ExpenseSplit
import com.favalabs.divvy.models.ReceiptItemRow
import com.favalabs.divvy.models.formatAmount
import com.favalabs.divvy.models.splitEqually
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.sentry.Sentry
import java.util.UUID

data class AssignMember(
    val id: String,
    val name: String,
    val color: Color
)

data class AssignOwedAmount(
    val memberId: String,
    val baseAmountCents: Long,
    val effectiveAmountCents: Long,
    val coveredByUserId: String? = null,
    val coveredMemberIds: List<String> = emptyList(),
)

data class ReceiptItem(
    val id: String,
    val name: String,
    val priceCents: Long,
    val priceText: String = if (priceCents > 0) String.format("%.2f", priceCents / 100.0) else ""
) {
    fun formattedPrice(currencyCode: String = "USD"): String =
        formatAmount(priceCents, currencyCode)

    val formattedPrice: String
        get() = formatAmount(priceCents, "USD")
}

data class AssignItemsUiState(
    val description: String = "",
    val amountDisplay: String = "",
    val currency: String = "USD",
    val members: List<AssignMember> = emptyList(),
    val items: List<ReceiptItem> = emptyList(),
    val assignments: Map<String, Set<String>> = emptyMap(),
    val expandedItemId: String? = null,
    val paidByUserId: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val coveredBy: Map<String, String> = emptyMap(),
    val expandedCoveringMemberId: String? = null,
    val isManualMode: Boolean = false,
    val scannedReceipt: ParsedReceipt? = null,
    val editingItemId: String? = null,
    val taxEnabled: Boolean = false,
    val taxIsPercent: Boolean = false,
    val taxText: String = "",
    val tipEnabled: Boolean = false,
    val tipIsPercent: Boolean = false,
    val tipText: String = "",
    val discountEnabled: Boolean = false,
    val discountText: String = "",
) {
    private val enteredTotalCents: Long
        get() = ((amountDisplay.toDoubleOrNull() ?: 0.0) * 100).toLong()

    val subtotalCents: Long
        get() = items.sumOf { it.priceCents }

    val formattedSubtotal: String
        get() = formatAmount(subtotalCents, currency)

    val assignedItemTotalCents: Long
        get() = items.sumOf { item ->
            if (assignments[item.id].orEmpty().isNotEmpty()) item.priceCents else 0L
        }

    val assignmentProgress: Float
        get() = if (subtotalCents <= 0L) 0f else (assignedItemTotalCents.toFloat() / subtotalCents.toFloat()).coerceIn(0f, 1f)

    val isCoverageComplete: Boolean
        get() = subtotalCents > 0L && assignedItemTotalCents == subtotalCents

    val formattedAssignedItemTotal: String
        get() = formatAmount(assignedItemTotalCents, currency)

    val taxCents: Long
        get() = calculateTaxCents()

    val tipCents: Long
        get() = calculateTipCents()

    val discountCents: Long
        get() = calculateDiscountCents()

    val calculatedTotalCents: Long
        get() = subtotalCents + taxCents + tipCents - discountCents

    val formattedTax: String
        get() = formatSignedAmount(taxCents)

    val formattedTip: String
        get() = formatSignedAmount(tipCents)

    val formattedDiscount: String
        get() = formatSignedAmount(-discountCents)

    val formattedCalculatedTotal: String
        get() = formatAmount(calculatedTotalCents, currency)

    val formattedEnteredTotal: String
        get() = formatAmount(enteredTotalCents, currency)

    val totalDifferenceCents: Long
        get() = calculatedTotalCents - enteredTotalCents

    val formattedTotalDifference: String
        get() = formatSignedAmount(totalDifferenceCents)

    val isTotalMatch: Boolean
        get() = calculatedTotalCents == enteredTotalCents

    val canSubmit: Boolean
        get() = !isSaving && isTotalMatch && isCoverageComplete

    val perUserAmounts: Map<String, Long>
        get() = calculatePerUserAmounts()

    val owedAmounts: List<AssignOwedAmount>
        get() = members.map { member ->
            val coveredByUserId = coveredBy[member.id]
            val coveredMemberIds = coveredBy.filterValues { it == member.id }.keys.toList()
            val baseAmountCents = perUserAmounts[member.id] ?: 0L
            val effectiveAmountCents = if (coveredByUserId != null) {
                baseAmountCents
            } else {
                baseAmountCents + coveredMemberIds.sumOf { perUserAmounts[it] ?: 0L }
            }
            AssignOwedAmount(
                memberId = member.id,
                baseAmountCents = baseAmountCents,
                effectiveAmountCents = effectiveAmountCents,
                coveredByUserId = coveredByUserId,
                coveredMemberIds = coveredMemberIds,
            )
        }

    private fun calculateTaxCents(): Long {
        val receipt = receiptForScannedMode()
        return if (receipt != null) {
            receipt.taxCents
        } else {
            resolveAmountCents(taxEnabled, taxText, taxIsPercent, subtotalCents)
        }
    }

    private fun calculateTipCents(): Long {
        val receipt = receiptForScannedMode()
        return if (receipt != null) {
            receipt.tipCents
        } else {
            resolveAmountCents(tipEnabled, tipText, tipIsPercent, subtotalCents)
        }
    }

    private fun calculateDiscountCents(): Long {
        val receipt = receiptForScannedMode()
        return if (receipt != null) {
            receipt.discountCents
        } else {
            resolveAmountCents(discountEnabled, discountText, false, subtotalCents)
        }
    }

    private fun receiptForScannedMode(): ParsedReceipt? =
        scannedReceipt?.takeIf { !isManualMode }

    private fun calculatePerUserAmounts(): Map<String, Long> {
        val perUser = mutableMapOf<String, Long>()
        for (item in items) {
            val assignees = assignments[item.id].orEmpty()
            if (assignees.isEmpty()) continue
            for (split in splitEqually(item.priceCents, assignees.toList())) {
                perUser[split.userId] = (perUser[split.userId] ?: 0L) + split.amountCents
            }
        }

        val participantIds = perUser.keys.toList()
        if (participantIds.isNotEmpty()) {
            if (taxCents > 0) {
                for (split in splitEqually(taxCents, participantIds)) {
                    perUser[split.userId] = (perUser[split.userId] ?: 0L) + split.amountCents
                }
            }
            if (tipCents > 0) {
                for (split in splitEqually(tipCents, participantIds)) {
                    perUser[split.userId] = (perUser[split.userId] ?: 0L) + split.amountCents
                }
            }
            if (discountCents > 0) {
                for (split in splitEqually(discountCents, participantIds)) {
                    perUser[split.userId] = (perUser[split.userId] ?: 0L) - split.amountCents
                }
            }
        }
        return perUser
    }

    private fun formatSignedAmount(amountCents: Long): String =
        when {
            amountCents > 0L -> formatAmount(amountCents, currency)
            amountCents < 0L -> "-${formatAmount(-amountCents, currency)}"
            else -> formatAmount(0L, currency)
        }
}

private val MemberColors = listOf(
    Color(0xFF10B981),
    Color(0xFF3B82F6),
    Color(0xFFF59E0B),
    Color(0xFFF43F5E),
    Color(0xFF8B5CF6),
    Color(0xFF14B8A6),
)

@HiltViewModel(assistedFactory = AssignItemsViewModel.Factory::class)
class AssignItemsViewModel @AssistedInject constructor(
    @Assisted("groupId") private val groupId: String,
    @Assisted("amountDisplay") private val amountDisplay: String,
    @Assisted("description") private val description: String,
    @Assisted("paidByUserId") private val paidByUserId: String,
    @Assisted("currency") private val currency: String,
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val expensesRepository: ExpensesRepository,
    private val balanceRepository: BalanceRepository,
    private val groupRepository: GroupRepository,
    private val activityRepository: ActivityRepository,
    private val scannedReceiptStore: ScannedReceiptStore,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupId: String,
            @Assisted("amountDisplay") amountDisplay: String,
            @Assisted("description") description: String,
            @Assisted("paidByUserId") paidByUserId: String,
            @Assisted("currency") currency: String
        ): AssignItemsViewModel
    }

    private val _uiState = MutableStateFlow(
        AssignItemsUiState(
            description = description.ifBlank { "Receipt" },
            amountDisplay = amountDisplay,
            currency = currency,
            paidByUserId = paidByUserId,
            isLoading = true
        )
    )
    val uiState: StateFlow<AssignItemsUiState> = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()
                memberRepository.refreshMembers(groupId)
                val groupMembers = memberRepository.getMembers(groupId).first()
                val allMembers = mutableListOf(AssignMember(currentUserId, "You", MemberColors[0]))
                groupMembers.filter { it.userId != currentUserId }.forEachIndexed { i, gm ->
                    allMembers += AssignMember(gm.userId, gm.name, MemberColors[(i + 1) % MemberColors.size])
                }

                val scannedReceipt = scannedReceiptStore.peek()
                val isManual = scannedReceipt == null || scannedReceipt.items.isEmpty()
                val items = if (!isManual) {
                    scannedReceipt.items.map { ReceiptItem(it.id, it.name, it.priceCents) }
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        members = allMembers,
                        items = items,
                        isManualMode = isManual,
                        scannedReceipt = scannedReceipt,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onItemTap(itemId: String) {
        _uiState.update {
            it.copy(expandedItemId = if (it.expandedItemId == itemId) null else itemId)
        }
    }

    fun onToggleMemberForItem(itemId: String, memberId: String) {
        _uiState.update { state ->
            val current = state.assignments[itemId].orEmpty()
            val updated = if (memberId in current) current - memberId else current + memberId
            state.copy(assignments = state.assignments + (itemId to updated))
        }
    }

    fun onToggleCoveringForMember(memberId: String) {
        _uiState.update {
            it.copy(
                expandedCoveringMemberId =
                    if (it.expandedCoveringMemberId == memberId) null else memberId
            )
        }
    }

    fun onSetCovering(coveredUserId: String, covererUserId: String?) {
        _uiState.update { state ->
            val next = if (covererUserId != null) {
                state.coveredBy + (coveredUserId to covererUserId)
            } else {
                state.coveredBy - coveredUserId
            }
            state.copy(coveredBy = next, expandedCoveringMemberId = null)
        }
    }

    fun onAddItem() {
        val newItem = ReceiptItem(id = UUID.randomUUID().toString(), name = "", priceCents = 0)
        _uiState.update { it.copy(items = it.items + newItem, editingItemId = newItem.id, expandedItemId = newItem.id) }
    }

    fun onRemoveItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.filter { it.id != itemId },
                assignments = state.assignments - itemId,
                editingItemId = if (state.editingItemId == itemId) null else state.editingItemId,
                expandedItemId = if (state.expandedItemId == itemId) null else state.expandedItemId
            )
        }
    }

    fun onEditItem(itemId: String) {
        _uiState.update {
            it.copy(editingItemId = if (it.editingItemId == itemId) null else itemId)
        }
    }

    fun onItemNameChange(itemId: String, name: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == itemId) it.copy(name = name) else it })
        }
    }

    fun onItemPriceChange(itemId: String, priceText: String) {
        val filtered = priceText.filter { c -> c.isDigit() || c == '.' }
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return
        val cents = (filtered.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
        _uiState.update { state ->
            state.copy(items = state.items.map {
                if (it.id == itemId) it.copy(priceCents = cents, priceText = filtered) else it
            })
        }
    }

    fun onToggleTax(enabled: Boolean) {
        _uiState.update { it.copy(taxEnabled = enabled, taxText = if (!enabled) "" else it.taxText) }
    }

    fun onTaxModeChange(isPercent: Boolean) {
        _uiState.update { it.copy(taxIsPercent = isPercent, taxText = "") }
    }

    fun onTaxTextChange(text: String) {
        val filtered = text.filter { c -> c.isDigit() || c == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return
        _uiState.update { it.copy(taxText = filtered) }
    }

    fun onToggleTip(enabled: Boolean) {
        _uiState.update { it.copy(tipEnabled = enabled, tipText = if (!enabled) "" else it.tipText) }
    }

    fun onTipModeChange(isPercent: Boolean) {
        _uiState.update { it.copy(tipIsPercent = isPercent, tipText = "") }
    }

    fun onTipTextChange(text: String) {
        val filtered = text.filter { c -> c.isDigit() || c == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return
        _uiState.update { it.copy(tipText = filtered) }
    }

    fun onToggleDiscount(enabled: Boolean) {
        _uiState.update { it.copy(discountEnabled = enabled, discountText = if (!enabled) "" else it.discountText) }
    }

    fun onDiscountTextChange(text: String) {
        val filtered = text.filter { c -> c.isDigit() || c == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return
        _uiState.update { it.copy(discountText = filtered) }
    }

    fun assignedNamesForItem(itemId: String): String {
        val state = _uiState.value
        val ids = state.assignments[itemId].orEmpty()
        if (ids.isEmpty()) return "Not assigned"
        return state.members.filter { it.id in ids }.joinToString(", ") { it.name }
    }

    fun onNext() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val perUser = state.perUserAmounts

        val taxCents = state.taxCents
        val tipCents = state.tipCents
        val discountCents = state.discountCents
        Log.d("AssignItems", "tax=$taxCents, tip=$tipCents, discount=$discountCents")
        Log.d("AssignItems", "participants=${perUser.keys.toList()}, itemTotal=${perUser.values.sum()}")

        val splits = state.members.map { m ->
            ExpenseSplit(m.id, perUser[m.id] ?: 0L, isCoveredBy = state.coveredBy[m.id])
        }
        val amountCents = splits.sumOf { it.amountCents }
        Log.d("AssignItems", "finalTotal=$amountCents, splits=$splits")

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val groupExpense = expensesRepository.createExpenseWithSplits(
                    groupId = groupId,
                    description = state.description,
                    amountCents = amountCents,
                    currency = currency,
                    splitMethod = "BY_ITEM",
                    paidByUserId = state.paidByUserId,
                    splits = splits
                )
                val receiptRows = state.items.map { item ->
                    val assignees = state.assignments[item.id].orEmpty()
                    ReceiptItemRow(
                        expenseId = groupExpense.id,
                        description = item.name,
                        priceCents = item.priceCents,
                        assignedUserId = assignees.singleOrNull()
                    )
                }.toMutableList()
                if (tipCents > 0) {
                    receiptRows += ReceiptItemRow(groupExpense.id, "Tip", tipCents)
                }
                if (discountCents > 0) {
                    receiptRows += ReceiptItemRow(groupExpense.id, "Discount", discountCents)
                }
                try {
                    expensesRepository.saveReceiptItems(receiptRows)
                } catch (e: Exception) {
                    Sentry.captureException(e)
                }
                balanceRepository.clearCache(groupId)
                balanceRepository.refreshBalances(groupId)
                expensesRepository.refreshGroupExpenses(groupId)
                groupRepository.refreshGroups()
                activityRepository.refreshActivityFeed()
                runCatching { friendsRepository.getFriendsBalances() }
                _uiState.update { it.copy(isSaving = false) }
                _done.send(Unit)
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}

private fun resolveAmountCents(enabled: Boolean, text: String, isPercent: Boolean, subtotalCents: Long): Long {
    if (!enabled) return 0L
    val value = text.toDoubleOrNull() ?: return 0L
    return if (isPercent) (subtotalCents * value / 100.0).toLong()
    else (value * 100).toLong()
}
