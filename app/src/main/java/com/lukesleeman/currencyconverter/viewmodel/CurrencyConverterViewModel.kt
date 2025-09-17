package com.lukesleeman.currencyconverter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the Currency Converter screen
 */
open class CurrencyConverterViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    // Current input amount and selected currency
    private val _inputAmount = MutableStateFlow("")
    open val inputAmount: StateFlow<String> = _inputAmount.asStateFlow()

    private val _selectedBaseCurrency = MutableStateFlow("USD")
    open val selectedBaseCurrency: StateFlow<String> = _selectedBaseCurrency.asStateFlow()

    // Repository state flows
    open val selectedCurrencies: StateFlow<List<Currency>> = repository.selectedCurrencies
    open val exchangeRates: StateFlow<Map<String, Double>> = repository.exchangeRates
    open val isLoading: StateFlow<Boolean> = repository.isLoading
    open val error: StateFlow<String?> = repository.error

    // Converted amounts for all selected currencies
    open val convertedAmounts: Flow<Map<Currency, Double>> = combine(
        inputAmount,
        selectedBaseCurrency,
        selectedCurrencies,
        exchangeRates
    ) { amount, baseCurrency, currencies, rates ->
        val numericAmount = amount.toDoubleOrNull() ?: 0.0
        currencies.associateWith { currency ->
            repository.calculateConvertedAmount(baseCurrency, currency.code, numericAmount)
        }
    }

    init {
        // Load initial exchange rates
        refreshExchangeRates()
    }

    /**
     * Updates the input amount
     */
    open fun updateInputAmount(amount: String) {
        _inputAmount.value = amount
    }

    /**
     * Sets the base currency for conversion
     */
    open fun setBaseCurrency(currencyCode: String) {
        _selectedBaseCurrency.value = currencyCode
        refreshExchangeRates()
    }

    /**
     * Adds a new currency to the conversion list
     */
    open fun addCurrency(currency: Currency) {
        repository.addCurrency(currency)
    }

    /**
     * Removes a currency from the conversion list
     */
    open fun removeCurrency(currency: Currency) {
        repository.removeCurrency(currency)
    }

    /**
     * Gets available currencies that can be added
     */
    open fun getAvailableCurrencies(): List<Currency> {
        return repository.getAvailableCurrencies()
    }

    /**
     * Refreshes exchange rates from the API
     */
    open fun refreshExchangeRates() {
        viewModelScope.launch {
            repository.fetchExchangeRates(_selectedBaseCurrency.value)
        }
    }

    /**
     * Clears any error state
     */
    open fun clearError() {
        repository.clearError()
    }
}