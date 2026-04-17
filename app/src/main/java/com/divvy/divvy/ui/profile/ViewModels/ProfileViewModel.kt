package com.divvy.divvy.ui.profile.ViewModels

import com.divvy.divvy.notifications.ExpenseNotificationService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divvy.divvy.FeatureFlags
import com.divvy.divvy.SentryUserSync
import com.divvy.divvy.ui.auth.DummyAccount
import com.divvy.divvy.backend.ProfilesRepository
import com.divvy.divvy.backend.SupabaseClientProvider
import com.divvy.divvy.models.ProfileRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.sentry.Sentry

data class ProfileUiState(
    val profile: ProfileRow? = null,
    val email: String? = null,
    val phone: String? = null,
    val phoneVerified: Boolean? = null,
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profilesRepository: ProfilesRepository,
    private val expenseNotificationService: ExpenseNotificationService
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (FeatureFlags.AUTH_BYPASS) {
                _uiState.update {
                    it.copy(
                        profile = DummyAccount.profile,
                        email = DummyAccount.profile.email,
                        phone = DummyAccount.profile.phone,
                        phoneVerified = DummyAccount.profile.phoneVerified,
                        avatarUrl = null,
                        isLoading = false
                    )
                }
                return@launch
            }
            try {
                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                    ?: SupabaseClientProvider.client.auth.retrieveUserForCurrentSession(updateSession = true)
                val profile = profilesRepository.getProfile(user.id)
                val metadata = user.userMetadata?.jsonObject
                val phone = profile?.phone ?: user.phone
                val avatarUrl = metadata
                    ?.get("avatar_url")
                    ?.jsonPrimitive
                    ?.content
                    ?.takeIf { it.isNotBlank() }
                    ?: metadata
                        ?.get("picture")
                        ?.jsonPrimitive
                        ?.content
                        ?.takeIf { it.isNotBlank() }
                _uiState.update {
                    it.copy(
                        profile = profile,
                        email = user.email,
                        phone = phone,
                        phoneVerified = profile?.phoneVerified,
                        avatarUrl = avatarUrl,
                        isLoading = false
                    )
                }
                // Keep Sentry user context fresh for returning users
                SentryUserSync.attach(user.id)
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load profile.")
                }
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (FeatureFlags.AUTH_BYPASS) {
                _uiState.update { it.copy(isLoading = false, profile = null, email = null, phone = null, phoneVerified = null) }
                onComplete()
                return@launch
            }
            try {
                SupabaseClientProvider.client.auth.signOut()
                SentryUserSync.detach()
                expenseNotificationService.stop()
                _uiState.update { it.copy(isLoading = false, profile = null, email = null, phone = null, phoneVerified = null) }
                onComplete()
            } catch (e: Exception) {
                Sentry.captureException(e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to sign out.")
                }
            }
        }
    }
}