package com.favalabs.divvy.ui.auth.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.favalabs.divvy.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.favalabs.divvy.components.AuthTextField
import com.favalabs.divvy.ui.auth.ViewModels.AuthFlowViewModel

@Composable
fun LaunchScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_divvy_transparent),
                contentDescription = "Divvy Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Divvy",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Split bills\nthe easy way",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stop chasing. Start settling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .clickable(onClick = onCreateAccount),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Create an account",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account? ",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Log in",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        }
    }
}

@Composable
fun CreateStartScreen(
    onBack: () -> Unit,
    onPhone: () -> Unit,
    onGoogle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Create new account", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\uD83D\uDC4B",
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Let's get started!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Begin with creating a new free account.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(28.dp))
        AuthPrimaryButton(
            label = "Continue with phone",
            modifier = Modifier.fillMaxWidth(),
            onClick = onPhone
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "or",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthOutlinedButton(
            label = "Continue with Google",
            modifier = Modifier.fillMaxWidth(),
            onClick = onGoogle
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "By using Divvy, you agree to the",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Terms and Privacy Policy.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun CreatePhoneScreen(
    viewModel: AuthFlowViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Add your phone 1/3", onBack = onBack)
        StepIndicator(step = 1)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u260E\uFE0F",
            fontSize = 40.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We'll text you a 6-digit code to verify.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PhoneNumberField(
            countryCode = state.countryCode,
            countryFlag = state.countryFlag,
            phoneDigits = state.phoneDigits,
            onCountryChange = viewModel::updateCountryOption,
            onPhoneDigitsChange = viewModel::updatePhoneDigits,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthPrimaryButton(
            label = if (state.isLoading) "Sending..." else "Send code",
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            onClick = { viewModel.sendPhoneOtp(createUser = true, onSuccess = onNext) }
        )
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "By using Divvy, you agree to the",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Terms and Privacy Policy.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun VerifyPhoneScreen(
    viewModel: AuthFlowViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onChangePhone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Verify your phone 2/3", onBack = onBack)
        StepIndicator(step = 2)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\uD83D\uDD10",
            fontSize = 40.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We just sent a 6-digit code to\n${formatPhoneNumber(state.countryCode, state.phoneDigits)}",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        OtpInputRow(value = state.otp, onValueChange = viewModel::updateOtp)
        Spacer(modifier = Modifier.height(16.dp))
        AuthPrimaryButton(
            label = if (state.isLoading) "Verifying..." else "Verify phone",
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            onClick = { viewModel.verifyPhoneOtp(onSuccess = onNext) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Wrong number? Send to different phone",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable { onChangePhone() }
        )
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "By using Divvy, you agree to the",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Terms and Privacy Policy.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun NameScreen(
    viewModel: AuthFlowViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.prefillProfileEmail()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Tell us about you", onBack = onBack)
        StepIndicator(step = 3)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "\uD83D\uDE04",
            fontSize = 40.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Add your info so friends can find you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PhoneNumberField(
            countryCode = state.countryCode,
            countryFlag = state.countryFlag,
            phoneDigits = state.phoneDigits,
            onCountryChange = viewModel::updateCountryOption,
            onPhoneDigitsChange = viewModel::updatePhoneDigits,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        AuthTextField(
            value = state.profileEmail,
            onValueChange = viewModel::updateProfileEmail,
            placeholder = "Email",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(12.dp))
        AuthTextField(
            value = state.firstName,
            onValueChange = viewModel::updateFirstName,
            placeholder = "First name",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        AuthTextField(
            value = state.lastName,
            onValueChange = viewModel::updateLastName,
            placeholder = "Last name",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(18.dp))
        AuthPrimaryButton(
            label = if (state.isLoading) "Saving..." else "Continue",
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            onClick = { viewModel.saveProfile(onSuccess = onDone) }
        )
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun LoginStartScreen(
    onBack: () -> Unit,
    onPhone: () -> Unit,
    onGoogle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Log into account", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\uD83D\uDC4B",
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Welcome back!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "It's time to divvy up",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(28.dp))
        AuthPrimaryButton(
            label = "Continue with phone",
            modifier = Modifier.fillMaxWidth(),
            onClick = onPhone
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "or",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(14.dp))
        AuthOutlinedButton(
            label = "Continue with Google",
            modifier = Modifier.fillMaxWidth(),
            onClick = onGoogle
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "By using Divvy, you agree to the",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Terms and Privacy Policy.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun LoginPhoneScreen(
    viewModel: AuthFlowViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Log into account", onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enter your phone number to continue.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PhoneNumberField(
            countryCode = state.countryCode,
            countryFlag = state.countryFlag,
            phoneDigits = state.phoneDigits,
            onCountryChange = viewModel::updateCountryOption,
            onPhoneDigitsChange = viewModel::updatePhoneDigits,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthPrimaryButton(
            label = if (state.isLoading) "Sending..." else "Send code",
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            onClick = { viewModel.sendPhoneOtp(createUser = false, onSuccess = onNext) }
        )
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun VerifyPhoneLoginScreen(
    viewModel: AuthFlowViewModel,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onChangePhone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthTopBar(title = "Enter verification code", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\uD83D\uDD10",
            fontSize = 40.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We just sent a 6-digit code to\n${formatPhoneNumber(state.countryCode, state.phoneDigits)}",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        OtpInputRow(value = state.otp, onValueChange = viewModel::updateOtp)
        Spacer(modifier = Modifier.height(16.dp))
        AuthPrimaryButton(
            label = if (state.isLoading) "Verifying..." else "Verify & log in",
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            onClick = { viewModel.verifyPhoneOtp(onSuccess = onLogin) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Wrong number? Send to different phone",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable { onChangePhone() }
        )
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
