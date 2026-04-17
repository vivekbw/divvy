package com.divvy.divvy.backend

import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface ForexRepository {
    /** Returns the exchange rate to convert 1 unit of [from] into [to]. */
    suspend fun getRate(from: String, to: String): Double?
}

@Singleton
class FrankfurterForexRepository @Inject constructor() : ForexRepository {

    @Serializable
    private data class FrankfurterResponse(
        val base: String = "",
        val rates: Map<String, Double> = emptyMap()
    )

    // Cache rates for the session keyed by base currency
    private val cache = mutableMapOf<String, Map<String, Double>>()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getRate(from: String, to: String): Double? {
        if (from == to) return 1.0
        val rates = getRates(from) ?: return null
        return rates[to]
    }

    private suspend fun getRates(base: String): Map<String, Double>? {
        cache[base]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.frankfurter.app/latest?base=$base")
                val text = url.readText()
                val response = json.decodeFromString<FrankfurterResponse>(text)
                val rates = response.rates + (base to 1.0)
                cache[base] = rates
                rates
            } catch (e: Exception) {
                // A null return causes callers to use the hardcoded fallback rate table.
                // Capture so we know if Frankfurter is down or the base currency is unsupported.
                Sentry.captureException(e)
                null
            }
        }
    }
}