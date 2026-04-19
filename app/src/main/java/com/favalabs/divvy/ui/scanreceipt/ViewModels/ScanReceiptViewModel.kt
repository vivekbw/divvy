package com.favalabs.divvy.ui.scanreceipt.ViewModels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.GeminiReceiptService
import com.favalabs.divvy.backend.ScannedReceiptStore
import com.favalabs.divvy.offline.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import io.sentry.Sentry

enum class ScanUiState { Camera, Processing, Error }

data class ScanScreenState(
    val uiState: ScanUiState = ScanUiState.Camera,
    val flashEnabled: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ScanReceiptViewModel @Inject constructor(
    private val geminiReceiptService: GeminiReceiptService,
    private val scannedReceiptStore: ScannedReceiptStore,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(ScanScreenState())
    val state: StateFlow<ScanScreenState> = _state.asStateFlow()

    sealed interface ScanEvent {
        data object NavigateToReview : ScanEvent
    }

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun toggleFlash() {
        _state.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun processImage(context: Context, uri: Uri) {
        if (!networkMonitor.isOnline.value) {
            _state.update {
                it.copy(
                    uiState = ScanUiState.Error,
                    errorMessage = "Receipt scanning requires an internet connection.\nPlease connect to the internet and try again."
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(uiState = ScanUiState.Processing, errorMessage = null) }
            try {
                val parsed = geminiReceiptService.parseReceipt(context, uri)

                if (parsed.items.isEmpty()) {
                    _state.update {
                        it.copy(
                            uiState = ScanUiState.Error,
                            errorMessage = "Couldn't find any items on this receipt.\nTry a clearer photo or better lighting."
                        )
                    }
                    return@launch
                }

                scannedReceiptStore.store(parsed)
                _state.update { it.copy(uiState = ScanUiState.Camera) }
                _events.send(ScanEvent.NavigateToReview)
            } catch (e: Exception) {
                Sentry.captureException(e)
                _state.update {
                    it.copy(
                        uiState = ScanUiState.Error,
                        errorMessage = "Failed to process: ${e.message ?: "Unknown error"}\nTry again."
                    )
                }
            }
        }
    }

    fun processImageFromFile(context: Context, file: File) {
        processImage(context, Uri.fromFile(file))
    }

    fun dismissError() {
        _state.update { it.copy(uiState = ScanUiState.Camera, errorMessage = null) }
    }
}
