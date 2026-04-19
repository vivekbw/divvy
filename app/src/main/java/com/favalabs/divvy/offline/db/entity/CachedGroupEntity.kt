package com.favalabs.divvy.offline.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_groups")
data class CachedGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val createdBy: String,
    val memberCount: Int,
    val currency: String,
    val balancesJson: String,
    val lastRefreshedAt: Long = System.currentTimeMillis()
)
