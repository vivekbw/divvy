package com.favalabs.divvy.ui.groupdetail.ViewModels

import com.favalabs.divvy.backend.MemberRepository
import com.favalabs.divvy.backend.ProfilesRepository
import com.favalabs.divvy.models.ProfileRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InviteMembersState(
    val showSheet: Boolean = false,
    val allProfiles: List<ProfileRow> = emptyList(),
    val searchQuery: String = "",
    val isAdding: Boolean = false
)

class InviteMembersDelegate(
    private val groupId: String,
    private val scope: CoroutineScope,
    private val memberRepository: MemberRepository,
    private val profilesRepository: ProfilesRepository,
    private val onMemberAdded: (String) -> Unit
) {
    private val _state = MutableStateFlow(InviteMembersState())
    val state: StateFlow<InviteMembersState> = _state.asStateFlow()

    fun onShowSheet() {
        scope.launch {
            val profiles = profilesRepository.listAllProfiles()
            _state.update { it.copy(showSheet = true, allProfiles = profiles, searchQuery = "") }
        }
    }

    fun onDismissSheet() {
        _state.update { it.copy(showSheet = false, searchQuery = "") }
    }

    fun onSearchChange(value: String) {
        _state.update { it.copy(searchQuery = value) }
    }

    fun onInviteMember(profile: ProfileRow) {
        scope.launch {
            _state.update { it.copy(isAdding = true) }
            memberRepository.addMember(groupId, profile.id)
            onMemberAdded(profile.id)
            _state.update { it.copy(isAdding = false) }
        }
    }
}
