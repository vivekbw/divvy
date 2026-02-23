package com.example.divvy

import platform.Foundation.NSUUID

actual fun formatDouble(value: Double, decimals: Int): String {
    val factor = pow(10.0, decimals)
    val rounded = kotlin.math.round(value * factor) / factor
    val parts = rounded.toString().split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) parts[1] else ""
    return "$intPart.${decPart.padEnd(decimals, '0').take(decimals)}"
}

private fun pow(base: Double, exp: Int): Double {
    var result = 1.0
    repeat(exp) { result *= base }
    return result
}

actual fun randomUuidString(): String = NSUUID().UUIDString()
