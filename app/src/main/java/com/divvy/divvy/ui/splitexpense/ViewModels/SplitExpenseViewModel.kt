package com.divvy.divvy.ui.splitexpense.ViewModels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divvy.divvy.backend.ActivityRepository
import com.divvy.divvy.backend.AuthRepository
import com.divvy.divvy.backend.BalanceRepository
import com.divvy.divvy.backend.DataResult
import com.divvy.divvy.backend.ExpensesRepository
import com.divvy.divvy.backend.FriendsRepository
import com.divvy.divvy.backend.GroupRepository
import com.divvy.divvy.backend.MemberRepository
import com.divvy.divvy.backend.ProfilesRepository
import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.Group
import com.divvy.divvy.models.GroupMember
import com.divvy.divvy.models.ProfileRow
import com.divvy.divvy.models.SupportedCurrency
import com.divvy.divvy.models.splitEqually
import android.util.Log
import com.divvy.divvy.ui.groups.ViewModels.CreateGroupStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.sentry.Sentry

enum class SplitMethod(val title: String, val subtitle: String) {
    Equally("Split equally", "Everyone pays the same"),
    ByPercentage("By percentage", "Custom % for each person"),
    ByItems("By items", "Assign items to people")
}

data class SplitMember(
    val id: String,
    val name: String,
    val isCurrentUser: Boolean = false,
)

data class SplitExpenseUiState(
    val amount: String = "",
    val description: String = "",
    val currency: String = "USD",
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val splitMethod: SplitMethod = SplitMethod.Equally,
    val members: List<SplitMember> = emptyList(),
    val paidByUserId: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isMembersLoading: Boolean = false,
    val showCreateGroup: Boolean = false,
    val createStep: CreateGroupStep = CreateGroupStep.Basics,
    val createName: String = "",
    val createIcon: GroupIcon = GroupIcon.Group,
    val allProfiles: List<ProfileRow> = emptyList(),
    val profileSearchQuery: String = "",
    val selectedMemberIds: Set<String> = emptySet(),
    val isLoadingProfiles: Boolean = false,
    val isCreatingGroup: Boolean = false,
    val createErrorMessage: String? = null,
    val errorMessage: String? = null,
    val coveredBy: Map<String, String> = emptyMap(),
    val expandedCoveringMemberId: String? = null,
) {
    val paidByName: String
        get() = members.firstOrNull { it.id == paidByUserId }?.name ?: "You"

    val currencySymbol: String
        get() = SupportedCurrency.fromCode(currency).symbol

    val perPersonAmounts: Map<String, Long>
        get() {
            val total = amount.toDoubleOrNull() ?: return emptyMap()
            if (members.isEmpty()) return emptyMap()
            val amountCents = (total * 100).toLong()
            return splitEqually(amountCents, members.map { it.id })
                .associate { it.userId to it.amountCents }
        }

    val canCreate: Boolean
        get() = amount.toDoubleOrNull() != null &&
            amount.toDouble() > 0 &&
            selectedGroupId != null &&
            members.isNotEmpty() &&
            !isMembersLoading
}

@HiltViewModel
class SplitExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expensesRepository: ExpensesRepository,
    private val balanceRepository: BalanceRepository,
    private val activityRepository: ActivityRepository,
    private val profilesRepository: ProfilesRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val scannedAmount: String = savedStateHandle["scannedAmount"] ?: ""
    private val scannedDescription: String = savedStateHandle["scannedDescription"] ?: ""
    private val preselectedGroupId: String = savedStateHandle["preselectedGroupId"] ?: ""

    private val currentUserId = authRepository.getCurrentUserId()

    private val _uiState = MutableStateFlow(
        SplitExpenseUiState(
            isLoading = true,
            amount = scannedAmount,
            description = scannedDescription,
            paidByUserId = currentUserId,
            splitMethod = if (scannedAmount.isNotEmpty()) SplitMethod.ByItems else SplitMethod.Equally,
        )
    )
    val uiState: StateFlow<SplitExpenseUiState> = _uiState.asStateFlow()

    sealed interface SplitEvent {
        data object Created : SplitEvent
        data class GoToAssignItems(val groupId: String, val amount: String, val description: String, val paidByUserId: String, val currency: String) : SplitEvent
        data class GoToSplitByPercentage(val groupId: String, val amount: String, val description: String, val paidByUserId: String, val currency: String) : SplitEvent
    }

    private val _events = Channel<SplitEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            groupRepository.listGroups().collect { result ->
                val groups = (result as? DataResult.Success)?.data ?: return@collect
                val selectedId = _uiState.value.selectedGroupId
                    ?: preselectedGroupId.takeIf { it.isNotEmpty() }
                    ?: groups.firstOrNull()?.id
                _uiState.update { current ->
                    current.copy(
                        groups = groups,
                        selectedGroupId = selectedId,
                        isLoading = false
                    )
                }
                if (selectedId != null) {
                    loadMembersForGroup(selectedId)
                }
            }
        }
    }

    private fun loadMembersForGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMembersLoading = true, errorMessage = null) }
            memberRepository.refreshMembers(groupId)
            val groupMembers = memberRepository.getMembers(groupId).first()

            val allMembers = buildMemberList(groupMembers)
            _uiState.update { it.copy(members = allMembers, isMembersLoading = false) }
        }
    }

    private fun buildMemberList(groupMembers: List<GroupMember>): List<SplitMember> {
        val memberSet = mutableSetOf<SplitMember>()

        // Ensure "You" is always present for splitting logic
        val me = SplitMember(id = currentUserId, name = "You", isCurrentUser = true)
        memberSet.add(me)

        groupMembers.forEach { member ->
            val isMe = member.userId == currentUserId
            memberSet.add(SplitMember(
                id = member.userId,
                name = if (isMe) "You" else member.name,
                isCurrentUser = isMe
            ))
        }

        return memberSet.toList().sortedByDescending { it.isCurrentUser }
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { c -> c.isDigit() || c == '.' }
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return
        _uiState.update { it.copy(amount = filtered, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun onGroupSelected(groupId: String) {
        _uiState.update { it.copy(selectedGroupId = groupId, errorMessage = null) }
        loadMembersForGroup(groupId)
    }

    fun onSplitMethodSelected(method: SplitMethod) {
        _uiState.update { it.copy(splitMethod = method, errorMessage = null) }
    }

    fun onPaidBySelected(userId: String) {
        _uiState.update { it.copy(paidByUserId = userId, errorMessage = null) }
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

    fun onCurrencySelected(currency: String) {
        _uiState.update { it.copy(currency = currency, errorMessage = null) }
    }

    fun onShowCreateGroup() {
        _uiState.update {
            it.copy(
                showCreateGroup = true,
                createStep = CreateGroupStep.Basics,
                createName = "",
                createIcon = GroupIcon.Group,
                profileSearchQuery = "",
                selectedMemberIds = emptySet(),
                createErrorMessage = null,
                isLoadingProfiles = true
            )
        }
        viewModelScope.launch {
            val profiles = runCatching { profilesRepository.listAllProfiles() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    allProfiles = profiles.filter { p -> p.id != currentUserId },
                    isLoadingProfiles = false
                )
            }
        }
    }

    fun onDismissCreateGroup() {
        _uiState.update { it.copy(showCreateGroup = false, createStep = CreateGroupStep.Basics) }
    }

    fun onCreateNameChange(value: String) { _uiState.update { it.copy(createName = value) } }
    fun onCreateIconSelected(icon: GroupIcon) { _uiState.update { it.copy(createIcon = icon) } }
    fun onProfileSearchChange(value: String) { _uiState.update { it.copy(profileSearchQuery = value) } }

    fun onToggleMemberSelection(profileId: String) {
        _uiState.update { current ->
            val next = current.selectedMemberIds.toMutableSet()
            if (next.contains(profileId)) next.remove(profileId) else next.add(profileId)
            current.copy(selectedMemberIds = next)
        }
    }

    fun onCreateNextStep() {
        _uiState.update { current ->
            val next = when (current.createStep) {
                CreateGroupStep.Basics -> CreateGroupStep.Members
                CreateGroupStep.Members -> CreateGroupStep.Review
                CreateGroupStep.Review -> CreateGroupStep.Review
            }
            current.copy(createStep = next, createErrorMessage = null)
        }
    }

    fun onCreateBackStep() {
        _uiState.update { current ->
            val prev = when (current.createStep) {
                CreateGroupStep.Basics -> CreateGroupStep.Basics
                CreateGroupStep.Members -> CreateGroupStep.Basics
                CreateGroupStep.Review -> CreateGroupStep.Members
            }
            current.copy(createStep = prev, createErrorMessage = null)
        }
    }

    fun onConfirmCreateGroup() {
        val current = _uiState.value
        val name = current.createName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(createErrorMessage = "Group name is required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingGroup = true, createErrorMessage = null) }
            runCatching {
                val group = groupRepository.createGroup(name, current.createIcon)
                current.selectedMemberIds.forEach { userId ->
                    runCatching { memberRepository.addMember(group.id, userId) }
                }
                memberRepository.refreshMembers(group.id)
                groupRepository.refreshGroups()
                _uiState.update {
                    it.copy(
                        showCreateGroup = false,
                        isCreatingGroup = false,
                        createStep = CreateGroupStep.Basics,
                        selectedGroupId = group.id
                    )
                }
                loadMembersForGroup(group.id)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isCreatingGroup = false,
                        createErrorMessage = "Unable to create group. Please try again."
                    )
                }
            }
        }
    }

    fun onCreateSplit() {
        val state = _uiState.value
        val groupId = state.selectedGroupId ?: return
        if (state.amount.toDoubleOrNull() == null) return
        if (state.members.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Loading group members...") }
            return
        }

        val desc = state.description.trim().ifBlank { "Expense" }
        val amountCents = (state.amount.toDouble() * 100).toLong()

        when (state.splitMethod) {
            SplitMethod.ByItems -> {
                viewModelScope.launch {
                    _events.send(SplitEvent.GoToAssignItems(groupId, state.amount, desc, state.paidByUserId, state.currency))
                }
                return
            }
            SplitMethod.ByPercentage -> {
                viewModelScope.launch {
                    _events.send(SplitEvent.GoToSplitByPercentage(groupId, state.amount, desc, state.paidByUserId, state.currency))
                }
                return
            }
            SplitMethod.Equally -> Unit
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            try {
                // Ensure the list is derived from the latest state, ensuring all members are included
                val allUserIds = state.members.map { it.id }
                Log.d("SplitExpense", "coveredBy map: ${state.coveredBy}")
                val splits = splitEqually(amountCents, allUserIds).map { split ->
                    split.copy(isCoveredBy = state.coveredBy[split.userId])
                }
                Log.d("SplitExpense", "splits with covering: ${splits.map { "${it.userId} -> isCoveredBy=${it.isCoveredBy}" }}")

                expensesRepository.createExpenseWithSplits(
                    groupId = groupId,
                    description = desc,
                    amountCents = amountCents,
                    currency = state.currency,
                    splitMethod = "EQUAL",
                    paidByUserId = state.paidByUserId,
                    splits = splits
                )
                balanceRepository.clearCache(groupId)
                balanceRepository.refreshBalances(groupId)
                expensesRepository.refreshGroupExpenses(groupId)
                groupRepository.refreshGroups()
                activityRepository.refreshActivityFeed()
                runCatching { friendsRepository.getFriendsBalances() }
                _uiState.update { it.copy(isCreating = false) }
                _events.send(SplitEvent.Created)
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        errorMessage = e.message ?: "Failed to add expense. Please try again."
                    )
                }
            }
        }
    }
}
