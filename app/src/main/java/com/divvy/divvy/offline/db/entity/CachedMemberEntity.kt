package com.divvy.divvy.offline.db.entity

import androidx.room.Entity

@Entity(tableName = "cached_members", primaryKeys = ["groupId", "userId"])
data class CachedMemberEntity(
    val groupId: String,
    val userId: String,
    val name: String
)
