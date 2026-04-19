package com.favalabs.divvy.ui.splitpercentage.ViewModels

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
import com.favalabs.divvy.models.formatAmount
import com.favalabs.divvy.models.splitByPercentage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PercentageMember(
    val id: String,
    val name: String,
    val color: Color
)

data class SplitByPercentageUiState(
    val description: String = "",
    val amountDisplay: String = "",
    val currency: String = "USD",
    val members: List<PercentageMember> = emptyList(),
    val percentages: Map<String, String> = emptyMap(),
    val paidByUserId: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val coveredBy: Map<String, String> = emptyMap(),
    val expandedCoveringMemberId: String? = null,
) {
    val totalPercentage: Double
        get() = percentages.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    val isValid: Boolean
        get() = kotlin.math.abs(totalPercentage - 100.0) < 0.01

    fun dollarAmountFor(memberId: String): String {
        val total = amountDisplay.toDoubleOrNull() ?: 0.0
        val pct = percentages[memberId]?.toDoubleOrNull() ?: 0.0
        val amountCents = (total * pct / 100.0 * 100).toLong()
        return formatAmount(amountCents, currency)
    }

    fun effectiveDollarAmountFor(memberId: String): String {
        val total = amountDisplay.toDoubleOrNull() ?: 0.0
        val ownPct = percentages[memberId]?.toDoubleOrNull() ?: 0.0
        val ownAmount = total * ownPct / 100.0
        val coveredAmount = coveredBy.entries
            .filter { it.value == memberId }
            .sumOf { (coveredId, _) ->
                val pct = percentages[coveredId]?.toDoubleOrNull() ?: 0.0
                total * pct / 100.0
            }
        return "$${String.format("%.2f", ownAmount + coveredAmount)}"
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

@HiltViewModel(assistedFactory = SplitByPercentageViewModel.Factory::class)
class SplitByPercentageViewModel @AssistedInject constructor(
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
        ): SplitByPercentageViewModel
    }

    private val _uiState = MutableStateFlow(
        SplitByPercentageUiState(
            description = description.ifBlank { "Expense" },
            amountDisplay = amountDisplay,
            currency = currency,
            paidByUserId = paidByUserId,
            isLoading = true
        )
    )
    val uiState: StateFlow<SplitByPercentageUiState> = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    private val myUserId = authRepository.getCurrentUserId()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                memberRepository.refreshMembers(groupId)
                val groupMembers = memberRepository.getMembers(groupId).first()
                val allMembers = mutableListOf(PercentageMember(myUserId, "You", MemberColors[0]))
                groupMembers.filter { it.userId != myUserId }.forEachIndexed { i, gm ->
                    allMembers += PercentageMember(gm.userId, gm.name, MemberColors[(i + 1) % MemberColors.size])
                }
                val equalPct = String.format("%.1f", 100.0 / allMembers.size)
                _uiState.update {
                    it.copy(
                        members = allMembers,
                        percentages = allMembers.associate { m -> m.id to equalPct },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onPercentageChange(memberId: String, value: String) {
        val filtered = value.filter { c -> c.isDigit() || c == '.' }
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 1) return
        _uiState.update { state ->
            state.copy(percentages = state.percentages + (memberId to filtered))
        }
    }

    fun onSplitEvenly() {
        _uiState.update { state ->
            val equalPct = String.format("%.1f", 100.0 / state.members.size)
            state.copy(percentages = state.members.associate { it.id to equalPct })
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

    fun onDone() {
        val state = _uiState.value
        if (!state.isValid) return
        val amountCents = ((state.amountDisplay.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val percentages = state.members.associate { m ->
            m.id to (state.percentages[m.id]?.toDoubleOrNull() ?: 0.0)
        }
        val splits = splitByPercentage(amountCents, percentages).map { split ->
            split.copy(isCoveredBy = state.coveredBy[split.userId])
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                expensesRepository.createExpenseWithSplits(
                    groupId = groupId,
                    description = state.description,
                    amountCents = amountCents,
                    currency = currency,
                    splitMethod = "PERCENTAGE",
                    paidByUserId = state.paidByUserId,
                    splits = splits
                )
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
