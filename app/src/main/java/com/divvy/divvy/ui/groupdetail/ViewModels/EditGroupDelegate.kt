package com.divvy.divvy.ui.groupdetail.ViewModels

import com.divvy.divvy.backend.GroupRepository
import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.Group
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditGroupState(
    val isEditing: Boolean = false,
    val editName: String = "",
    val editIcon: GroupIcon = GroupIcon.Group,
    val isSaving: Boolean = false
)

class EditGroupDelegate(
    private val groupId: String,
    private val scope: CoroutineScope,
    private val groupRepository: GroupRepository
) {
    private val _state = MutableStateFlow(EditGroupState())
    val state: StateFlow<EditGroupState> = _state.asStateFlow()

    fun onStartEdit(group: Group) {
        _state.update { it.copy(isEditing = true, editName = group.name, editIcon = group.icon) }
    }

    fun onCancelEdit() {
        _state.update { it.copy(isEditing = false) }
    }

    fun onEditNameChange(value: String) {
        _state.update { it.copy(editName = value) }
    }

    fun onEditIconSelected(icon: GroupIcon) {
        _state.update { it.copy(editIcon = icon) }
    }

    fun onSaveEdit() {
        val s = _state.value
        val name = s.editName.trim()
        if (name.isBlank()) return
        scope.launch {
            _state.update { it.copy(isSaving = true) }
            groupRepository.updateGroup(groupId, name, s.editIcon)
            _state.update { EditGroupState() }
        }
    }
}
