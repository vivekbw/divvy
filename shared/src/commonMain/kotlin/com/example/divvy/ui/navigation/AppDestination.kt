package com.example.divvy.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination {
    @Serializable data object Home : AppDestination
    @Serializable data object Ledger : AppDestination
    @Serializable data object Analytics : AppDestination
    @Serializable data object Profile : AppDestination
    @Serializable data class GroupDetail(val groupId: String) : AppDestination
    @Serializable data object ScanReceipt : AppDestination
    @Serializable data class SplitExpense(val scannedAmount: String = "", val scannedDescription: String = "", val preselectedGroupId: String = "") : AppDestination
    @Serializable data class AssignItems(val groupId: String, val amountDisplay: String, val description: String) : AppDestination
    @Serializable data class SplitByPercentage(val groupId: String, val amountDisplay: String, val description: String) : AppDestination
}
