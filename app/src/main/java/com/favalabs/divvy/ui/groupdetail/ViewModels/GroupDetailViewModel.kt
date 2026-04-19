package com.favalabs.divvy.ui.groupdetail.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.AuthRepository
import com.favalabs.divvy.backend.BalanceRepository
import com.favalabs.divvy.backend.ExpensesRepository
import com.favalabs.divvy.backend.ForexRepository
import com.favalabs.divvy.backend.FriendsRepository
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.MemberRepository
import com.favalabs.divvy.components.GroupIcon
import com.favalabs.divvy.models.ActivityItem
import com.favalabs.divvy.models.Group
import com.favalabs.divvy.models.GroupExpense
import com.favalabs.divvy.models.GroupMember
import com.favalabs.divvy.models.MemberBalance
import com.favalabs.divvy.models.SimplifiedPayment
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
import java.time.format.TextStyle
import java.util.Locale

enum class SettleMode { Fully, Partially }

data class UserBalanceGroup(
    val userId: String,
    val name: String,
    val balances: List<MemberBalance>
)

data class GroupDetailUiState(
    val group: Group = Group(id = "", name = ""),
    val memberBalances: List<MemberBalance> = emptyList(),
    val cadMemberBalances: List<MemberBalance> = emptyList(),
    val groupNetBalances: List<MemberBalance> = emptyList(),
    val cadGroupNetBalances: List<MemberBalance> = emptyList(),
    val simplifiedPayments: List<SimplifiedPayment> = emptyList(),
    val cadSimplifiedPayments: List<SimplifiedPayment> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = true,
    val isCreator: Boolean = false,
    val currentMemberIds: Set<String> = emptySet(),
    /** Net balance converted to CAD for the summary card. Null while rates are loading. */
    val netBalanceCad: Long? = null,
    val myUserId: String = "",
    /** Toggle: only show transactions involving the current user (default on) */
    val onlyMine: Boolean = true,
    /** Toggle: convert all amounts to CAD (default off) */
    val convertToCad: Boolean = false,
    // Delegate states
    val settlement: SettlementState = SettlementState(),
    val editGroup: EditGroupState = EditGroupState()
) {
    // Convenience accessors so the UI doesn't need to know about delegates
    val expandedMemberId get() = settlement.expandedMemberId
    val expandedCurrency get() = settlement.expandedCurrency
    val settleMode get() = settlement.settleMode
    val settleAmount get() = settlement.settleAmount
    val isSettling get() = settlement.isSettling
    val isEditing get() = editGroup.isEditing
    val editName get() = editGroup.editName
    val editIcon get() = editGroup.editIcon
    val isSavingEdit get() = editGroup.isSaving

    /** The grouped balances list to display after applying toggles */
    val displayedBalances: List<UserBalanceGroup> get() {
        val base = if (onlyMine) {
            if (convertToCad) cadMemberBalances else memberBalances
        } else {
            if (convertToCad) cadGroupNetBalances else groupNetBalances
        }

        return base.filter { it.balanceCents != 0L }
            .groupBy { it.userId }
            .map { (id, bals) ->
                UserBalanceGroup(
                    userId = id,
                    name = bals.first().name,
                    balances = bals.sortedByDescending { kotlin.math.abs(it.balanceCents) }
                )
            }
            .sortedWith(
                compareBy<UserBalanceGroup> { it.userId != myUserId } // Hoist 'You' to the top
                    .thenBy { it.name }
            )
    }

    /** The payments list to display after applying toggles */
    val displayedPayments: List<SimplifiedPayment> get() {
        val base = if (convertToCad) cadSimplifiedPayments else simplifiedPayments
        return if (onlyMine) base.filter { it.fromIsCurrentUser || it.toIsCurrentUser } else base
    }
}

@HiltViewModel(assistedFactory = GroupDetailViewModel.Factory::class)
class GroupDetailViewModel @AssistedInject constructor(
    @Assisted private val groupId: String,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val balanceRepository: BalanceRepository,
    private val expensesRepository: ExpensesRepository,
    private val forexRepository: ForexRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(groupId: String): GroupDetailViewModel
    }

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val myUserId: String = authRepository.getCurrentUserId()

    // Base fallback rates keyed to CAD = 1.0 (from 2026-03-25 mock data)
    private val fallbackRates = mapOf(
        "AUD" to 1.0419, "BRL" to 3.7968, "CHF" to 0.57289, "CNY" to 5.0059, "CZK" to 15.2951,
        "DKK" to 4.6765, "EUR" to 0.6259, "GBP" to 0.54177, "HKD" to 5.673, "HUF" to 243.59,
        "IDR" to 12231.0, "ILS" to 2.2649, "INR" to 68.16, "ISK" to 89.75, "JPY" to 115.33,
        "KRW" to 1086.52, "MXN" to 12.8898, "MYR" to 2.8768, "NOK" to 7.0636, "NZD" to 1.2469,
        "PHP" to 43.579, "PLN" to 2.6737, "RON" to 3.1888, "SEK" to 6.7419, "SGD" to 0.92815,
        "THB" to 23.645, "TRY" to 32.183, "USD" to 0.72554, "ZAR" to 12.2715, "CAD" to 1.0
    )

    init {
        _uiState.update { it.copy(myUserId = myUserId) }
    }

    val settlementDelegate = SettlementDelegate(
        groupId = groupId,
        myUserId = myUserId,
        scope = viewModelScope,
        expensesRepository = expensesRepository,
        balanceRepository = balanceRepository,
        groupRepository = groupRepository,
        friendsRepository = friendsRepository,
        getMemberBalances = { _uiState.value.memberBalances }
    )

    val editDelegate = EditGroupDelegate(
        groupId = groupId,
        scope = viewModelScope,
        groupRepository = groupRepository
    )

    init {
        viewModelScope.launch {
            memberRepository.refreshMembers(groupId)
            balanceRepository.refreshBalances(groupId)
            expensesRepository.refreshGroupExpenses(groupId)
        }

        viewModelScope.launch {
            combine(
                groupRepository.getGroup(groupId),
                memberRepository.getMembers(groupId),
                balanceRepository.observeBalances(groupId),
                expensesRepository.observeGroupExpenses(groupId)
            ) { group, members, rawBalances, expenses ->
                val simplifiedPaymentsList = simplifyPayments(rawBalances, members, myUserId)

                val memberBalances = members
                    .filter { it.userId != myUserId }
                    .flatMap { member ->
                        val memberRawBalances = rawBalances.filter { it.userId == member.userId }
                        if (memberRawBalances.isEmpty()) {
                            listOf(MemberBalance(userId = member.userId, name = member.name, balanceCents = 0L))
                        } else {
                            memberRawBalances.map { raw ->
                                MemberBalance(
                                    userId = member.userId,
                                    name = member.name,
                                    balanceCents = raw.balanceCents,
                                    currency = raw.currency
                                )
                            }
                        }
                    }

                // Calculate Group Net Balances (Toggle Off State)
                val groupNetBals = mutableListOf<MemberBalance>()
                val currencies = memberBalances.map { it.currency }.distinct()
                for (currency in currencies) {
                    val rows = memberBalances.filter { it.currency == currency }
                    var myNet = 0L
                    for (mb in rows) {
                        myNet += mb.balanceCents
                        // If they owe you $10, their net is -$10 (they owe overall)
                        groupNetBals.add(MemberBalance(mb.userId, mb.name, -mb.balanceCents, currency))
                    }
                    if (myNet != 0L) {
                        groupNetBals.add(MemberBalance(myUserId, "You", myNet, currency))
                    }
                }

                // --- 2. CAD Converted Calculations ---

                var netCad = 0L
                for (raw in rawBalances) {
                    if (raw.currency == Group.BASE_CURRENCY) {
                        netCad += raw.balanceCents
                    } else {
                        // Rate falls back to the hardcoded map if the network fails
                        val rate = forexRepository.getRate(raw.currency, Group.BASE_CURRENCY)
                            ?: getFallbackRate(raw.currency, Group.BASE_CURRENCY)
                        netCad += (raw.balanceCents * rate).toLong()
                    }
                }

                val cadMemberBals = mutableListOf<MemberBalance>()
                val activeMembers = members.filter { it.userId != myUserId }
                for (member in activeMembers) {
                    val memberRaws = rawBalances.filter { it.userId == member.userId }
                    if (memberRaws.isEmpty()) continue

                    var totalCad = 0L
                    for (b in memberRaws) {
                        if (b.currency == Group.BASE_CURRENCY) {
                            totalCad += b.balanceCents
                        } else {
                            val rate = forexRepository.getRate(b.currency, Group.BASE_CURRENCY)
                                ?: getFallbackRate(b.currency, Group.BASE_CURRENCY)
                            totalCad += (b.balanceCents * rate).toLong()
                        }
                    }

                    cadMemberBals.add(
                        MemberBalance(
                            userId = member.userId,
                            name = member.name,
                            balanceCents = totalCad,
                            currency = Group.BASE_CURRENCY
                        )
                    )
                }

                val cadGroupNetBals = mutableListOf<MemberBalance>()
                var myCadNet = 0L
                for (mb in cadMemberBals) {
                    myCadNet += mb.balanceCents
                    cadGroupNetBals.add(MemberBalance(mb.userId, mb.name, -mb.balanceCents, Group.BASE_CURRENCY))
                }
                if (myCadNet != 0L) {
                    cadGroupNetBals.add(MemberBalance(myUserId, "You", myCadNet, Group.BASE_CURRENCY))
                }

                val mergedCadPayments = simplifyPayments(cadMemberBals, members, myUserId)

                // --- 3. Activity Feed ---

                val activity = buildActivity(expenses, members)
                val memberIds = members.map { it.userId }.toSet() + myUserId

                // --- 4. Package all calculations ---
                GroupDetailData(
                    group = group,
                    memberBalances = memberBalances,
                    groupNetBalances = groupNetBals,
                    simplifiedPayments = simplifiedPaymentsList,
                    cadMemberBalances = cadMemberBals,
                    cadGroupNetBalances = cadGroupNetBals,
                    cadSimplifiedPayments = mergedCadPayments,
                    netBalanceCad = netCad,
                    activity = activity,
                    memberIds = memberIds
                )

            }.collect { data ->
                // --- 5. Commit all updates cleanly in one single UI pass ---
                _uiState.update { current ->
                    current.copy(
                        group = data.group,
                        memberBalances = data.memberBalances,
                        groupNetBalances = data.groupNetBalances,
                        simplifiedPayments = data.simplifiedPayments,
                        cadMemberBalances = data.cadMemberBalances,
                        cadGroupNetBalances = data.cadGroupNetBalances,
                        cadSimplifiedPayments = data.cadSimplifiedPayments,
                        netBalanceCad = data.netBalanceCad,
                        activity = data.activity,
                        isLoading = false,
                        isCreator = data.group.createdBy == myUserId,
                        currentMemberIds = data.memberIds
                    )
                }
            }
        }

        // Delegate State Collectors
        viewModelScope.launch {
            settlementDelegate.state.collect { s ->
                _uiState.update { it.copy(settlement = s) }
            }
        }
        viewModelScope.launch {
            editDelegate.state.collect { s ->
                _uiState.update { it.copy(editGroup = s) }
            }
        }
    }

    fun onToggleOnlyMine() = _uiState.update { it.copy(onlyMine = !it.onlyMine) }
    fun onToggleConvertToCad() = _uiState.update { it.copy(convertToCad = !it.convertToCad) }

    // --- Settlement (forwarded to delegate) ---
    fun onMemberClick(userId: String, currency: String) {
        val payments = if (_uiState.value.convertToCad) _uiState.value.cadSimplifiedPayments else _uiState.value.simplifiedPayments
        settlementDelegate.onMemberClick(userId, currency, payments)
    }
    fun onSettleModeSelected(mode: SettleMode, currency: String = "USD") = settlementDelegate.onSettleModeSelected(mode, currency)
    fun onSettleAmountChange(value: String) = settlementDelegate.onSettleAmountChange(value)
    fun onConfirmSettle() = settlementDelegate.onConfirmSettle()

    // --- Edit group (forwarded to delegate) ---
    fun onStartEdit() = editDelegate.onStartEdit(_uiState.value.group)
    fun onCancelEdit() = editDelegate.onCancelEdit()
    fun onEditNameChange(value: String) = editDelegate.onEditNameChange(value)
    fun onEditIconSelected(icon: GroupIcon) = editDelegate.onEditIconSelected(icon)
    fun onSaveEdit() = editDelegate.onSaveEdit()

    // --- Helpers ---

    /** * Calculates the exchange rate between two currencies using the base map.
     * Since the map defines 1 CAD = X Target Currency, we can find the cross-rate
     * by routing through CAD: Rate = ToRate / FromRate.
     */
    private fun getFallbackRate(from: String, to: String): Double {
        if (from == to) return 1.0
        val fromRate = fallbackRates[from] ?: 1.0
        val toRate = fallbackRates[to] ?: 1.0
        return toRate / fromRate
    }

    private fun buildActivity(
        expenses: List<GroupExpense>,
        members: List<GroupMember>
    ): List<ActivityItem> {
        val memberMap = members.associateBy { it.userId }
        return expenses
            .sortedByDescending { it.createdAt }
            .map { expense ->
                val paidByCurrentUser = expense.paidByUserId == myUserId

                val displayAmountCents = if (paidByCurrentUser) {
                    val myEffectiveShare = expense.splits
                        .find { it.userId == myUserId }
                        ?.let { if (it.isCoveredBy != null) 0L else it.amountCents }
                        ?: 0L
                    expense.amountCents - myEffectiveShare
                } else {
                    val myDirectShare = expense.splits
                        .find { it.userId == myUserId }
                        ?.let { if (it.isCoveredBy != null) 0L else it.amountCents }
                        ?: 0L
                    val coveringAmount = expense.splits
                        .filter { it.isCoveredBy == myUserId }
                        .sumOf { it.amountCents }
                    myDirectShare + coveringAmount
                }

                ActivityItem(
                    id = expense.id,
                    title = expense.title,
                    amountCents = displayAmountCents,
                    dateLabel = dateLabel(expense.createdAt),
                    paidByLabel = if (paidByCurrentUser) "You"
                    else memberMap[expense.paidByUserId]?.name ?: "Unknown",
                    paidByCurrentUser = paidByCurrentUser,
                    timestamp = expense.createdAt,
                    currency = expense.currency,
                    isPending = expense.id.startsWith("local_")
                )
            }
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

// Updated data class to hold everything our combine block computes
private data class GroupDetailData(
    val group: Group,
    val memberBalances: List<MemberBalance>,
    val groupNetBalances: List<MemberBalance>,
    val simplifiedPayments: List<SimplifiedPayment>,
    val cadMemberBalances: List<MemberBalance>,
    val cadGroupNetBalances: List<MemberBalance>,
    val cadSimplifiedPayments: List<SimplifiedPayment>,
    val netBalanceCad: Long,
    val activity: List<ActivityItem>,
    val memberIds: Set<String>
)

private fun simplifyPayments(
    memberBalances: List<MemberBalance>,
    members: List<GroupMember>,
    myUserId: String
): List<SimplifiedPayment> {
    val nameMap = members.associate { it.userId to it.name }
    val currencies = memberBalances.map { it.currency }.distinct()
    val result = mutableListOf<SimplifiedPayment>()

    for (currency in currencies) {
        val rows = memberBalances.filter { it.currency == currency }
        val absoluteBalances = LinkedHashMap<String, Long>()
        var myBalance = 0L
        for (mb in rows) {
            absoluteBalances[mb.userId] = (absoluteBalances[mb.userId] ?: 0L) - mb.balanceCents
            myBalance += mb.balanceCents
        }
        if (myBalance != 0L) absoluteBalances[myUserId] = myBalance

        val creditors = java.util.PriorityQueue<Pair<String, Long>>(compareByDescending { it.second })
        val debtors   = java.util.PriorityQueue<Pair<String, Long>>(compareByDescending { it.second })

        for ((uid, bal) in absoluteBalances) {
            when {
                bal > 0L -> creditors.add(uid to bal)
                bal < 0L -> debtors.add(uid to -bal)
            }
        }

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val (creditor, credit) = creditors.poll()!!
            val (debtor,  debt)   = debtors.poll()!!

            val amount = minOf(credit, debt)
            result.add(
                SimplifiedPayment(
                    fromUserId        = debtor,
                    fromName          = nameMap[debtor] ?: debtor,
                    toUserId          = creditor,
                    toName            = nameMap[creditor] ?: creditor,
                    amountCents       = amount,
                    currency          = currency,
                    fromIsCurrentUser = debtor == myUserId,
                    toIsCurrentUser   = creditor == myUserId
                )
            )

            val remaining = credit - debt
            when {
                remaining > 0L -> creditors.add(creditor to remaining)
                remaining < 0L -> debtors.add(debtor to -remaining)
            }
        }
    }

    return result.sortedWith(compareBy<SimplifiedPayment> { it.currency }.thenByDescending { it.amountCents })
}