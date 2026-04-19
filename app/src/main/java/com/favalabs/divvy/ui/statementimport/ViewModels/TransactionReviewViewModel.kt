package com.favalabs.divvy.ui.statementimport.ViewModels

import androidx.lifecycle.ViewModel
import com.favalabs.divvy.backend.TransactionHolder
import com.favalabs.divvy.models.ParsedTransaction
import com.favalabs.divvy.models.TransactionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ReviewableTransaction(
    val index: Int,
    val transaction: ParsedTransaction,
    val status: TransactionStatus = TransactionStatus.Pending
)

enum class FilterTab { All, Pending, Added, Skipped }

data class TransactionReviewUiState(
    val transactions: List<ReviewableTransaction> = emptyList(),
    val activeFilter: FilterTab = FilterTab.All,
    val selectedTransaction: ReviewableTransaction? = null
) {
    val filteredTransactions: List<ReviewableTransaction>
        get() = when (activeFilter) {
            FilterTab.All -> transactions
            FilterTab.Pending -> transactions.filter { it.status == TransactionStatus.Pending }
            FilterTab.Added -> transactions.filter { it.status == TransactionStatus.Added }
            FilterTab.Skipped -> transactions.filter { it.status == TransactionStatus.Skipped }
        }

    val addedCount: Int get() = transactions.count { it.status == TransactionStatus.Added }
    val totalCount: Int get() = transactions.size
}

@HiltViewModel
class TransactionReviewViewModel @Inject constructor(
    transactionHolder: TransactionHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionReviewUiState())
    val uiState: StateFlow<TransactionReviewUiState> = _uiState.asStateFlow()

    init {
        val transactions = transactionHolder.transactions
        if (transactions.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    transactions = transactions.mapIndexed { index, tx ->
                        ReviewableTransaction(index = index, transaction = tx)
                    }
                )
            }
        }
    }

    fun onFilterSelected(filter: FilterTab) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun onTransactionSelected(transaction: ReviewableTransaction) {
        _uiState.update { it.copy(selectedTransaction = transaction) }
    }

    fun onDismissDetail() {
        _uiState.update { it.copy(selectedTransaction = null) }
    }

    fun onSkipTransaction(index: Int) {
        updateStatus(index, TransactionStatus.Skipped)
        _uiState.update { it.copy(selectedTransaction = null) }
    }

    fun onMarkAdded(index: Int) {
        updateStatus(index, TransactionStatus.Added)
        _uiState.update { it.copy(selectedTransaction = null) }
    }

    private fun updateStatus(index: Int, status: TransactionStatus) {
        _uiState.update { current ->
            current.copy(
                transactions = current.transactions.map { tx ->
                    if (tx.index == index) tx.copy(status = status) else tx
                }
            )
        }
    }
}
