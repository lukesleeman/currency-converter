package com.lukesleeman.currencyconverter.data

import android.annotation.SuppressLint
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.serialization.Serializable

/**
 * Represents user preferences and app state that should persist between sessions
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UserPreferences(
    /**
     * List of currency codes the user has selected to display
     */
    val selectedCurrencyCodes: List<String> = emptyList(),

    /**
     * The currently active/focused currency code
     */
    val activeCurrencyCode: String? = null,

    /**
     * The current input value as a string (what user has typed)
     */
    val currentInputValue: String = "1.00",

    /**
     * Timestamp when preferences were last saved
     */
    val lastSaved: Long = System.currentTimeMillis()
) {
    companion object {

        /**
         * Default preferences for new users
         */
        fun default(): UserPreferences = UserPreferences(
            selectedCurrencyCodes = listOf("USD", "EUR", "GBP", "JPY"),
            activeCurrencyCode = "USD",
            currentInputValue = "1.00"
        )
    }
}