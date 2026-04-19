package com.favalabs.divvy.ui.auth.Views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.favalabs.divvy.ui.auth.ViewModels.AuthFlowViewModel
import com.favalabs.divvy.ui.auth.ViewModels.OAuthFlow
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.launch

private object AuthRoute {
    const val LAUNCH               = "launch"
    const val CREATE_START         = "create_start"
    const val CREATE_PHONE         = "create_phone"
    const val VERIFY_PHONE_CREATE  = "verify_phone_create"
    const val NAME                 = "name"
    const val LOGIN_START          = "login_start"
    const val LOGIN_PHONE          = "login_phone"
    const val VERIFY_PHONE_LOGIN   = "verify_phone_login"
}

@Composable
fun AuthNav(
    onAuthenticated: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: AuthFlowViewModel = hiltViewModel()
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == AuthRoute.NAME) return@LaunchedEffect
            val hasProfile = viewModel.hasProfile()
            if (!hasProfile) {
                navController.navigate(AuthRoute.NAME) {
                    popUpTo(AuthRoute.LAUNCH) { inclusive = false }
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }
            when (viewModel.consumeOAuthFlow()) {
                OAuthFlow.CREATE -> onAuthenticated()
                OAuthFlow.LOGIN -> onAuthenticated()
                null -> {
                    if (currentRoute == AuthRoute.LAUNCH ||
                        currentRoute == AuthRoute.LOGIN_START ||
                        currentRoute == AuthRoute.LOGIN_PHONE ||
                        currentRoute == AuthRoute.VERIFY_PHONE_LOGIN ||
                        currentRoute == AuthRoute.CREATE_PHONE ||
                        currentRoute == AuthRoute.VERIFY_PHONE_CREATE
                    ) {
                        onAuthenticated()
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthRoute.LAUNCH,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AuthRoute.LAUNCH) {
            LaunchScreen(
                onCreateAccount = { navController.navigate(AuthRoute.CREATE_START) },
                onLogin = { navController.navigate(AuthRoute.LOGIN_START) }
            )
        }
        composable(AuthRoute.CREATE_START) {
            CreateStartScreen(
                onBack = { navController.popBackStack() },
                onPhone = { navController.navigate(AuthRoute.CREATE_PHONE) },
                onGoogle = { viewModel.startGoogleSignIn(flow = OAuthFlow.CREATE) }
            )
        }
        composable(AuthRoute.CREATE_PHONE) {
            CreatePhoneScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(AuthRoute.VERIFY_PHONE_CREATE) }
            )
        }
        composable(AuthRoute.VERIFY_PHONE_CREATE) {
            VerifyPhoneScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNext = {
                    scope.launch {
                        if (viewModel.hasProfile()) {
                            onAuthenticated()
                        } else {
                            navController.navigate(AuthRoute.NAME)
                        }
                    }
                },
                onChangePhone = { navController.popBackStack(AuthRoute.CREATE_PHONE, inclusive = false) }
            )
        }
        composable(AuthRoute.NAME) {
            NameScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDone = onAuthenticated
            )
        }
        composable(AuthRoute.LOGIN_START) {
            LoginStartScreen(
                onBack = { navController.popBackStack() },
                onPhone = { navController.navigate(AuthRoute.LOGIN_PHONE) },
                onGoogle = { viewModel.startGoogleSignIn(flow = OAuthFlow.LOGIN) }
            )
        }
        composable(AuthRoute.LOGIN_PHONE) {
            LoginPhoneScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(AuthRoute.VERIFY_PHONE_LOGIN) }
            )
        }
        composable(AuthRoute.VERIFY_PHONE_LOGIN) {
            VerifyPhoneLoginScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogin = onAuthenticated,
                onChangePhone = { navController.popBackStack(AuthRoute.LOGIN_PHONE, inclusive = false) }
            )
        }
    }
}
