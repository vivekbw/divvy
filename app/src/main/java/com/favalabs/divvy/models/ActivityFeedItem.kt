package com.favalabs.divvy.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityFeedItem(
    val id: String,
    @SerialName("activity_type") val activityType: String, // "EXPENSE", "SETTLEMENT", "MEMBER_JOINED"
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("group_icon") val groupIcon: String,
    val title: String,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("actor_id") val actorId: String,
    @SerialName("actor_name") val actorName: String,
    @SerialName("actor_avatar_url") val actorAvatarUrl: String? = null,
    @SerialName("target_name") val targetName: String? = null,
    @SerialName("created_at") val createdAt: String,
    val currency: String = "USD"
)
