package com.divvy.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class ParsedTransaction(
    val date: String,
    val description: String,
    val amountCents: Long
)

enum class TransactionStatus {
    Pending,
    Added,
    Skipped
}
