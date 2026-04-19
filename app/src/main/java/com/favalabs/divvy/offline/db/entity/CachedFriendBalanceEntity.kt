package com.favalabs.divvy.offline.db.entity

import androidx.room.Entity

@Entity(tableName = "cached_friend_balances", primaryKeys = ["userId", "groupId", "currency"])
data class CachedFriendBalanceEntity(
    val userId: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val groupId: String,
    val groupName: String,
    val groupIcon: String?,
    val currency: String,
    val balanceCents: Long
)
