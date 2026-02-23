package com.example.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class ActivityItem(
    val id: String,
    val title: String,
    val amountCents: Long,
    val dateLabel: String,       // e.g. "Today", "Yesterday"
    val paidByLabel: String,     // e.g. "You", "Sarah"
    val paidByCurrentUser: Boolean,
    val timestamp: String
)