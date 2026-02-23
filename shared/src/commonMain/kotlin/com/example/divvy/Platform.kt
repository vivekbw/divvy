package com.example.divvy

data class DivvyConfig(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val authBypass: Boolean = false
)

expect fun formatDouble(value: Double, decimals: Int = 2): String

fun formatCents(cents: Long): String {
    val absCents = kotlin.math.abs(cents)
    val dollars = absCents / 100
    val remainder = absCents % 100
    return "$${dollars}.${remainder.toString().padStart(2, '0')}"
}

fun formatDollars(value: Double): String = "$${formatDouble(value, 2)}"

expect fun randomUuidString(): String
