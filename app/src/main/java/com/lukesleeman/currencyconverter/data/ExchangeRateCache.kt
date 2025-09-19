package com.lukesleeman.currencyconverter.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

/**
 * Represents cached exchange rate data with metadata in a single JSON structure
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExchangeRateCache(
    val rates: Map<String, Double>,
    val timestamp: Long,
    val baseCurrency: String = "EUR"
) {
    /**
     * Check if the cache is expired (older than 1 hour)
     */
    fun isExpired(maxAgeHours: Int = 1): Boolean {
        val maxAgeMillis = TimeUnit.HOURS.toMillis(maxAgeHours.toLong())
        return System.currentTimeMillis() - timestamp > maxAgeMillis
    }
}