package com.divvy.divvy.ui.expenses.ViewModels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor() : ViewModel() {
    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status
}
