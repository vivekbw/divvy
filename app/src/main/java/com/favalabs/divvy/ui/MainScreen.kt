package com.favalabs.divvy.ui

import androidx.compose.foundation.layout.Column
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.favalabs.divvy.components.OfflineBanner
import com.favalabs.divvy.components.PendingSyncBanner
import com.favalabs.divvy.notifications.RequestNotificationPermission
import com.favalabs.divvy.offline.NetworkMonitor
import com.favalabs.divvy.offline.OfflineSyncManager
import com.favalabs.divvy.ui.navigation.AppDestination
import com.favalabs.divvy.ui.navigation.AppNavHost
import com.favalabs.divvy.ui.navigation.BottomNavigationBar

@Composable
fun MainScreen(networkMonitor: NetworkMonitor, syncManager: OfflineSyncManager) {
    RequestNotificationPermission()
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(navController) {
        val listener = Consumer<Intent> { navController.handleDeepLink(it) }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.hasRoute<AppDestination.Home>() == true ||
            currentDestination?.hasRoute<AppDestination.Groups>() == true ||
            currentDestination?.hasRoute<AppDestination.Friends>() == true ||
            currentDestination?.hasRoute<AppDestination.Profile>() == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OfflineBanner(networkMonitor)
            PendingSyncBanner(syncManager)
            AppNavHost(
                navController = navController,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
