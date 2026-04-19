package com.favalabs.divvy.backend

import com.favalabs.divvy.models.ActivityFeedItem
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getGlobalActivityFeed(): Flow<DataResult<List<ActivityFeedItem>>>
    suspend fun refreshActivityFeed()
}
