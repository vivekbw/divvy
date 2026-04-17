package com.divvy.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.divvy.divvy.offline.db.entity.CachedExpenseSplitEntity

@Dao
interface CachedExpenseSplitDao {
    @Query("SELECT * FROM cached_expense_splits WHERE expenseId = :expenseId")
    suspend fun getByExpenseId(expenseId: String): List<CachedExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<CachedExpenseSplitEntity>)

    @Query("DELETE FROM cached_expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: String)

    @Query("UPDATE cached_expense_splits SET expenseId = :newId WHERE expenseId = :oldId")
    suspend fun replaceTempExpenseId(oldId: String, newId: String)
}
