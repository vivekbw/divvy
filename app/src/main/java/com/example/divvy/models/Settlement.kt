package com.example.divvy.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Settlement(
    @SerialName("group_id")     val groupId: String,
    @SerialName("payer_id")     val payerId: String,
    @SerialName("payee_id")     val payeeId: String,
    @SerialName("amount_cents") val amountCents: Long
)
