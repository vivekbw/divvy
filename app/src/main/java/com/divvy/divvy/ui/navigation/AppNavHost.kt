package com.divvy.divvy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.divvy.divvy.ui.assignitems.ViewModels.AssignItemsViewModel
import com.divvy.divvy.ui.assignitems.Views.AssignItemsScreen
import com.divvy.divvy.ui.splitpercentage.ViewModels.SplitByPercentageViewModel
import com.divvy.divvy.ui.splitpercentage.Views.SplitByPercentageScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.divvy.divvy.ui.frienddetail.FriendDetailScreen
import com.divvy.divvy.ui.friends.FriendsScreen
import com.divvy.divvy.ui.groups.Views.GroupsScreen
import com.divvy.divvy.ui.home.Views.HomeScreen
import com.divvy.divvy.ui.analytics.Views.AnalyticsScreen
import com.divvy.divvy.ui.groupdetail.Views.GroupDetailScreen
import com.divvy.divvy.ui.groupmembers.Views.GroupMembersScreen
import com.divvy.divvy.ui.ledger.Views.LedgerScreen
import com.divvy.divvy.ui.profile.Views.ProfileScreen
import com.divvy.divvy.ui.notifications.Views.NotificationsScreen
import com.divvy.divvy.ui.receiptreview.Views.ReceiptReviewScreen
import com.divvy.divvy.ui.scanreceipt.Views.ScanReceiptScreen
import com.divvy.divvy.ui.splitexpense.Views.SplitExpenseScreen
import com.divvy.divvy.ui.statementimport.Views.StatementUploadScreen
import com.divvy.divvy.ui.statementimport.Views.TransactionReviewScreen
import com.divvy.divvy.ui.statementimport.ViewModels.TransactionReviewViewModel
import androidx.navigation.navDeepLink
import com.divvy.divvy.ui.joingroup.Views.JoinGroupScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home,
        modifier = modifier
    ) {
        composable<AppDestination.Home> {
            HomeScreen(
                onGroupClick = { id -> navController.navigate(AppDestination.GroupDetail(id)) },
                onGroupsClick = {
                    navController.navigate(AppDestination.Groups) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddExpense = { navController.navigate(AppDestination.SplitExpense()) },
                onLedgerClick = { navController.navigate(AppDestination.Ledger) },
                onImportStatement = { navController.navigate(AppDestination.StatementUpload) },
                onNotificationsClick = { navController.navigate(AppDestination.Notifications) }
            )
        }
        composable<AppDestination.Groups> {
            GroupsScreen(
                onGroupClick = { id -> navController.navigate(AppDestination.GroupDetail(id)) },
                onCreatedGroupNavigate = { id ->
                    navController.navigate(AppDestination.GroupDetail(id))
                }
            )
        }
        composable<AppDestination.Friends> {
            FriendsScreen(
                onFriendClick = { friendUserId ->
                    navController.navigate(AppDestination.FriendDetail(friendUserId))
                },
                onCreatedGroupNavigate = { id ->
                    navController.navigate(AppDestination.GroupDetail(id))
                },
                onAddExpenseNavigate = { groupId ->
                    navController.navigate(AppDestination.SplitExpense(preselectedGroupId = groupId))
                }
            )
        }
        composable<AppDestination.Analytics> {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<AppDestination.Ledger> {
            LedgerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<AppDestination.Profile> {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<AppDestination.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<AppDestination.FriendDetail> { backStack ->
            val dest: AppDestination.FriendDetail = backStack.toRoute()
            FriendDetailScreen(
                friendUserId = dest.friendUserId,
                onBack = { navController.popBackStack() },
                onAddExpenseNavigate = { groupId ->
                    navController.navigate(AppDestination.SplitExpense(preselectedGroupId = groupId))
                }
            )
        }
        composable<AppDestination.GroupDetail> { backStack ->
            val dest: AppDestination.GroupDetail = backStack.toRoute()
            GroupDetailScreen(
                groupId = dest.groupId,
                onBack = { navController.popBackStack() },
                onLeaveGroup = {
                    navController.popBackStack(
                        route = AppDestination.Home,
                        inclusive = false
                    )
                },
                onAddExpense = {
                    navController.navigate(AppDestination.SplitExpense(preselectedGroupId = dest.groupId))
                },
                onViewMembers = {
                    navController.navigate(AppDestination.GroupMembers(dest.groupId))
                }
            )
        }
        composable<AppDestination.GroupMembers> { backStack ->
            val dest: AppDestination.GroupMembers = backStack.toRoute()
            GroupMembersScreen(
                groupId = dest.groupId,
                onBack = { navController.popBackStack() },
                onLeaveGroup = {
                    navController.popBackStack(
                        route = AppDestination.Home,
                        inclusive = false
                    )
                }
            )
        }
        composable<AppDestination.ScanReceipt> {
            ScanReceiptScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReview = {
                    navController.navigate(AppDestination.ReceiptReview)
                }
            )
        }
        composable<AppDestination.ReceiptReview> {
            ReceiptReviewScreen(
                onBack = { navController.popBackStack() },
                onContinue = { amount, description ->
                    navController.navigate(
                        AppDestination.SplitExpense(
                            scannedAmount = amount,
                            scannedDescription = description
                        )
                    ) {
                        popUpTo<AppDestination.ScanReceipt> { inclusive = true }
                    }
                }
            )
        }
        composable<AppDestination.SplitExpense> {
            SplitExpenseScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAssignItems = { groupId, amount, description, paidByUserId, currency ->
                    navController.navigate(
                        AppDestination.AssignItems(groupId, amount, description, paidByUserId, currency)
                    )
                },
                onNavigateToSplitByPercentage = { groupId, amount, description, paidByUserId, currency ->
                    navController.navigate(
                        AppDestination.SplitByPercentage(groupId, amount, description, paidByUserId, currency)
                    )
                }
            )
        }
        composable<AppDestination.SplitByPercentage> { backStack ->
            val dest: AppDestination.SplitByPercentage = backStack.toRoute()
            val viewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel<
                    SplitByPercentageViewModel, SplitByPercentageViewModel.Factory
                    >(
                creationCallback = { factory ->
                    factory.create(dest.groupId, dest.amountDisplay, dest.description, dest.paidByUserId, dest.currency)
                }
            )
            SplitByPercentageScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.popBackStack() // pop SplitByPercentage
                    navController.popBackStack() // pop SplitExpense → land on GroupDetail
                }
            )
        }
        composable<AppDestination.AssignItems> { backStack ->
            val dest: AppDestination.AssignItems = backStack.toRoute()
            val viewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel<
                    AssignItemsViewModel, AssignItemsViewModel.Factory
                    >(
                creationCallback = { factory ->
                    factory.create(dest.groupId, dest.amountDisplay, dest.description, dest.paidByUserId, dest.currency)
                }
            )
            AssignItemsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.popBackStack() // pop AssignItems
                    navController.popBackStack() // pop SplitExpense → land on GroupDetail
                }
            )
        }
        composable<AppDestination.StatementUpload> {
            StatementUploadScreen(
                onBack = { navController.popBackStack() },
                onTransactionsParsed = {
                    navController.navigate(AppDestination.TransactionReview) {
                        popUpTo(AppDestination.StatementUpload) { inclusive = true }
                    }
                }
            )
        }
        composable<AppDestination.TransactionReview> {
            val viewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel<
                    TransactionReviewViewModel
                    >()
            TransactionReviewScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack(
                        route = AppDestination.Home,
                        inclusive = false
                    )
                },
                onAddAsExpense = { amountCents, description ->
                    val amountStr = String.format("%.2f", amountCents / 100.0)
                    navController.navigate(
                        AppDestination.SplitExpense(
                            scannedAmount = amountStr,
                            scannedDescription = description
                        )
                    )
                },
                onDone = {
                    navController.popBackStack(
                        route = AppDestination.Home,
                        inclusive = false
                    )
                }
            )
        }
        composable<AppDestination.JoinGroup>(
            deepLinks = listOf(navDeepLink<AppDestination.JoinGroup>(basePath = "divvy://join"))
        ) { backStack ->
            val dest: AppDestination.JoinGroup = backStack.toRoute()
            JoinGroupScreen(
                groupId = dest.groupId,
                groupName = dest.groupName,
                onBack = { navController.popBackStack() },
                onJoined = { id ->
                    navController.navigate(AppDestination.GroupDetail(id)) {
                        popUpTo(AppDestination.Home) { inclusive = false }
                    }
                }
            )
        }
    }
}