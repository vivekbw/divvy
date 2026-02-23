package com.example.divvy.ui.splitpercentage.ViewModels

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.CURRENT_USER_ID
import com.example.divvy.backend.GroupRepository
import com.example.divvy.formatDouble
import com.example.divvy.models.GroupExpense
import com.example.divvy.models.splitByPercentage
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

data class PercentageMember(val id: String, val name: String, val color: Color)

data class SplitByPercentageUiState(
    val description: String = "",
    val amountDisplay: String = "",
    val members: List<PercentageMember> = emptyList(),
    val percentages: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
) {
    val totalPercentage: Double get() = percentages.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val isValid: Boolean get() = kotlin.math.abs(totalPercentage - 100.0) < 0.01
    fun dollarAmountFor(memberId: String): String {
        val total = amountDisplay.toDoubleOrNull() ?: 0.0
        val pct = percentages[memberId]?.toDoubleOrNull() ?: 0.0
        return "$${formatDouble(total * pct / 100.0, 2)}"
    }
}

private val MemberColors = listOf(
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF7C4DFF),
    Color(0xFF2196F3), Color(0xFF00695C), Color(0xFFE91E63)
)

class SplitByPercentageViewModel(
    private val groupId: String,
    private val amountDisplay: String,
    private val description: String,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitByPercentageUiState(description = description.ifBlank { "Expense" }, amountDisplay = amountDisplay, isLoading = true))
    val uiState: StateFlow<SplitByPercentageUiState> = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val groupMembers = groupRepository.getMembers(groupId).first()
            val allMembers = mutableListOf(PercentageMember(CURRENT_USER_ID, "You", MemberColors[0]))
            groupMembers.forEachIndexed { i, gm -> allMembers += PercentageMember(gm.userId, gm.name, MemberColors[(i + 1) % MemberColors.size]) }
            val equalPct = formatDouble(100.0 / allMembers.size, 1)
            _uiState.update { it.copy(members = allMembers, percentages = allMembers.associate { m -> m.id to equalPct }, isLoading = false) }
        }
    }

    fun onPercentageChange(memberId: String, value: String) {
        val filtered = value.filter { c -> c.isDigit() || c == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 1) return
        _uiState.update { state -> state.copy(percentages = state.percentages + (memberId to filtered)) }
    }

    fun onSplitEvenly() {
        _uiState.update { state ->
            val equalPct = formatDouble(100.0 / state.members.size, 1)
            state.copy(percentages = state.members.associate { it.id to equalPct })
        }
    }

    fun onDone() {
        val state = _uiState.value
        if (!state.isValid) return
        val amountCents = ((state.amountDisplay.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val percentages = state.members.associate { m -> m.id to (state.percentages[m.id]?.toDoubleOrNull() ?: 0.0) }
        val splits = splitByPercentage(amountCents, percentages)
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
