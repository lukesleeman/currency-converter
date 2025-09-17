package com.lukesleeman.currencyconverter.repository

import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.AVAILABLE_CURRENCIES
import com.lukesleeman.currencyconverter.network.CurrencyApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing currency data and exchange rates.
 * Exchange rates are always fetched and stored relative to EUR.
 */
open class CurrencyRepository(private val api: CurrencyApi) {

    private val _selectedCurrencies = MutableStateFlow(
        listOf(
            AVAILABLE_CURRENCIES.first { it.code == "USD" },
            AVAILABLE_CURRENCIES.first { it.code == "EUR" },
            AVAILABLE_CURRENCIES.first { it.code == "GBP" },
            AVAILABLE_CURRENCIES.first { it.code == "JPY" }
        )
    )
    open val selectedCurrencies: StateFlow<List<Currency>> = _selectedCurrencies.asStateFlow()

    // Rates are stored relative to EUR
    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    open val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    open val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error.asStateFlow()

    private val INTERNAL_API_BASE_CURRENCY = "EUR"

    /**
     * Fetches exchange rates. Rates are always fetched relative to EUR.
     */
    open suspend fun fetchExchangeRates() { // Removed baseCurrency parameter
        _isLoading.value = true
        _error.value = null

        try {
            val response = api.getExchangeRates(INTERNAL_API_BASE_CURRENCY) // Always use EUR
            if (response.isSuccessful) {
                response.body()?.let { exchangeRateResponse ->
                    // Ensure EUR itself has a rate of 1.0 in the map
                    val ratesWithEurBase = exchangeRateResponse.conversionRates.toMutableMap()
                    ratesWithEurBase[INTERNAL_API_BASE_CURRENCY] = 1.0 
                    _exchangeRates.value = ratesWithEurBase
                }
            } else {
                _error.value = "Failed to fetch exchange rates (API base: EUR)"
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred during rate fetch"
        } finally {
            _isLoading.value = false
        }
    }

    open fun addCurrency(currency: Currency) {
        val currentList = _selectedCurrencies.value.toMutableList()
        if (!currentList.contains(currency)) {
            currentList.add(currency)
            _selectedCurrencies.value = currentList
        }
    }

    open fun removeCurrency(currency: Currency) {
        val currentList = _selectedCurrencies.value.toMutableList()
        currentList.remove(currency)
        _selectedCurrencies.value = currentList
    }

    open fun getAvailableCurrencies(): List<Currency> {
        val selected = _selectedCurrencies.value.map { it.code }.toSet()
        return AVAILABLE_CURRENCIES.filter { it.code !in selected }
    }

    /**
     * Calculates converted amount. Assumes _exchangeRates are EUR-based.
     * @param fromCurrencyCode The currency code of the amount.
     * @param toCurrencyCode The target currency code to convert to.
     * @param amount The amount in fromCurrencyCode.
     * @return The converted amount in toCurrencyCode.
     */
    open fun calculateConvertedAmount(
        fromCurrencyCode: String,
        toCurrencyCode: String,
        amount: Double
    ): Double {
        if (fromCurrencyCode == toCurrencyCode) return amount

        val rates = _exchangeRates.value // These are EUR-based rates

        val fromRateToBase = rates[fromCurrencyCode]
        val toRateFromBase = rates[toCurrencyCode]

        if (fromCurrencyCode == INTERNAL_API_BASE_CURRENCY) { // From EUR to Target
            return amount * (toRateFromBase ?: 1.0)
        }

        if (toCurrencyCode == INTERNAL_API_BASE_CURRENCY) { // From Source to EUR
            return if (fromRateToBase != null && fromRateToBase != 0.0) {
                amount / fromRateToBase
            } else {
                amount // Fallback or handle error, here returning original amount
            }
        }
        
        // Convert fromCurrency -> EUR -> toCurrency
        if (fromRateToBase != null && fromRateToBase != 0.0 && toRateFromBase != null) {
            val amountInEur = amount / fromRateToBase
            return amountInEur * toRateFromBase
        }
        
        // Fallback if rates are missing, return original amount or handle error appropriately
        // This could happen if a currency was added but rates haven't updated yet for it.
        return amount 
    }

    open fun clearError() {
        _error.value = null
    }

    /**
     * Provides fallback exchange rates when API is unavailable.
     * These rates are EUR-based (EUR = 1.0) to match the repository's design.
     */
    private fun getDefaultRates(): Map<String, Double> {
        // EUR-based exchange rates (1 EUR equals X units of other currency)
        return mapOf(
            "EUR" to 1.0, "USD" to 1.18, "GBP" to 0.86, "JPY" to 129.4, "CNY" to 7.59,
            "CAD" to 1.47, "AUD" to 1.59, "CHF" to 1.08, "INR" to 87.6, "KRW" to 1388.2,
            "MXN" to 23.7, "BRL" to 6.12, "RUB" to 86.5, "ZAR" to 17.4, "SEK" to 10.13,
            "NOK" to 10.0, "DKK" to 7.53, "SGD" to 1.59, "HKD" to 9.18, "NZD" to 1.67
        )
    }

    init {
        _exchangeRates.value = getDefaultRates()
        // Consider an initial fetch for EUR based rates if desired immediately.
        // kotlinx.coroutines.GlobalScope.launch { fetchExchangeRates() } // Example, requires a CoroutineScope
    }
}