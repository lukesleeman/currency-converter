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
class CurrencyRepository(private val api: CurrencyApi) {

    private val _selectedCurrencies = MutableStateFlow(
        listOf(
            AVAILABLE_CURRENCIES.first { it.code == "USD" },
            AVAILABLE_CURRENCIES.first { it.code == "EUR" },
            AVAILABLE_CURRENCIES.first { it.code == "GBP" },
            AVAILABLE_CURRENCIES.first { it.code == "JPY" }
        )
    )
    val selectedCurrencies: StateFlow<List<Currency>> = _selectedCurrencies.asStateFlow()

    // Rates are stored relative to EUR
    private var exchangeRates: Map<String, Double> = emptyMap()

    private companion object {
        const val INTERNAL_API_BASE_CURRENCY = "EUR"
    }

    /**
     * Fetches exchange rates and returns a Result for better error handling
     */
    suspend fun fetchExchangeRates(): Result<Unit> {
        return try {
            val response = api.getExchangeRates(INTERNAL_API_BASE_CURRENCY)
            if (response.isSuccessful) {
                response.body()?.let { exchangeRateResponse ->
                    // Ensure EUR itself has a rate of 1.0 in the map
                    val ratesWithEurBase = exchangeRateResponse.conversionRates.toMutableMap()
                    ratesWithEurBase[INTERNAL_API_BASE_CURRENCY] = 1.0
                    exchangeRates = ratesWithEurBase
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch exchange rates (API base: EUR)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addCurrency(currency: Currency) {
        val currentList = _selectedCurrencies.value.toMutableList()
        if (!currentList.contains(currency)) {
            currentList.add(currency)
            _selectedCurrencies.value = currentList
        }
    }

    fun getAvailableCurrencies(): List<Currency> {
        val selected = _selectedCurrencies.value.map { it.code }.toSet()
        return AVAILABLE_CURRENCIES.filter { it.code !in selected }
    }

    /**
     * Converts all currencies in the list from the anchor currency
     */
    fun convertAllCurrencies(anchorCode: String, amount: Double, currencies: List<Currency>): Map<String, Double> {
        return currencies.associate { currency ->
            currency.code to calculateConvertedAmount(anchorCode, currency.code, amount)
        }
    }

    /**
     * Calculates converted amount between two currencies using EUR-based rates.
     */
    private fun calculateConvertedAmount(
        fromCurrencyCode: String,
        toCurrencyCode: String,
        amount: Double
    ): Double {
        if (fromCurrencyCode == toCurrencyCode) return amount

        val rates = exchangeRates

        val fromRateToBase = rates[fromCurrencyCode]
        val toRateFromBase = rates[toCurrencyCode]

        return when {
            fromCurrencyCode == INTERNAL_API_BASE_CURRENCY -> {
                // From EUR to Target
                amount * (toRateFromBase ?: 1.0)
            }
            toCurrencyCode == INTERNAL_API_BASE_CURRENCY -> {
                // From Source to EUR
                if (fromRateToBase != null && fromRateToBase != 0.0) {
                    amount / fromRateToBase
                } else {
                    amount // Fallback
                }
            }
            else -> {
                // Convert fromCurrency -> EUR -> toCurrency
                if (fromRateToBase != null && fromRateToBase != 0.0 && toRateFromBase != null) {
                    val amountInEur = amount / fromRateToBase
                    amountInEur * toRateFromBase
                } else {
                    amount // Fallback
                }
            }
        }
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
        exchangeRates = getDefaultRates()
    }
}