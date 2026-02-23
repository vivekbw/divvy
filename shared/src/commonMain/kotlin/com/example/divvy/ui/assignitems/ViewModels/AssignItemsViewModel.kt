package com.example.divvy.ui.assignitems.ViewModels

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.CURRENT_USER_ID
import com.example.divvy.backend.GroupRepository
import com.example.divvy.formatDouble
import com.example.divvy.models.ExpenseSplit
import com.example.divvy.models.GroupExpense
import com.example.divvy.models.splitEqually
import com.example.divvy.randomUuidString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class AssignMember(val id: String, val name: String, val color: Color)

data class ReceiptItem(val id: String, val name: String, val priceCents: Long) {
    val formattedPrice: String get() = "$${formatDouble(priceCents / 100.0, 2)}"
}

data class AssignItemsUiState(
    val description: String = "",
    val amountDisplay: String = "",
    val members: List<AssignMember> = emptyList(),
    val items: List<ReceiptItem> = emptyList(),
    val assignments: Map<String, Set<String>> = emptyMap(),
    val expandedItemId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

private val MemberColors = listOf(
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF7C4DFF),
    Color(0xFF2196F3), Color(0xFF00695C), Color(0xFFE91E63)
)

private val stubItems = listOf(
    ReceiptItem("i1", "Organic Bananas", 399), ReceiptItem("i2", "Almond Milk", 450),
    ReceiptItem("i3", "Sourdough Bread", 599), ReceiptItem("i4", "Avocados (4 pack)", 699),
    ReceiptItem("i5", "Chicken Breast", 1299)
)

class AssignItemsViewModel(
    private val groupId: String,
    private val amountDisplay: String,
    private val description: String,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignItemsUiState(description = description.ifBlank { "Receipt" }, amountDisplay = amountDisplay, isLoading = true))
    val uiState: StateFlow<AssignItemsUiState> = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val groupMembers = groupRepository.getMembers(groupId).first()
            val allMembers = mutableListOf(AssignMember(CURRENT_USER_ID, "You", MemberColors[0]))
            groupMembers.forEachIndexed { i, gm -> allMembers += AssignMember(gm.userId, gm.name, MemberColors[(i + 1) % MemberColors.size]) }
            _uiState.update { it.copy(members = allMembers, items = stubItems, isLoading = false) }
        }
    }

    fun onItemTap(itemId: String) { _uiState.update { it.copy(expandedItemId = if (it.expandedItemId == itemId) null else itemId) } }

    fun onToggleMemberForItem(itemId: String, memberId: String) {
        _uiState.update { state ->
            val current = state.assignments[itemId].orEmpty()
            val updated = if (memberId in current) current - memberId else current + memberId
            state.copy(assignments = state.assignments + (itemId to updated))
        }
    }

    fun onNext() {
        val state = _uiState.value
        val amountCents = ((state.amountDisplay.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val perUser = mutableMapOf<String, Long>()
        for (item in state.items) {
            val assignees = state.assignments[item.id].orEmpty()
            if (assignees.isEmpty()) continue
            for (split in splitEqually(item.priceCents, assignees.toList())) {
                perUser[split.userId] = (perUser[split.userId] ?: 0L) + split.amountCents
            }
        }
        val splits = state.members.map { m -> ExpenseSplit(m.id, perUser[m.id] ?: 0L) }
        val expense = GroupExpense(
            id = randomUuidString(), groupId = groupId, title = state.description,
            amountCents = amountCents, paidByUserId = CURRENT_USER_ID, splits = splits,
            createdAt = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            groupRepository.addExpense(expense)
            _uiState.update { it.copy(isSaving = false) }
            _done.send(Unit)
        }
    }
}
