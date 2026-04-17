package com.divvy.divvy.ui.auth.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divvy.divvy.backend.ProfilesRepository
import com.divvy.divvy.backend.SupabaseClientProvider
import com.divvy.divvy.models.ProfileRow
import com.divvy.divvy.SentryUserSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import javax.inject.Inject
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OAuthFlow {
    CREATE,
    LOGIN
}

data class AuthFlowState(
    val otp: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val profileEmail: String = "",
    val phoneDigits: String = "",
    val countryCode: String = "+1",
    val countryFlag: String = "🇺🇸",
    val phoneVerified: Boolean = false,
    val authMethod: String? = null,
    val otpSent: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthFlowViewModel @Inject constructor(
    private val profilesRepository: ProfilesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val profilePrefs by lazy {
        appContext.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
    }

    private val _state = MutableStateFlow(AuthFlowState())
    val state: StateFlow<AuthFlowState> = _state.asStateFlow()

    private var pendingOAuthFlow: OAuthFlow? = null

    val sessionStatus: StateFlow<SessionStatus> = if (SupabaseClientProvider.isConfigured()) {
        SupabaseClientProvider.client.auth.sessionStatus
    } else {
        MutableStateFlow(SessionStatus.NotAuthenticated)
    }

    fun updateCountryOption(flag: String, code: String) = _state.update {
        val maxDigits = maxDigitsForCountry(code)
        val trimmed = it.phoneDigits.take(maxDigits)
        it.copy(
            countryFlag = flag,
            countryCode = code,
            phoneDigits = trimmed,
            phoneVerified = false,
            otp = "",
            otpSent = false,
            errorMessage = null
        )
    }

    fun updatePhoneDigits(value: String) = _state.update {
        val method = if (it.authMethod == "GOOGLE") it.authMethod else "PHONE"
        val maxDigits = maxDigitsForCountry(it.countryCode)
        val digits = value.filter { ch -> ch.isDigit() }.take(maxDigits)
        it.copy(
            phoneDigits = digits,
            phoneVerified = false,
            otp = "",
            otpSent = false,
            errorMessage = null,
            authMethod = method
        )
    }
    fun updateOtp(value: String) = _state.update { it.copy(otp = value, errorMessage = null) }
    fun updateFirstName(value: String) = _state.update { it.copy(firstName = value, errorMessage = null) }
    fun updateLastName(value: String) = _state.update { it.copy(lastName = value, errorMessage = null) }
    fun updateProfileEmail(value: String) = _state.update { it.copy(profileEmail = value, errorMessage = null) }

    fun startGoogleSignIn(flow: OAuthFlow) {
        if (!checkConfigured()) return
        pendingOAuthFlow = flow
        _state.update { it.copy(authMethod = "GOOGLE") }
        launchAuth {
            SupabaseClientProvider.client.auth.signInWith(Google)
        }
    }

    fun sendPhoneOtp(createUser: Boolean, onSuccess: () -> Unit) {
        if (!checkConfigured()) return
        val phone = phoneE164()
        if (phone.isBlank()) {
            _state.update { it.copy(errorMessage = "Phone number is required.") }
            return
        }
        launchAuth {
            _state.update { it.copy(authMethod = "PHONE") }
            SupabaseClientProvider.client.auth.signInWith(OTP) {
                this.phone = phone
                this.createUser = createUser
            }
            _state.update { it.copy(otpSent = true) }
            onSuccess()
        }
    }

    fun verifyPhoneOtp(onSuccess: () -> Unit) {
        if (!checkConfigured()) return
        val token = state.value.otp.trim()
        val phone = phoneE164()
        if (phone.isBlank() || token.length != 6) {
            _state.update { it.copy(errorMessage = "Enter the 6-digit code.") }
            return
        }
        launchAuth {
            SupabaseClientProvider.client.auth.verifyPhoneOtp(
                type = OtpType.Phone.SMS,
                phone = phone,
                token = token
            )
            _state.update { it.copy(phoneVerified = true) }
            onSuccess()
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        if (!checkConfigured()) return
        val first = state.value.firstName.trim()
        val last = state.value.lastName.trim()
        val enteredEmail = state.value.profileEmail.trim()
        val phone = phoneE164()
        if (first.isBlank() || last.isBlank()) {
            _state.update { it.copy(errorMessage = "First name and last name are required.") }
            return
        }
        if (phone.isBlank()) {
            _state.update { it.copy(errorMessage = "Phone number is required.") }
            return
        }
        launchAuth {
            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: SupabaseClientProvider.client.auth.retrieveUserForCurrentSession(updateSession = true)
            val email = enteredEmail.ifBlank { user.email.orEmpty() }
            if (email.isBlank()) {
                _state.update { it.copy(errorMessage = "Email is required.") }
                return@launchAuth
            }
            val method = state.value.authMethod ?: "PHONE"
            profilesRepository.upsertProfile(
                ProfileRow(
                    id = user.id,
                    firstName = first,
                    lastName = last,
                    authMethod = method,
                    email = email,
                    phone = phone,
                    phoneVerified = state.value.phoneVerified
                )
            )
            // Cache profile existence for offline auth
            profilePrefs.edit().putBoolean("has_profile_${user.id}", true).apply()
            // Tag all subsequent Sentry events with this user's opaque UUID
            SentryUserSync.attach(user.id)
            onSuccess()
        }
    }

    fun prefillProfileEmail() {
        if (!checkConfigured()) return
        if (state.value.profileEmail.isNotBlank()) return
        viewModelScope.launch {
            try {
                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                    ?: SupabaseClientProvider.client.auth.retrieveUserForCurrentSession(updateSession = true)
                val email = user.email?.trim().orEmpty()
                if (email.isNotBlank()) {
                    _state.update { it.copy(profileEmail = email) }
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun consumeOAuthFlow(): OAuthFlow? {
        val flow = pendingOAuthFlow
        pendingOAuthFlow = null
        return flow
    }

    suspend fun hasProfile(): Boolean {
        if (!checkConfigured()) return false
        return try {
            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: SupabaseClientProvider.client.auth.retrieveUserForCurrentSession(updateSession = true)
            val profile = profilesRepository.getProfile(user.id)
            val hasIt = profile != null
            if (hasIt) {
                // Cache successful profile check for offline use
                profilePrefs.edit().putBoolean("has_profile_${user.id}", true).apply()
            }
            hasIt
        } catch (e: Exception) {
            // Network failed — check local cache
            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
            if (user != null && profilePrefs.getBoolean("has_profile_${user.id}", false)) {
                return true
            }
            Sentry.captureException(e)
            false
        }
    }

    private fun checkConfigured(): Boolean {
        if (!SupabaseClientProvider.isConfigured()) {
            _state.update {
                it.copy(
                    errorMessage = "Missing Supabase keys. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties."
                )
            }
            return false
        }
        return true
    }

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                block()
            } catch (e: Exception) {
                val raw = e.message ?: "Something went wrong."
                val friendly = if (raw.contains("Token has expired", ignoreCase = true) ||
                    raw.contains("invalid", ignoreCase = true)
                ) {
                    "Code is invalid or expired. Please request a new one."
                } else {
                    raw.lines().firstOrNull()?.take(160) ?: "Something went wrong."
                }
                _state.update { it.copy(errorMessage = friendly) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun phoneE164(): String {
        val digits = state.value.phoneDigits.trim()
        if (digits.isBlank()) return ""
        return "${state.value.countryCode}$digits"
    }

    private fun maxDigitsForCountry(countryCode: String): Int {
        return if (countryCode == "+1") 10 else 15
    }
}
