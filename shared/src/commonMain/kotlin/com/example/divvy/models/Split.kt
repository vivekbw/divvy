package com.example.divvy.models

import kotlinx.serialization.Serializable

@Serializable
data class Split(
    val userId: String,
    val amountCents: Long,
    val coveredByUserId: String? = null
)
