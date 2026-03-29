package com.example.divvy.offline.repository

import com.example.divvy.backend.ActivityRepository
import com.example.divvy.backend.DataResult
import com.example.divvy.backend.SupabaseActivityRepository
import com.example.divvy.models.ActivityFeedItem
import com.example.divvy.offline.NetworkMonitor
import com.example.divvy.offline.db.dao.CachedActivityDao
import com.example.divvy.offline.db.entity.CachedActivityEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineActivityRepository @Inject constructor(
    private val remote: SupabaseActivityRepository,
    private val activityDao: CachedActivityDao,
    private val networkMonitor: NetworkMonitor
) : ActivityRepository {

    override fun getGlobalActivityFeed(): Flow<DataResult<List<ActivityFeedItem>>> =
        activityDao.getAll().map { entities ->
            DataResult.Success(entities.map { it.toActivityFeedItem() })
        }

    override suspend fun refreshActivityFeed() {
        if (!networkMonitor.isOnline.value) return
        try {
            remote.refreshActivityFeed()
            // Get the latest success result from remote and cache it
            val result = remote.getGlobalActivityFeed().first { it is DataResult.Success }
            if (result is DataResult.Success) {
                activityDao.deleteAll()
                activityDao.insertAll(result.data.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh activity feed")
        }
    }

    private fun CachedActivityEntity.toActivityFeedItem(): ActivityFeedItem = ActivityFeedItem(
        id = id,
        activityType = activityType,
        groupId = groupId,
        groupName = groupName,
        groupIcon = groupIcon,
        title = title,
        amountCents = amountCents,
        actorId = actorId,
        actorName = actorName,
        actorAvatarUrl = actorAvatarUrl,
        targetName = targetName,
        createdAt = createdAt,
        currency = currency
    )

    private fun ActivityFeedItem.toEntity(): CachedActivityEntity = CachedActivityEntity(
        id = id,
        activityType = activityType,
        groupId = groupId,
        groupName = groupName,
        groupIcon = groupIcon,
        title = title,
        amountCents = amountCents,
        actorId = actorId,
        actorName = actorName,
        actorAvatarUrl = actorAvatarUrl,
        targetName = targetName,
        createdAt = createdAt,
        currency = currency
    )
}
