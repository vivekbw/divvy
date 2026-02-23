package com.example.divvy.ui.auth.Views

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.divvy.ui.auth.ViewModels.AuthFlowViewModel
import com.example.divvy.ui.auth.ViewModels.OAuthFlow
import io.github.jan.supabase.gotrue.SessionStatus
import org.koin.compose.viewmodel.koinViewModel

private object AuthRoute { const val LAUNCH = "launch"; const val CREATE_START = "create_start"; const val CREATE_PHONE = "create_phone"; const val VERIFY_PHONE_CREATE = "verify_phone_create"; const val NAME = "name"; const val LOGIN_START = "login_start"; const val LOGIN_PHONE = "login_phone"; const val VERIFY_PHONE_LOGIN = "verify_phone_login" }

@Composable
fun AuthNav(onAuthenticated: () -> Unit) {
    val navController = rememberNavController(); val viewModel: AuthFlowViewModel = koinViewModel(); val sessionStatus by viewModel.sessionStatus.collectAsState()
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route; if (currentRoute == AuthRoute.NAME) return@LaunchedEffect
            val hasProfile = viewModel.hasProfile()
            if (!hasProfile) { navController.navigate(AuthRoute.NAME) { popUpTo(AuthRoute.LAUNCH) { inclusive = false }; launchSingleTop = true }; return@LaunchedEffect }
            when (viewModel.consumeOAuthFlow()) { OAuthFlow.CREATE -> onAuthenticated(); OAuthFlow.LOGIN -> onAuthenticated(); null -> { if (currentRoute == AuthRoute.LAUNCH || currentRoute == AuthRoute.LOGIN_START || currentRoute == AuthRoute.LOGIN_PHONE || currentRoute == AuthRoute.VERIFY_PHONE_LOGIN || currentRoute == AuthRoute.CREATE_PHONE || currentRoute == AuthRoute.VERIFY_PHONE_CREATE) onAuthenticated() } }
        }
    }
    NavHost(navController, AuthRoute.LAUNCH, Modifier.fillMaxSize()) {
        composable(AuthRoute.LAUNCH) { LaunchScreen({ navController.navigate(AuthRoute.CREATE_START) }, { navController.navigate(AuthRoute.LOGIN_START) }) }
        composable(AuthRoute.CREATE_START) { CreateStartScreen({ navController.popBackStack() }, { navController.navigate(AuthRoute.CREATE_PHONE) }, { viewModel.startGoogleSignIn(OAuthFlow.CREATE) }) }
        composable(AuthRoute.CREATE_PHONE) { CreatePhoneScreen(viewModel, { navController.popBackStack() }, { navController.navigate(AuthRoute.VERIFY_PHONE_CREATE) }) }
        composable(AuthRoute.VERIFY_PHONE_CREATE) { VerifyPhoneScreen(viewModel, { navController.popBackStack() }, { navController.navigate(AuthRoute.NAME) }, { navController.popBackStack(AuthRoute.CREATE_PHONE, false) }) }
        composable(AuthRoute.NAME) { NameScreen(viewModel, { navController.popBackStack() }, onAuthenticated) }
        composable(AuthRoute.LOGIN_START) { LoginStartScreen({ navController.popBackStack() }, { navController.navigate(AuthRoute.LOGIN_PHONE) }, { viewModel.startGoogleSignIn(OAuthFlow.LOGIN) }) }
        composable(AuthRoute.LOGIN_PHONE) { LoginPhoneScreen(viewModel, { navController.popBackStack() }, { navController.navigate(AuthRoute.VERIFY_PHONE_LOGIN) }) }
        composable(AuthRoute.VERIFY_PHONE_LOGIN) { VerifyPhoneLoginScreen(viewModel, { navController.popBackStack() }, onAuthenticated, { navController.popBackStack(AuthRoute.LOGIN_PHONE, false) }) }
    }
}
