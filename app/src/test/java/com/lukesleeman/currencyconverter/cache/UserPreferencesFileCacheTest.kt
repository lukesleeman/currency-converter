package com.lukesleeman.currencyconverter.cache

import com.lukesleeman.currencyconverter.data.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for UserPreferencesFileCache
 */
class UserPreferencesFileCacheTest {

    private lateinit var testCacheDir: File
    private lateinit var cache: UserPreferencesFileCache

    @Before
    fun setup() {
        // Create a temporary directory for testing
        testCacheDir = File.createTempFile("test_cache", "").apply {
            delete()
            mkdirs()
        }
        cache = UserPreferencesFileCache(testCacheDir)
    }

    @After
    fun cleanup() {
        // Clean up test directory
        testCacheDir.deleteRecursively()
    }

    @Test
    fun `loadPreferences should return default when no file exists`() = runTest {
        val preferences = cache.loadPreferences()

        assertEquals(UserPreferences.default().selectedCurrencyCodes, preferences.selectedCurrencyCodes)
        assertEquals(UserPreferences.default().activeCurrencyCode, preferences.activeCurrencyCode)
        assertEquals(UserPreferences.default().currentInputValue, preferences.currentInputValue)
    }

    @Test
    fun `preferencesExist should return false when no file exists`() = runTest {
        val exists = cache.preferencesExist()

        assertFalse(exists)
    }

    @Test
    fun `savePreferences should create file and allow loading`() = runTest {
        val testPreferences = UserPreferences(
            selectedCurrencyCodes = listOf("USD", "EUR", "JPY"),
            activeCurrencyCode = "EUR",
            currentInputValue = "42.50"
        )

        cache.savePreferences(testPreferences)

        assertTrue(cache.preferencesExist())

        val loadedPreferences = cache.loadPreferences()

        assertEquals(testPreferences.selectedCurrencyCodes, loadedPreferences.selectedCurrencyCodes)
        assertEquals(testPreferences.activeCurrencyCode, loadedPreferences.activeCurrencyCode)
        assertEquals(testPreferences.currentInputValue, loadedPreferences.currentInputValue)
    }

    @Test
    fun `savePreferences should update timestamp`() = runTest {
        val originalPreferences = UserPreferences(
            selectedCurrencyCodes = listOf("USD"),
            lastSaved = 1000L
        )

        cache.savePreferences(originalPreferences)

        val loadedPreferences = cache.loadPreferences()

        assertTrue(loadedPreferences.lastSaved > originalPreferences.lastSaved)
    }

    @Test
    fun `clearPreferences should remove file`() = runTest {
        val testPreferences = UserPreferences(
            selectedCurrencyCodes = listOf("USD")
        )

        cache.savePreferences(testPreferences)
        assertTrue(cache.preferencesExist())

        cache.clearPreferences()
        assertFalse(cache.preferencesExist())

        // Loading after clear should return defaults
        val preferences = cache.loadPreferences()
        assertEquals(UserPreferences.default().selectedCurrencyCodes, preferences.selectedCurrencyCodes)
    }

    @Test
    fun `should handle corrupted file gracefully`() = runTest {
        // Create a corrupted file
        val preferencesFile = File(testCacheDir, "user_preferences.json")
        preferencesFile.writeText("{ invalid json content }")

        // Should return defaults when file is corrupted
        val preferences = cache.loadPreferences()
        assertEquals(UserPreferences.default().selectedCurrencyCodes, preferences.selectedCurrencyCodes)
    }

    @Test
    fun `should create cache directory if it doesnt exist`() = runTest {
        // Delete the cache directory
        testCacheDir.deleteRecursively()
        assertFalse(testCacheDir.exists())

        // Saving should recreate the directory
        val testPreferences = UserPreferences(selectedCurrencyCodes = listOf("USD"))
        cache.savePreferences(testPreferences)

        assertTrue(testCacheDir.exists())
        assertTrue(cache.preferencesExist())
    }
}