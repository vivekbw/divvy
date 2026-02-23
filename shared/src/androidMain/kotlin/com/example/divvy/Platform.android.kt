package com.example.divvy

import java.util.UUID

actual fun formatDouble(value: Double, decimals: Int): String {
    return String.format("%.${decimals}f", value)
}

actual fun randomUuidString(): String = UUID.randomUUID().toString()
