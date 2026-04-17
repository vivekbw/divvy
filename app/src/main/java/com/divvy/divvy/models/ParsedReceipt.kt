package com.divvy.divvy.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ReceiptItemRow(
    @SerialName("expense_id") val expenseId: String,
    val description: String,
    @SerialName("price_cents") val priceCents: Long,
    @EncodeDefault @SerialName("assigned_user_id") val assignedUserId: String? = null
)

@Serializable
data class ParsedReceiptItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val priceCents: Long
) {
    val formattedPrice: String
        get() = "$${String.format("%.2f", priceCents / 100.0)}"
}

@Serializable
data class ParsedReceipt(
    val imageUri: String? = null,
    val merchant: String = "",
    val items: List<ParsedReceiptItem> = emptyList(),
    val subtotalCents: Long = 0L,
    val taxCents: Long = 0L,
    val tipCents: Long = 0L,
    val discountCents: Long = 0L,
    val totalCents: Long = 0L
) {
    val itemsTotalCents: Long
        get() = items.sumOf { it.priceCents }

    val effectiveTotalCents: Long
        get() = if (totalCents > 0) totalCents
                else itemsTotalCents + taxCents + tipCents - discountCents

    val formattedTotal: String
        get() = "$${String.format("%.2f", effectiveTotalCents / 100.0)}"
}
