package com.divvy.divvy.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String = "",
    @SerialName("group_id")        val groupId: String = "",
    val merchant: String = "",
    @SerialName("amount_cents")    val amountCents: Long = 0L,
    @SerialName("split_method")    val splitMethod: String = "EQUAL",
    val currency: String = "USD",
    @SerialName("paid_by_user_id") val paidByUserId: String = "",
    @SerialName("created_at")      val createdAt: String = ""
)