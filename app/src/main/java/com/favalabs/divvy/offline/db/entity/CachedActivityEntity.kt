package com.favalabs.divvy.offline.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_activity")
data class CachedActivityEntity(
    @PrimaryKey val id: String,
    val activityType: String,
    val groupId: String,
    val groupName: String,
    val groupIcon: String,
    val title: String,
    val amountCents: Long,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val targetName: String?,
    val createdAt: String,
    val currency: String
)
