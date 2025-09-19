package com.lukesleeman.currencyconverter.repository

import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.AVAILABLE_CURRENCIES
import com.lukesleeman.currencyconverter.data.ExchangeRateCache
import com.lukesleeman.currencyconverter.data.ExchangeRateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Repository for managing currency data and exchange rates.
 * Exchange rates are always fetched and stored relative to EUR.
 */
class CurrencyRepository(
    private val fetchExchangeRatesFromApi: suspend (String) -> Response<ExchangeRateResponse>,
    private val saveRates: suspend (Map<String, Double>, Long) -> Unit,
    private val loadRates: suspend () -> ExchangeRateCache
) {

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
    private var exchangeRates: Map<String, Double>

    private companion object {
        const val INTERNAL_API_BASE_CURRENCY = "EUR"
    }

    init {
        // Load initial rates from cache (always returns something)
        exchangeRates = runBlocking {
            loadRates().rates
        }
    }

    /**
     * Fetches fresh exchange rates from API and updates cache
     * Always succeeds since we have fallback rates loaded at initialization
     */
    suspend fun fetchExchangeRates(): Result<Unit> {
        return try {
            val response = fetchExchangeRatesFromApi(INTERNAL_API_BASE_CURRENCY)
            if (response.isSuccessful) {
                response.body()?.let { exchangeRateResponse ->
                    // Ensure EUR itself has a rate of 1.0 in the map
                    val ratesWithEurBase = exchangeRateResponse.conversionRates.toMutableMap()
                    ratesWithEurBase[INTERNAL_API_BASE_CURRENCY] = 1.0

                    // Save to cache and update in-memory rates
                    saveRates(ratesWithEurBase, System.currentTimeMillis())
                    exchangeRates = ratesWithEurBase
                }
            }
            // Always succeed - we have fallback rates from initialization
            Result.success(Unit)
        } catch (e: Exception) {
            // API failed, but we still have fallback rates
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

}