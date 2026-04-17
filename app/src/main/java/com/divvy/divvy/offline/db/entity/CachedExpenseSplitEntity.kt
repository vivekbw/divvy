package com.divvy.divvy.offline.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_expense_splits")
data class CachedExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val expenseId: String,
    val userId: String,
    val amountCents: Long,
    val isCoveredBy: String? = null
)
