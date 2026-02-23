package com.example.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class ActivityItem(
    val id: String,
    val title: String,
    val amountCents: Long,
    val dateLabel: String,
    val paidByLabel: String,
    val paidByCurrentUser: Boolean,
    val timestamp: String
)
