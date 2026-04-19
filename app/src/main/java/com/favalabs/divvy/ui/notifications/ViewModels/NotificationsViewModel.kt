package com.favalabs.divvy.ui.notifications.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.favalabs.divvy.backend.ActivityRepository
import com.favalabs.divvy.backend.DataResult
import com.favalabs.divvy.models.ActivityFeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val items: List<ActivityFeedItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            activityRepository.getGlobalActivityFeed().collect { result ->
                _uiState.update {
                    when (result) {
                        is DataResult.Loading -> it.copy(isLoading = true, errorMessage = null)
                        is DataResult.Error   -> it.copy(isLoading = false, errorMessage = result.message)
                        is DataResult.Success -> it.copy(
                            isLoading = false,
                            errorMessage = null,
                            items = result.data
                        )
                    }
                }
            }
        }

        // Kick a fresh fetch so the screen is never stale on open.
        viewModelScope.launch {
            try {
                activityRepository.refreshActivityFeed()
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load notifications.") }
            }
        }
    }

    fun onRetry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                activityRepository.refreshActivityFeed()
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load notifications.") }
            }
        }
    }
}