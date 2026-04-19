package com.favalabs.divvy.ui.groupmembers.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.ActivityRepository
import com.favalabs.divvy.backend.AuthRepository
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.MemberRepository
import com.favalabs.divvy.backend.ProfilesRepository
import com.favalabs.divvy.models.GroupMember
import com.favalabs.divvy.models.ProfileRow
import com.favalabs.divvy.ui.groupdetail.buildGroupInviteLink
import com.favalabs.divvy.ui.groupdetail.ViewModels.InviteMembersDelegate
import com.favalabs.divvy.ui.groupdetail.ViewModels.InviteMembersState
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

data class GroupMembersUiState(
    val members: List<GroupMember> = emptyList(),
    val isLoading: Boolean = true,
    val isCreator: Boolean = false,
    val currentMemberIds: Set<String> = emptySet(),
    val leftGroup: Boolean = false,
    val deletedGroup: Boolean = false,
    val inviteUrl: String = "",
    val inviteMembers: InviteMembersState = InviteMembersState()
) {
    val showInviteSheet get() = inviteMembers.showSheet
    val allProfiles get() = inviteMembers.allProfiles
    val inviteSearchQuery get() = inviteMembers.searchQuery
    val isAddingMember get() = inviteMembers.isAdding
}

@HiltViewModel(assistedFactory = GroupMembersViewModel.Factory::class)
class GroupMembersViewModel @AssistedInject constructor(
    @Assisted private val groupId: String,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val profilesRepository: ProfilesRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(groupId: String): GroupMembersViewModel
    }

    private val _uiState = MutableStateFlow(GroupMembersUiState())
    val uiState: StateFlow<GroupMembersUiState> = _uiState.asStateFlow()

    private val myUserId: String = authRepository.getCurrentUserId()

    private val inviteDelegate = InviteMembersDelegate(
        groupId = groupId,
        scope = viewModelScope,
        memberRepository = memberRepository,
        profilesRepository = profilesRepository,
        onMemberAdded = { profileId ->
            _uiState.update { it.copy(currentMemberIds = it.currentMemberIds + profileId) }
            viewModelScope.launch {
                groupRepository.refreshGroups()
                activityRepository.refreshActivityFeed()
                memberRepository.clearCache(groupId)
                memberRepository.refreshMembers(groupId)
            }
        }
    )

    init {
        _uiState.update { it.copy(inviteUrl = "divvy.app/join/$groupId") }

        viewModelScope.launch {
            memberRepository.refreshMembers(groupId)
        }

        viewModelScope.launch {
            combine(
                groupRepository.getGroup(groupId),
                memberRepository.getMembers(groupId)
            ) { group, members ->
                val memberIds = members.map { it.userId }.toSet() + myUserId
                Triple(group, members, memberIds)
            }.collect { (group, members, memberIds) ->
                _uiState.update { current ->
                    current.copy(
                        members = members.sortedBy { it.name.lowercase() },
                        isLoading = false,
                        isCreator = group.createdBy == myUserId,
                        currentMemberIds = memberIds,
                        inviteUrl = buildGroupInviteLink(groupId, group.name)
                    )
                }
            }
        }

        viewModelScope.launch {
            inviteDelegate.state.collect { s ->
                _uiState.update { it.copy(inviteMembers = s) }
            }
        }
    }

    fun onShowInviteSheet() = inviteDelegate.onShowSheet()
    fun onDismissInviteSheet() = inviteDelegate.onDismissSheet()
    fun onInviteSearchChange(value: String) = inviteDelegate.onSearchChange(value)
    fun onInviteMember(profile: ProfileRow) = inviteDelegate.onInviteMember(profile)

    fun onLeaveGroup() {
        viewModelScope.launch {
            memberRepository.leaveGroup(groupId)
            groupRepository.invalidateCache()
            groupRepository.refreshGroups()
            activityRepository.refreshActivityFeed()
            _uiState.update { it.copy(leftGroup = true) }
        }
    }

    fun onDeleteGroup() {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
            groupRepository.refreshGroups()
            activityRepository.refreshActivityFeed()
            _uiState.update { it.copy(deletedGroup = true) }
        }
    }
}
