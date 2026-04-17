package com.divvy.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.divvy.divvy.offline.db.entity.CachedGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedGroupDao {
    @Query("SELECT * FROM cached_groups ORDER BY name ASC")
    fun getAll(): Flow<List<CachedGroupEntity>>

    @Query("SELECT * FROM cached_groups WHERE id = :groupId")
    fun getById(groupId: String): Flow<CachedGroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<CachedGroupEntity>)

    @Query("UPDATE cached_groups SET name = :name, icon = :icon WHERE id = :groupId")
    suspend fun updateNameAndIcon(groupId: String, name: String, icon: String)

    @Query("DELETE FROM cached_groups WHERE id = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM cached_groups")
    suspend fun deleteAll()

    @Query("DELETE FROM cached_groups WHERE lastRefreshedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
