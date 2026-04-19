package com.favalabs.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.favalabs.divvy.offline.db.entity.CachedActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedActivityDao {
    @Query("SELECT * FROM cached_activity ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CachedActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<CachedActivityEntity>)

    @Query("DELETE FROM cached_activity")
    suspend fun deleteAll()
}
