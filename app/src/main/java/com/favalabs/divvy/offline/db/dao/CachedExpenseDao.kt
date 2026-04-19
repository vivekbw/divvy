package com.favalabs.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.favalabs.divvy.offline.db.entity.CachedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedExpenseDao {
    @Query("SELECT * FROM cached_expenses WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getByGroupId(groupId: String): Flow<List<CachedExpenseEntity>>

    @Query("SELECT * FROM cached_expenses ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CachedExpenseEntity>>

    @Query("SELECT * FROM cached_expenses WHERE id = :expenseId")
    suspend fun getById(expenseId: String): CachedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<CachedExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: CachedExpenseEntity)

    @Query("DELETE FROM cached_expenses WHERE groupId = :groupId AND isPending = 0")
    suspend fun deleteByGroupId(groupId: String)

    @Query("DELETE FROM cached_expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)

    @Query("UPDATE cached_expenses SET id = :newId, isPending = 0 WHERE id = :oldId")
    suspend fun replaceTempId(oldId: String, newId: String)
}
