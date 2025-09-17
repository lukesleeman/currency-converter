package com.lukesleeman.currencyconverter.repository

import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.AVAILABLE_CURRENCIES
import com.lukesleeman.currencyconverter.network.CurrencyApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing currency data and exchange rates
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

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    open val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    open val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    open val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Fetches exchange rates for the given base currency
     */
    open suspend fun fetchExchangeRates(baseCurrency: String) {
        _isLoading.value = true
        _error.value = null

        try {
            val response = api.getExchangeRates(baseCurrency)
            if (response.isSuccessful) {
                response.body()?.let { exchangeRateResponse ->
                    _exchangeRates.value = exchangeRateResponse.conversionRates
                }
            } else {
                _error.value = "Failed to fetch exchange rates"
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Adds a currency to the selected list
     */
    open fun addCurrency(currency: Currency) {
        val currentList = _selectedCurrencies.value.toMutableList()
        if (!currentList.contains(currency)) {
            currentList.add(currency)
            _selectedCurrencies.value = currentList
        }
    }

    /**
     * Removes a currency from the selected list
     */
    open fun removeCurrency(currency: Currency) {
        val currentList = _selectedCurrencies.value.toMutableList()
        currentList.remove(currency)
        _selectedCurrencies.value = currentList
    }

    /**
     * Gets available currencies that are not currently selected
     */
    open fun getAvailableCurrencies(): List<Currency> {
        val selected = _selectedCurrencies.value.map { it.code }.toSet()
        return AVAILABLE_CURRENCIES.filter { it.code !in selected }
    }

    /**
     * Calculates converted amount for a given base currency and amount
     */
    open fun calculateConvertedAmount(
        baseCurrency: String,
        targetCurrency: String,
        amount: Double
    ): Double {
        val rates = _exchangeRates.value
        return if (baseCurrency == targetCurrency) {
            amount
        } else {
            val rate = rates[targetCurrency] ?: 1.0
            amount * rate
        }
    }

    /**
     * Clears any error state
     */
    open fun clearError() {
        _error.value = null
    }

    /**
     * Provides fallback exchange rates when API is unavailable
     */
    private fun getDefaultRates(): Map<String, Double> {
        return mapOf(
            "USD" to 1.0,
            "EUR" to 0.85,
            "GBP" to 0.73,
            "JPY" to 110.0,
            "CNY" to 6.45,
            "CAD" to 1.25,
            "AUD" to 1.35,
            "CHF" to 0.92,
            "INR" to 74.5,
            "KRW" to 1180.0,
            "MXN" to 20.1,
            "BRL" to 5.2,
            "RUB" to 73.5,
            "ZAR" to 14.8,
            "SEK" to 8.6,
            "NOK" to 8.5,
            "DKK" to 6.4,
            "SGD" to 1.35,
            "HKD" to 7.8,
            "NZD" to 1.42
        )
    }

    /**
     * Initialize with default rates for offline functionality
     */
    init {
        _exchangeRates.value = getDefaultRates()
    }
}