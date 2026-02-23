package com.example.divvy.ui.auth.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvy.backend.ProfilesRepository
import com.example.divvy.backend.SupabaseClientProvider
import com.example.divvy.models.ProfileRow
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

enum class OAuthFlow { CREATE, LOGIN }

data class AuthFlowState(
    val otp: String = "", val firstName: String = "", val lastName: String = "",
    val profileEmail: String = "", val phoneDigits: String = "",
    val countryCode: String = "+1", val countryFlag: String = "\uD83C\uDDFA\uD83C\uDDF8",
    val phoneVerified: Boolean = false, val authMethod: String? = null,
    val otpSent: Boolean = false, val isLoading: Boolean = false, val errorMessage: String? = null
)

class AuthFlowViewModel(
    private val profilesRepository: ProfilesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthFlowState())
    val state: StateFlow<AuthFlowState> = _state.asStateFlow()
    private var pendingOAuthFlow: OAuthFlow? = null

    val sessionStatus: StateFlow<SessionStatus> = if (SupabaseClientProvider.isConfigured()) {
        SupabaseClientProvider.client.auth.sessionStatus
    } else { MutableStateFlow(SessionStatus.NotAuthenticated) }

    fun updateCountryOption(flag: String, code: String) = _state.update {
        val maxDigits = maxDigitsForCountry(code)
        it.copy(countryFlag = flag, countryCode = code, phoneDigits = it.phoneDigits.take(maxDigits), phoneVerified = false, otp = "", otpSent = false, errorMessage = null)
    }
    fun updatePhoneDigits(value: String) = _state.update {
        val method = if (it.authMethod == "GOOGLE") it.authMethod else "PHONE"
        val digits = value.filter { ch -> ch.isDigit() }.take(maxDigitsForCountry(it.countryCode))
        it.copy(phoneDigits = digits, phoneVerified = false, otp = "", otpSent = false, errorMessage = null, authMethod = method)
    }
    fun updateOtp(value: String) = _state.update { it.copy(otp = value, errorMessage = null) }
    fun updateFirstName(value: String) = _state.update { it.copy(firstName = value, errorMessage = null) }
    fun updateLastName(value: String) = _state.update { it.copy(lastName = value, errorMessage = null) }
    fun updateProfileEmail(value: String) = _state.update { it.copy(profileEmail = value, errorMessage = null) }

    fun startGoogleSignIn(flow: OAuthFlow) {
        if (!checkConfigured()) return
        pendingOAuthFlow = flow
        _state.update { it.copy(authMethod = "GOOGLE") }
        launchAuth { SupabaseClientProvider.client.auth.signInWith(Google) }
    }

    fun sendPhoneOtp(createUser: Boolean, onSuccess: () -> Unit) {
        if (!checkConfigured()) return
        val phone = phoneE164()
        if (phone.isBlank()) { _state.update { it.copy(errorMessage = "Phone number is required.") }; return }
        launchAuth {
            _state.update { it.copy(authMethod = "PHONE") }
            SupabaseClientProvider.client.auth.signInWith(OTP) { this.phone = phone; this.createUser = createUser }
            _state.update { it.copy(otpSent = true) }
            onSuccess()
        }
    }

    fun verifyPhoneOtp(onSuccess: () -> Unit) {
        if (!checkConfigured()) return
        val token = state.value.otp.trim(); val phone = phoneE164()
        if (phone.isBlank() || token.length != 6) { _state.update { it.copy(errorMessage = "Enter the 6-digit code.") }; return }
        launchAuth {
            SupabaseClientProvider.client.auth.verifyPhoneOtp(type = OtpType.Phone.SMS, phone = phone, token = token)
            _state.update { it.copy(phoneVerified = true) }; onSuccess()
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        if (!checkConfigured()) return
        val first = state.value.firstName.trim(); val last = state.value.lastName.trim()
        val enteredEmail = state.value.profileEmail.trim(); val phone = phoneE164()
        if (first.isBlank() || last.isBlank()) { _state.update { it.copy(errorMessage = "First name and last name are required.") }; return }
        if (phone.isBlank()) { _state.update { it.copy(errorMessage = "Phone number is required.") }; return }
        launchAuth {
            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: SupabaseClientProvider.client.auth.retrieveUserForCurrentSession(updateSession = true)
            val email = enteredEmail.ifBlank { user.email.orEmpty() }
            if (email.isBlank()) { _state.update { it.copy(errorMessage = "Email is required.") }; return@launchAuth }
            profilesRepository.upsertProfile(ProfileRow(id = user.id, firstName = first, lastName = last, authMethod = state.value.authMethod ?: "PHONE", email = email, phone = phone, phoneVerified = state.value.phoneVerified))
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
                if (email.isNotBlank()) _state.update { it.copy(profileEmail = email) }
            } catch (_: Exception) {}
        }
    }

    fun consumeOAuthFlow(): OAuthFlow? { val flow = pendingOAuthFlow; pendingOAuthFlow = null; return flow }

    suspend fun hasProfile(): Boolean {
        if (!checkConfigured()) return false
        return try {
            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: SupabaseClientProvider.client.auth.retrieveUserForCurrentSession(updateSession = true)
            profilesRepository.getProfile(user.id) != null
        } catch (_: Exception) { false }
    }

    private fun checkConfigured(): Boolean {
        if (!SupabaseClientProvider.isConfigured()) {
            _state.update { it.copy(errorMessage = "Missing Supabase keys. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties.") }
            return false
        }; return true
    }

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try { block() } catch (e: Exception) {
                val raw = e.message ?: "Something went wrong."
                val friendly = if (raw.contains("Token has expired", ignoreCase = true) || raw.contains("invalid", ignoreCase = true))
                    "Code is invalid or expired. Please request a new one." else raw.lines().firstOrNull()?.take(160) ?: "Something went wrong."
                _state.update { it.copy(errorMessage = friendly) }
            } finally { _state.update { it.copy(isLoading = false) } }
        }
    }

    private fun phoneE164(): String { val digits = state.value.phoneDigits.trim(); if (digits.isBlank()) return ""; return "${state.value.countryCode}$digits" }
    private fun maxDigitsForCountry(countryCode: String): Int = if (countryCode == "+1") 10 else 15
}
