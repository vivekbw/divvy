package com.favalabs.divvy.models

enum class SupportedCurrency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$", "US Dollar"),
    CAD("CAD", "CA$", "Canadian Dollar"),
    EUR("EUR", "\u20AC", "Euro"),
    INR("INR", "\u20B9", "Indian Rupee"),
    TTD("TTD", "TT$", "Trinidad & Tobago Dollar");

    companion object {
        fun fromCode(code: String): SupportedCurrency =
            entries.find { it.code == code } ?: USD
    }
}

fun formatAmount(amountCents: Long, currencyCode: String): String {
    val currency = SupportedCurrency.fromCode(currencyCode)
    val dollars = kotlin.math.abs(amountCents) / 100.0
    return "${currency.symbol}${String.format("%.2f", dollars)}"
}
