package com.lukesleeman.currencyconverter.ui.state

import androidx.compose.ui.text.input.TextFieldValue
import com.lukesleeman.currencyconverter.data.Currency

/**
 * Represents the complete UI state for the Currency Converter screen.
 */
data class CurrencyConverterUiState(
    val currencies: List<CurrencyDisplayItem>,
    val activeCurrency: CurrencyDisplayItem,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Represents a single currency row in the UI.
 */
data class CurrencyDisplayItem(
    val currency: Currency,
    val textFieldValue: TextFieldValue
)