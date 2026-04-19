package com.favalabs.divvy.offline.db.entity

import androidx.room.Entity

@Entity(tableName = "cached_balances", primaryKeys = ["groupId", "userId", "currency"])
data class CachedBalanceEntity(
    val groupId: String,
    val userId: String,
    val name: String,
    val balanceCents: Long,
    val currency: String
)
