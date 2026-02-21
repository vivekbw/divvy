package com.example.divvy.models

import kotlinx.serialization.Serializable

enum class LedgerEntryType { EXPENSE, SETTLEMENT }

@Serializable
data class LedgerEntry(
    val id: String,
    val type: LedgerEntryType,
    val title: String,
    val amountCents: Long,
    val groupId: String,
    val groupName: String,
    val paidByName: String,
    val paidByCurrentUser: Boolean,
    val dateLabel: String,
    val toName: String = "",
    val splitMethod: String = ""
)
