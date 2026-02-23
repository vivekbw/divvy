package com.example.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    val userId: String,
    val name: String
)
