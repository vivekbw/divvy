package com.divvy.divvy.models

data class FriendActivityItem(
    val id: String,
    val title: String,
    val amountCents: Long,
    val dateLabel: String,
    val paidByCurrentUser: Boolean,
    val timestamp: String,
    val currency: String,
    val groupId: String,
    val groupName: String,
    val groupIcon: String?
)
