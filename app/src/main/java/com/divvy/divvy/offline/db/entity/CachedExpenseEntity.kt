package com.divvy.divvy.offline.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_expenses")
data class CachedExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val groupId: String,
    val title: String,
    val amountCents: Long,
    val currency: String,
    val paidByUserId: String,
    val createdAt: String,
    val isPending: Boolean = false
)
