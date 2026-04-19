package com.favalabs.divvy.ui.joingroup.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.MemberRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class JoinGroupUiState(
    val groupName: String = "",
    val isLoading: Boolean = false,
    val joined: Boolean = false,
    val error: String? = null
)

@HiltViewModel(assistedFactory = JoinGroupViewModel.Factory::class)
class JoinGroupViewModel @AssistedInject constructor(
    @Assisted("id") val groupId: String,
    @Assisted("name") val groupName: String,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("id") groupId: String,
            @Assisted("name") groupName: String
        ): JoinGroupViewModel
    }

    private val _uiState = MutableStateFlow(JoinGroupUiState(groupName = groupName))
    val uiState: StateFlow<JoinGroupUiState> = _uiState.asStateFlow()

    fun onJoin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                memberRepository.joinGroup(groupId)
                groupRepository.invalidateCache()
                groupRepository.refreshGroups()
                _uiState.update { it.copy(isLoading = false, joined = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to join group. Please try again.") }
            }
        }
    }
}
