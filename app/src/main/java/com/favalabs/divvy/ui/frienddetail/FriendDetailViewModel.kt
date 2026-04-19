package com.favalabs.divvy.ui.frienddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.AuthRepository
import com.favalabs.divvy.backend.DataResult
import com.favalabs.divvy.backend.ExpensesRepository
import com.favalabs.divvy.backend.ForexRepository
import com.favalabs.divvy.backend.FriendsRepository
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.MemberRepository
import com.favalabs.divvy.components.GroupIcon
import io.sentry.Sentry
import com.favalabs.divvy.models.FriendActivityItem
import com.favalabs.divvy.models.FriendGroupBalances
import com.favalabs.divvy.models.Group
import com.favalabs.divvy.models.GroupBalance
import com.favalabs.divvy.models.GroupExpense
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class FriendDetailUiState(
    val friendName: String = "",
    val balances: List<GroupBalance> = emptyList(),
    val groupedBalances: List<FriendGroupBalances> = emptyList(),
    val cadGroupedBalances: List<FriendGroupBalances> = emptyList(),
    val overallBalanceCad: Long? = null,
    val convertToCad: Boolean = false,
    val activity: List<FriendActivityItem> = emptyList(),
    val isLoading: Boolean = true,
    val navigateToSplitWithGroupId: String? = null
) {
    val displayedGroupedBalances: List<FriendGroupBalances>
        get() = if (convertToCad) cadGroupedBalances else groupedBalances
}

@HiltViewModel(assistedFactory = FriendDetailViewModel.Factory::class)
class FriendDetailViewModel @AssistedInject constructor(
    @Assisted private val friendUserId: String,
    private val authRepository: AuthRepository,
    private val friendsRepository: FriendsRepository,
    private val expensesRepository: ExpensesRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val forexRepository: ForexRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(friendUserId: String): FriendDetailViewModel
    }

    private val _uiState = MutableStateFlow(FriendDetailUiState())
    val uiState: StateFlow<FriendDetailUiState> = _uiState.asStateFlow()

    private val myUserId: String = authRepository.getCurrentUserId()
    private var friendFirstName: String = ""
    private var sharedGroupIds: List<String> = emptyList()

    private val fallbackRates = mapOf(
        "AUD" to 1.0419, "BRL" to 3.7968, "CHF" to 0.57289, "CNY" to 5.0059, "CZK" to 15.2951,
        "DKK" to 4.6765, "EUR" to 0.6259, "GBP" to 0.54177, "HKD" to 5.673, "HUF" to 243.59,
        "IDR" to 12231.0, "ILS" to 2.2649, "INR" to 68.16, "ISK" to 89.75, "JPY" to 115.33,
        "KRW" to 1086.52, "MXN" to 12.8898, "MYR" to 2.8768, "NOK" to 7.0636, "NZD" to 1.2469,
        "PHP" to 43.579, "PLN" to 2.6737, "RON" to 3.1888, "SEK" to 6.7419, "SGD" to 0.92815,
        "THB" to 23.645, "TRY" to 32.183, "USD" to 0.72554, "ZAR" to 12.2715, "CAD" to 1.0
    )

    init {
        viewModelScope.launch {
            val friendBalances = friendsRepository.getFriendsBalances()
            val friend = friendBalances.find { it.userId == friendUserId }

            val friendName = friend?.displayName ?: "Friend"
            friendFirstName = friend?.firstName.orEmpty().ifBlank { "Friend" }
            val balances = friend?.groupBalances ?: emptyList()
            sharedGroupIds = balances.map { it.groupId }.distinct()

            val groupInfoFromBalances = balances.associateBy { it.groupId }

            val nonZeroBalances = balances.filter { it.balanceCents != 0L }

            // Group balances by groupId
            val grouped = nonZeroBalances.groupBy { it.groupId }.map { (groupId, groupBalances) ->
                val first = groupBalances.first()
                FriendGroupBalances(
                    groupId = groupId,
                    groupName = first.groupName,
                    groupIcon = first.groupIcon,
                    balances = groupBalances
                )
            }

            // CAD-converted grouped balances
            val cadGrouped = grouped.map { group ->
                var totalCad = 0L
                for (b in group.balances) {
                    totalCad += convertToCad(b.balanceCents, b.currency)
                }
                FriendGroupBalances(
                    groupId = group.groupId,
                    groupName = group.groupName,
                    groupIcon = group.groupIcon,
                    balances = listOf(
                        GroupBalance(
                            groupId = group.groupId,
                            groupName = group.groupName,
                            groupIcon = group.groupIcon,
                            balanceCents = totalCad,
                            currency = Group.BASE_CURRENCY
                        )
                    )
                )
            }

            // Overall CAD balance
            var overallCad = 0L
            for (b in nonZeroBalances) {
                overallCad += convertToCad(b.balanceCents, b.currency)
            }

            _uiState.update {
                it.copy(
                    friendName = friendName,
                    balances = nonZeroBalances,
                    groupedBalances = grouped,
                    cadGroupedBalances = cadGrouped,
                    overallBalanceCad = overallCad
                )
            }

            // Show cached expenses immediately, then refresh in the background
            expensesRepository.observeAllGroupExpenses().collect { allExpenses ->
                val activity = buildFriendActivity(allExpenses, groupInfoFromBalances)
                _uiState.update {
                    it.copy(activity = activity, isLoading = false)
                }
            }
        }
    }

    fun onToggleConvertToCad() {
        _uiState.update { it.copy(convertToCad = !it.convertToCad) }
    }

    fun onAddExpense() {
        viewModelScope.launch {
            val allGroupIds = sharedGroupIds.toMutableSet()
            try {
                groupRepository.refreshGroups()
                val groups = groupRepository.listGroups().first { it is DataResult.Success }
                if (groups is DataResult.Success) {
                    allGroupIds.addAll(groups.data.map { it.id })
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
            }

            var existing1on1GroupId: String? = null
            for (groupId in allGroupIds) {
                try {
                    memberRepository.refreshMembers(groupId)
                    val members = memberRepository.getMembers(groupId).first()
                    if (members.size == 2 && members.any { it.userId == friendUserId }) {
                        existing1on1GroupId = groupId
                        break
                    }
                } catch (_: Exception) { } // non-fatal: skip this group and continue
            }

            if (existing1on1GroupId != null) {
                _uiState.update { it.copy(navigateToSplitWithGroupId = existing1on1GroupId) }
            } else {
                try {
                    val groupName = "$friendFirstName and You"
                    val group = groupRepository.createGroup(groupName, GroupIcon.Group)
                    memberRepository.addMember(group.id, friendUserId)
                    groupRepository.refreshGroups()
                    _uiState.update { it.copy(navigateToSplitWithGroupId = group.id) }
                } catch (e: Exception) {
                    Sentry.captureException(e)
                }
            }
        }
    }

    fun onNavigateToSplitHandled() {
        _uiState.update { it.copy(navigateToSplitWithGroupId = null) }
    }

    private suspend fun convertToCad(amountCents: Long, currency: String): Long {
        if (currency == Group.BASE_CURRENCY) return amountCents
        val rate = forexRepository.getRate(currency, Group.BASE_CURRENCY)
            ?: getFallbackRate(currency, Group.BASE_CURRENCY)
        return (amountCents * rate).toLong()
    }

    private fun getFallbackRate(from: String, to: String): Double {
        if (from == to) return 1.0
        val fromRate = fallbackRates[from] ?: 1.0
        val toRate = fallbackRates[to] ?: 1.0
        return toRate / fromRate
    }

    private suspend fun buildFriendActivity(
        allExpenses: List<GroupExpense>,
        groupInfoFromBalances: Map<String, GroupBalance>
    ): List<FriendActivityItem> {
        val groupInfoMap = groupInfoFromBalances.toMutableMap()
        val missingGroupIds = mutableSetOf<String>()

        val relevantExpenses = allExpenses.filter { expense ->
            val hasMe = expense.splits.any { it.userId == myUserId } || expense.paidByUserId == myUserId
            val hasFriend = expense.splits.any { it.userId == friendUserId } || expense.paidByUserId == friendUserId
            hasMe && hasFriend
        }

        relevantExpenses.forEach { expense ->
            if (expense.groupId !in groupInfoMap) {
                missingGroupIds.add(expense.groupId)
            }
        }

        if (missingGroupIds.isNotEmpty()) {
            try {
                groupRepository.refreshGroups()
                val groups = groupRepository.listGroups().first { it is DataResult.Success }
                if (groups is DataResult.Success) {
                    groups.data.forEach { group ->
                        if (group.id in missingGroupIds) {
                            groupInfoMap[group.id] = GroupBalance(
                                groupId = group.id,
                                groupName = group.name,
                                groupIcon = group.icon.name,
                                balanceCents = 0L,
                                currency = "USD"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
            }
        }

        return relevantExpenses
            .mapNotNull { expense -> buildActivityItem(expense, groupInfoMap) }
            .sortedByDescending { it.timestamp }
    }

    private fun buildActivityItem(
        expense: GroupExpense,
        groupInfoMap: Map<String, GroupBalance>
    ): FriendActivityItem? {
        val paidByMe = expense.paidByUserId == myUserId
        val paidByFriend = expense.paidByUserId == friendUserId

        if (!paidByMe && !paidByFriend) return null

        val displayAmountCents = if (paidByMe) {
            expense.splits.find { it.userId == friendUserId }
                ?.let { if (it.isCoveredBy != null) 0L else it.amountCents }
                ?: 0L
        } else {
            expense.splits.find { it.userId == myUserId }
                ?.let { if (it.isCoveredBy != null) 0L else it.amountCents }
                ?: 0L
        }

        if (displayAmountCents == 0L) return null

        val groupInfo = groupInfoMap[expense.groupId]

        return FriendActivityItem(
            id = expense.id,
            title = expense.title,
            amountCents = displayAmountCents,
            dateLabel = dateLabel(expense.createdAt),
            paidByCurrentUser = paidByMe,
            timestamp = expense.createdAt,
            currency = expense.currency,
            groupId = expense.groupId,
            groupName = groupInfo?.groupName ?: "Unknown Group",
            groupIcon = groupInfo?.groupIcon
        )
    }

    private fun dateLabel(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate.take(10))
            val today = LocalDate.now()
            when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            }
        } catch (_: Exception) { isoDate }
    }
}
