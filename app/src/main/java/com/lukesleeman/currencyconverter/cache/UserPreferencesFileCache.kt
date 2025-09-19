package com.lukesleeman.currencyconverter.cache

import com.lukesleeman.currencyconverter.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-based cache for user preferences using JSON storage
 */
class UserPreferencesFileCache(
    private val cacheDir: File,
    private val json: Json = Json.Default
) {
    private val preferencesFile = File(cacheDir, "user_preferences.json")

    /**
     * Save user preferences to cache file
     */
    suspend fun savePreferences(preferences: UserPreferences) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure cache directory exists
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                // Save preferences with updated timestamp
                val updatedPreferences = preferences.copy(lastSaved = System.currentTimeMillis())
                val preferencesJson = json.encodeToString(UserPreferences.serializer(), updatedPreferences)
                preferencesFile.writeText(preferencesJson)

            } catch (e: Exception) {
                // If saving fails, clean up partial file to prevent corruption
                preferencesFile.takeIf { it.exists() }?.delete()
                throw e
            }
        }
    }

    /**
     * Load user preferences from cache file
     * Returns cached preferences if file exists and is valid
     * Returns default preferences if no cache file exists or if cache is corrupted
     */
    suspend fun loadPreferences(): UserPreferences {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (preferencesFile.exists()) {
                    val preferencesJson = preferencesFile.readText()
                    json.decodeFromString<UserPreferences>(preferencesJson)
                } else null
            }.getOrNull() ?: UserPreferences.default()
        }
    }

    /**
     * Check if preferences file exists
     */
    suspend fun preferencesExist(): Boolean {
        return withContext(Dispatchers.IO) {
            preferencesFile.exists()
        }
    }

    /**
     * Clear all saved preferences (useful for testing or reset functionality)
     */
    suspend fun clearPreferences() {
        withContext(Dispatchers.IO) {
            preferencesFile.takeIf { it.exists() }?.delete()
        }
    }
}