package com.lukesleeman.currencyconverter.cache

import com.lukesleeman.currencyconverter.data.ExchangeRateCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeRateFileCacheTest {

    private lateinit var tempDir: File
    private lateinit var cache: ExchangeRateFileCache

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("test_cache").toFile()
        cache = ExchangeRateFileCache(tempDir, Json.Default)
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `saveRates should create single JSON file with correct data`() = runTest {
        // Given: Sample exchange rates
        val rates = mapOf("USD" to 1.18, "GBP" to 0.86, "EUR" to 1.0)
        val timestamp = 1234567890L

        // When: Save rates
        cache.saveRates(rates, timestamp)

        // Then: Single file should be created with complete cache data
        val cacheFile = File(tempDir, "exchange_rates.json")
        assertTrue(cacheFile.exists(), "Cache file should exist")

        // Verify file contains complete cache data
        val cacheJson = cacheFile.readText()
        val savedCache = Json.decodeFromString<ExchangeRateCache>(cacheJson)

        assertEquals(rates, savedCache.rates)
        assertEquals(timestamp, savedCache.timestamp)
        assertEquals("EUR", savedCache.baseCurrency)
    }

    @Test
    fun `loadRates should return default rates when file doesn't exist`() = runTest {
        // When: Load rates with no file
        val result = cache.loadRates()

        // Then: Should return default rates
        assertEquals("EUR", result.baseCurrency)
        assertTrue(result.rates.containsKey("EUR"))
        assertTrue(result.rates.containsKey("USD"))
        assertEquals(1.0, result.rates["EUR"])
    }

    @Test
    fun `loadRates should return cached data when file exists`() = runTest {
        // Given: Saved rates
        val originalRates = mapOf("USD" to 1.20, "JPY" to 130.0, "EUR" to 1.0)
        val originalTimestamp = System.currentTimeMillis()
        cache.saveRates(originalRates, originalTimestamp)

        // When: Load rates
        val result = cache.loadRates()

        // Then: Should return the cached data
        assertEquals(originalRates, result.rates)
        assertEquals(originalTimestamp, result.timestamp)
        assertEquals("EUR", result.baseCurrency)
    }

    @Test
    fun `loadRates should handle corrupted cache file gracefully`() = runTest {
        // Given: Corrupted cache file
        File(tempDir, "exchange_rates.json").writeText("invalid json {")

        // When: Load rates
        val result = cache.loadRates()

        // Then: Should return default rates, not crash
        assertEquals("EUR", result.baseCurrency)
        assertTrue(result.rates.containsKey("EUR"))
        assertTrue(result.rates.containsKey("USD"))
        assertEquals(1.0, result.rates["EUR"])
    }

    @Test
    fun `isExpired should detect expired cache correctly`() = runTest {
        // Given: Rates from 2 hours ago
        val expiredTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
        cache.saveRates(mapOf("USD" to 1.0), expiredTimestamp)

        // When: Load and check expiration
        val result = cache.loadRates()

        // Then: Should be marked as expired
        assertNotNull(result)
        assertTrue(result.isExpired(1), "Cache should be expired after 1 hour")
    }

    @Test
    fun `isExpired should detect fresh cache correctly`() = runTest {
        // Given: Recent rates (30 minutes ago)
        val recentTimestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        cache.saveRates(mapOf("USD" to 1.0), recentTimestamp)

        // When: Load and check expiration
        val result = cache.loadRates()

        // Then: Should not be expired
        assertNotNull(result)
        assertTrue(!result.isExpired(1), "Cache should not be expired within 1 hour")
    }

    @Test
    fun `saveRates should overwrite existing file`() = runTest {
        // Given: Initial rates
        cache.saveRates(mapOf("USD" to 1.10), 1000L)

        // When: Save different rates
        val newRates = mapOf("USD" to 1.25, "GBP" to 0.90)
        cache.saveRates(newRates, 2000L)

        // Then: Should have the new rates
        val result = cache.loadRates()
        assertEquals(newRates, result.rates)
        assertEquals(2000L, result.timestamp)
    }
}