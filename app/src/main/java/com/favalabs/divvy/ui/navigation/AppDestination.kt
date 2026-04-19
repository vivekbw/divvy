package com.favalabs.divvy.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination {

    @Serializable data object Home      : AppDestination
    @Serializable data object Groups    : AppDestination
    @Serializable data object Friends   : AppDestination
    @Serializable data object Ledger    : AppDestination
    @Serializable data object Analytics : AppDestination
    @Serializable data object Profile   : AppDestination
    @Serializable data object Notifications : AppDestination

    @Serializable
    data class GroupDetail(val groupId: String) : AppDestination

    @Serializable
    data class GroupMembers(val groupId: String) : AppDestination

    @Serializable
    data class FriendDetail(val friendUserId: String) : AppDestination

    @Serializable
    data object ScanReceipt : AppDestination

    @Serializable
    data object ReceiptReview : AppDestination

    @Serializable
    data class SplitExpense(
        val scannedAmount: String = "",
        val scannedDescription: String = "",
        val preselectedGroupId: String = ""
    ) : AppDestination

    @Serializable
    data class AssignItems(
        val groupId: String,
        val amountDisplay: String,
        val description: String,
        val paidByUserId: String,
        val currency: String = "USD"
    ) : AppDestination

    @Serializable
    data class SplitByPercentage(
        val groupId: String,
        val amountDisplay: String,
        val description: String,
        val paidByUserId: String,
        val currency: String = "USD"
    ) : AppDestination

    @Serializable
    data object StatementUpload : AppDestination

    @Serializable
    data object TransactionReview : AppDestination

    @Serializable
    data class JoinGroup(val groupId: String, val groupName: String = "") : AppDestination
}