package com.favalabs.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class MemberBalance(
    val userId: String,
    val name: String,
    val balanceCents: Long,   // positive = they owe you, negative = you owe them
    val currency: String = "USD"
)
