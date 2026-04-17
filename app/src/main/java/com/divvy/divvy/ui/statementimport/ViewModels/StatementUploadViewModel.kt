package com.divvy.divvy.ui.statementimport.ViewModels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divvy.divvy.backend.StatementRepository
import com.divvy.divvy.backend.TransactionHolder
import com.divvy.divvy.models.ParsedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.sentry.Sentry

data class StatementUploadUiState(
    val isProcessing: Boolean = false,
    val processingStep: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class StatementUploadViewModel @Inject constructor(
    private val statementRepository: StatementRepository,
    private val transactionHolder: TransactionHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatementUploadUiState())
    val uiState: StateFlow<StatementUploadUiState> = _uiState.asStateFlow()

    sealed interface UploadEvent {
        data class TransactionsParsed(val transactions: List<ParsedTransaction>) : UploadEvent
    }

    private val _events = Channel<UploadEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingStep = "Extracting text from PDF...", errorMessage = null) }
            try {
                val text = statementRepository.extractTextFromPdf(uri)

                _uiState.update { it.copy(processingStep = "Parsing transactions with AI...") }
                val transactions = statementRepository.parseTransactions(text)

                if (transactions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "No transactions found in this PDF. Make sure it's a bank statement."
                        )
                    }
                    return@launch
                }

                transactionHolder.transactions = transactions
                _uiState.update { it.copy(isProcessing = false) }
                _events.send(UploadEvent.TransactionsParsed(transactions))
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "Failed to process PDF"
                    )
                }
            }
        }
    }
}
