package com.example.divvy.ui.groups.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.ActivityRepository
import com.example.divvy.backend.AuthRepository
import com.example.divvy.backend.DataResult
import com.example.divvy.backend.ExpensesRepository
import com.example.divvy.backend.GroupRepository
import com.example.divvy.backend.MemberRepository
import com.example.divvy.backend.ProfilesRepository
import com.example.divvy.components.GroupIcon
import com.example.divvy.models.Group
import com.example.divvy.models.ProfileRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CreateGroupStep { Basics, Members, Review }

data class ManageGroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateGroupSheet: Boolean = false,
    val createStep: CreateGroupStep = CreateGroupStep.Basics,
    val createName: String = "",
    val createIcon: GroupIcon = GroupIcon.Group,
    val allProfiles: List<ProfileRow> = emptyList(),
    val profileSearchQuery: String = "",
    val selectedMemberIds: Set<String> = emptySet(),
    val isLoadingProfiles: Boolean = false,
    val isCreating: Boolean = false,
    val createErrorMessage: String? = null,
    val createCompletedGroupId: String? = null
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val expensesRepository: ExpensesRepository,
    private val memberRepository: MemberRepository,
    private val profilesRepository: ProfilesRepository,
    private val activityRepository: ActivityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageGroupsUiState(isLoading = true))
    val uiState: StateFlow<ManageGroupsUiState> = _uiState.asStateFlow()
    private val currentUserId: String = authRepository.getCurrentUserId()

    init {
        viewModelScope.launch {
            combine(
                groupRepository.listGroups(),
                expensesRepository.observeAllGroupExpenses()
            ) { groupsResult, allExpenses ->
                groupsResult to allExpenses
            }.collect { (result, allExpenses) ->
                _uiState.update { current ->
                    when (result) {
                        is DataResult.Loading -> current.copy(isLoading = true, errorMessage = null)
                        is DataResult.Error -> current.copy(isLoading = false, errorMessage = result.message)
                        is DataResult.Success -> {
                            val computedByGroup = balanceByGroupForUser(allExpenses, currentUserId)
                            val groups = result.data.map { g ->
                                g.copy(balanceCents = computedByGroup[g.id] ?: 0L)
                            }
                            current.copy(groups = groups, isLoading = false, errorMessage = null)
                        }
                    }
                }
            }
        }
        viewModelScope.launch { expensesRepository.refreshAllExpenses() }
    }

    fun onRetry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            groupRepository.refreshGroups()
        }
    }

    fun onCreateGroupClick() {
        _uiState.update {
            it.copy(
                showCreateGroupSheet = true,
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

    fun onCreateGroupDismiss() {
        _uiState.update {
            it.copy(
                showCreateGroupSheet = false,
                createStep = CreateGroupStep.Basics,
                profileSearchQuery = "",
                createErrorMessage = null
            )
        }
    }

    fun onCreateNameChange(value: String) { _uiState.update { it.copy(createName = value) } }
    fun onCreateIconSelected(icon: GroupIcon) { _uiState.update { it.copy(createIcon = icon) } }
    fun onProfileSearchChange(value: String) { _uiState.update { it.copy(profileSearchQuery = value) } }
    fun onToggleMemberSelection(profileId: String) {
        _uiState.update { current ->
            val nextSelected = current.selectedMemberIds.toMutableSet()
            if (nextSelected.contains(profileId)) nextSelected.remove(profileId) else nextSelected.add(profileId)
            current.copy(selectedMemberIds = nextSelected)
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
            val previous = when (current.createStep) {
                CreateGroupStep.Basics -> CreateGroupStep.Basics
                CreateGroupStep.Members -> CreateGroupStep.Basics
                CreateGroupStep.Review -> CreateGroupStep.Members
            }
            current.copy(createStep = previous, createErrorMessage = null)
        }
    }

    fun submitCreateGroup() {
        viewModelScope.launch {
            val current = _uiState.value
            val groupName = current.createName.trim()
            if (groupName.isBlank()) {
                _uiState.update { it.copy(createErrorMessage = "Group name is required.") }
                return@launch
            }

            _uiState.update { it.copy(isCreating = true, createErrorMessage = null) }
            runCatching {
                val createdGroup = groupRepository.createGroup(groupName, current.createIcon)
                val failedInvites = mutableListOf<String>()
                current.selectedMemberIds.forEach { userId ->
                    val result = runCatching { memberRepository.addMember(createdGroup.id, userId) }
                    if (result.isFailure) failedInvites.add(userId)
                }

                memberRepository.refreshMembers(createdGroup.id)
                groupRepository.refreshGroups()
                activityRepository.refreshActivityFeed()

                _uiState.update {
                    it.copy(
                        isCreating = false,
                        showCreateGroupSheet = false,
                        createStep = CreateGroupStep.Basics,
                        createErrorMessage = if (failedInvites.isNotEmpty()) {
                            "Some invites failed to send. You can retry from the group."
                        } else {
                            null
                        },
                        createCompletedGroupId = createdGroup.id
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        createErrorMessage = "Unable to create group. Please try again."
                    )
                }
            }
        }
    }

    fun onCreateNavigationHandled() {
        _uiState.update { it.copy(createCompletedGroupId = null) }
    }

    private fun balanceByGroupForUser(
        allExpenses: List<com.example.divvy.models.GroupExpense>,
        userId: String
    ): Map<String, Long> {
        return allExpenses.groupBy { it.groupId }.mapValues { (_, expenses) ->
            expenses.sumOf { expense ->
                val paid = if (expense.paidByUserId == userId) expense.amountCents else 0L
                val share = expense.splits.find { it.userId == userId }?.amountCents ?: 0L
                paid - share
            }
        }
    }
}
