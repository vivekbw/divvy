package com.divvy.divvy.offline.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.divvy.divvy.offline.db.entity.CachedMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMemberDao {
    @Query("SELECT * FROM cached_members WHERE groupId = :groupId ORDER BY name ASC")
    fun getByGroupId(groupId: String): Flow<List<CachedMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<CachedMemberEntity>)

    @Query("DELETE FROM cached_members WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)
}
