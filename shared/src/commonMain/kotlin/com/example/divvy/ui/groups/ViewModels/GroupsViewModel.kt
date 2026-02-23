package com.example.divvy.ui.groups.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.GroupRepository
import com.example.divvy.models.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManageGroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false
)

class GroupsViewModel(
    private val groupsRepository: GroupRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageGroupsUiState(isLoading = true))
    val uiState: StateFlow<ManageGroupsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupsRepository.listGroups().collect { groups ->
                _uiState.value = ManageGroupsUiState(groups = groups, isLoading = false)
            }
        }
    }
}
