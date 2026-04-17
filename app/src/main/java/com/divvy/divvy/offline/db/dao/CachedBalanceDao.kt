package com.divvy.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.divvy.divvy.offline.db.entity.CachedBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedBalanceDao {
    @Query("SELECT * FROM cached_balances WHERE groupId = :groupId")
    fun getByGroupId(groupId: String): Flow<List<CachedBalanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<CachedBalanceEntity>)

    @Query("DELETE FROM cached_balances WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)
}
