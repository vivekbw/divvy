package com.favalabs.divvy.models

import com.favalabs.divvy.components.GroupIcon
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyBalance(
    val currency: String = "USD",
    val balanceCents: Long = 0L
)

@Serializable
data class Group(
    val id: String,
    val name: String,
    val emoji: String = "",
    val icon: GroupIcon = GroupIcon.Group,
    val memberCount: Int = 0,
    val balances: List<CurrencyBalance> = emptyList(),
    val currency: String = "USD",
    val createdBy: String = ""
) {
    companion object {
        /** The base currency used for the net balance summary card. */
        const val BASE_CURRENCY = "CAD"
    }

    /**
     * Net balance in the base currency only.
     * Used for the summary card color (green = owed, red = owe).
     * Non-base currencies are intentionally excluded — summing across
     * currencies without exchange rates produces meaningless numbers.
     */
    val balanceCents: Long get() =
        balances.firstOrNull { it.currency == BASE_CURRENCY }?.balanceCents ?: 0L

    /** Positive = you are owed, negative = you owe */
    val isOwed: Boolean get() = balanceCents >= 0

    val formattedBalance: String
        get() {
            if (balances.isEmpty()) return formatAmount(0L, BASE_CURRENCY)
            // Base currency first, then others alphabetically
            val nonZero = balances.filter { it.balanceCents != 0L }
            if (nonZero.isEmpty()) return formatAmount(0L, BASE_CURRENCY)
            return nonZero
                .sortedWith(compareBy { if (it.currency == BASE_CURRENCY) 0 else 1 })
                .joinToString(" + ") { formatAmount(it.balanceCents, it.currency) }
        }

    val balanceLabel: String
        get() = if (isOwed) "You are owed ${formattedBalance}" else "You owe ${formattedBalance}"
}