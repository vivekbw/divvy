package com.example.divvy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.divvy.ui.assignitems.ViewModels.AssignItemsViewModel
import com.example.divvy.ui.assignitems.Views.AssignItemsScreen
import com.example.divvy.ui.splitpercentage.ViewModels.SplitByPercentageViewModel
import com.example.divvy.ui.splitpercentage.Views.SplitByPercentageScreen
import com.example.divvy.ui.analytics.Views.AnalyticsScreen
import com.example.divvy.ui.groupdetail.Views.GroupDetailScreen
import com.example.divvy.ui.home.Views.HomeScreen
import com.example.divvy.ui.ledger.Views.LedgerScreen
import com.example.divvy.ui.profile.Views.ProfileScreen
import com.example.divvy.ui.scanreceipt.Views.ScanReceiptScreen
import com.example.divvy.ui.splitexpense.Views.SplitExpenseScreen
import com.example.divvy.ui.splitexpense.ViewModels.SplitExpenseViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestination.Home) {
        composable<AppDestination.Home> {
            HomeScreen(
                onGroupClick = { id -> navController.navigate(AppDestination.GroupDetail(id)) },
                onAddExpense = { navController.navigate(AppDestination.SplitExpense()) },
                onScanReceipt = { navController.navigate(AppDestination.ScanReceipt) },
                onProfileClick = { navController.navigate(AppDestination.Profile) },
                onLedgerClick = { navController.navigate(AppDestination.Ledger) },
                onAnalyticsClick = { navController.navigate(AppDestination.Analytics) }
            )
        }
        composable<AppDestination.Analytics> { AnalyticsScreen(onBack = { navController.popBackStack() }) }
        composable<AppDestination.Ledger> { LedgerScreen(onBack = { navController.popBackStack() }) }
        composable<AppDestination.Profile> { ProfileScreen(onBack = { navController.popBackStack() }) }
        composable<AppDestination.GroupDetail> { backStack ->
            val dest: AppDestination.GroupDetail = backStack.toRoute()
            GroupDetailScreen(groupId = dest.groupId, onBack = { navController.popBackStack() },
                onLeaveGroup = { navController.popBackStack(route = AppDestination.Home, inclusive = false) },
                onAddExpense = { navController.navigate(AppDestination.SplitExpense(preselectedGroupId = dest.groupId)) })
        }
        composable<AppDestination.ScanReceipt> {
            ScanReceiptScreen(onBack = { navController.popBackStack() },
                onScanComplete = { amount, description -> navController.popBackStack(); navController.navigate(AppDestination.SplitExpense(scannedAmount = amount, scannedDescription = description)) })
        }
        composable<AppDestination.SplitExpense> { backStack ->
            val dest: AppDestination.SplitExpense = backStack.toRoute()
            val viewModel: SplitExpenseViewModel = koinViewModel(parameters = { parametersOf(dest.scannedAmount, dest.scannedDescription, dest.preselectedGroupId) })
            SplitExpenseScreen(viewModel = viewModel, onBack = { navController.popBackStack() },
                onNavigateToAssignItems = { groupId, amount, description -> navController.navigate(AppDestination.AssignItems(groupId, amount, description)) },
                onNavigateToSplitByPercentage = { groupId, amount, description -> navController.navigate(AppDestination.SplitByPercentage(groupId, amount, description)) })
        }
        composable<AppDestination.SplitByPercentage> { backStack ->
            val dest: AppDestination.SplitByPercentage = backStack.toRoute()
            val viewModel: SplitByPercentageViewModel = koinViewModel(parameters = { parametersOf(dest.groupId, dest.amountDisplay, dest.description) })
            SplitByPercentageScreen(viewModel = viewModel, onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack(route = AppDestination.Home, inclusive = false) })
        }
        composable<AppDestination.AssignItems> { backStack ->
            val dest: AppDestination.AssignItems = backStack.toRoute()
            val viewModel: AssignItemsViewModel = koinViewModel(parameters = { parametersOf(dest.groupId, dest.amountDisplay, dest.description) })
            AssignItemsScreen(viewModel = viewModel, onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack(route = AppDestination.Home, inclusive = false) })
        }
    }
}
