package com.example.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class MemberBalance(
    val userId: String,
    val name: String,
    val balanceCents: Long
)
