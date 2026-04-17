package com.divvy.divvy.backend

import com.divvy.divvy.models.ActivityFeedItem
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getGlobalActivityFeed(): Flow<DataResult<List<ActivityFeedItem>>>
    suspend fun refreshActivityFeed()
}
