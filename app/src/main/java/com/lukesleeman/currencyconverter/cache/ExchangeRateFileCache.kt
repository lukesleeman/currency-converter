package com.lukesleeman.currencyconverter.cache

import com.lukesleeman.currencyconverter.data.DefaultRates
import com.lukesleeman.currencyconverter.data.ExchangeRateCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-based cache for exchange rates using single JSON file storage
 */
class ExchangeRateFileCache(
    private val cacheDir: File,
    private val json: Json = Json.Default
) {
    private val cacheFile = File(cacheDir, "exchange_rates.json")

    /**
     * Save exchange rates to cache file
     */
    suspend fun saveRates(rates: Map<String, Double>, timestamp: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure cache directory exists
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                // Create complete cache object
                val cache = ExchangeRateCache(
                    rates = rates,
                    timestamp = timestamp,
                    baseCurrency = "EUR",
                )

                // Save as single JSON file
                val cacheJson = json.encodeToString(ExchangeRateCache.serializer(), cache)
                cacheFile.writeText(cacheJson)

            } catch (e: Exception) {
                // If saving fails, clean up partial file to prevent corruption
                cacheFile.takeIf { it.exists() }?.delete()
                throw e
            }
        }
    }

    /**
     * Load exchange rates from cache file
     * Returns cached data if file exists and is valid
     * Returns default rates if no cache file exists or if cache is corrupted
     */
    suspend fun loadRates(): ExchangeRateCache {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (cacheFile.exists()) {
                    val cacheJson = cacheFile.readText()
                    json.decodeFromString<ExchangeRateCache>(cacheJson)
                } else null
            }.getOrNull() ?: createDefaultCache()
        }
    }

    private fun createDefaultCache() = ExchangeRateCache(
        rates = DefaultRates.getDefaultRates(),
        timestamp = System.currentTimeMillis(),
        baseCurrency = "EUR"
    )
}