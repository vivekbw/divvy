package com.divvy.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.divvy.divvy.offline.db.entity.CachedFriendBalanceEntity

@Dao
interface CachedFriendBalanceDao {
    @Query("SELECT * FROM cached_friend_balances")
    suspend fun getAll(): List<CachedFriendBalanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CachedFriendBalanceEntity>)

    @Query("DELETE FROM cached_friend_balances")
    suspend fun deleteAll()
}
