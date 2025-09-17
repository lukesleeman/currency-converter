package com.lukesleeman.currencyconverter.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat

open class CurrencyConverterViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _currencyTextFieldValues = MutableStateFlow<Map<String, TextFieldValue>>(emptyMap())
    open val currencyTextFieldValues: StateFlow<Map<String, TextFieldValue>> = _currencyTextFieldValues.asStateFlow()

    private val decimalFormat = DecimalFormat("#,##0.00")

    open val selectedCurrencies: StateFlow<List<Currency>> = repository.selectedCurrencies
    open val isLoading: StateFlow<Boolean> = repository.isLoading
    open val error: StateFlow<String?> = repository.error

    init {
        // Initialize with default values for initial currencies
        val initialCurrencies = repository.selectedCurrencies.value
        updateAllCurrencyFields(initialCurrencies.first().code, 1.0, initialCurrencies)

        // Fetch exchange rates
        viewModelScope.launch { repository.fetchExchangeRates() }
    }

    private fun formatNumber(number: Double): String {
        return decimalFormat.format(number)
    }

    private fun parseAmount(text: String): Double? {
        return if (text.isEmpty() || text == "." || text == ",") 0.0
               else text.replace(",", "").toDoubleOrNull()
    }

    private fun updateAllCurrencyFields(anchorCurrencyCode: String, anchorAmount: Double, currencies: List<Currency> = selectedCurrencies.value) {
        val newTextFields = mutableMapOf<String, TextFieldValue>()
        currencies.forEach { currency ->
            if (currency.code == anchorCurrencyCode) {
                newTextFields[currency.code] = TextFieldValue(formatNumber(anchorAmount))
            } else {
                val convertedAmount = repository.calculateConvertedAmount(
                    anchorCurrencyCode,
                    currency.code,
                    anchorAmount
                )
                newTextFields[currency.code] = TextFieldValue(formatNumber(convertedAmount))
            }
        }
        _currencyTextFieldValues.value = newTextFields
    }

    open fun onCurrencyAmountChanged(currencyCode: String, newTfv: TextFieldValue) {
        val numericValue = parseAmount(newTfv.text)

        if (numericValue != null) {
            // Valid number input - recalculate all currencies using this as anchor
            updateAllCurrencyFields(currencyCode, numericValue)
            // Override the anchor field with user's exact input (preserves formatting)
            val currentFields = _currencyTextFieldValues.value.toMutableMap()
            currentFields[currencyCode] = newTfv
            _currencyTextFieldValues.value = currentFields
        } else {
            // Invalid number input - just update this field
            val currentFields = _currencyTextFieldValues.value.toMutableMap()
            currentFields[currencyCode] = newTfv
            _currencyTextFieldValues.value = currentFields
        }
    }

    open fun addCurrency(currency: Currency) {
        val wasEmpty = repository.selectedCurrencies.value.isEmpty()
        repository.addCurrency(currency)

        if (wasEmpty) {
            // If list was empty, initialize with this currency at 1.0
            updateAllCurrencyFields(currency.code, 1.0, listOf(currency))
        } else {
            // Add the new currency to existing fields, using first existing field as anchor
            val currentFields = _currencyTextFieldValues.value
            val existingEntry = currentFields.entries.firstOrNull()
            if (existingEntry != null) {
                val anchorAmount = parseAmount(existingEntry.value.text) ?: 1.0
                val convertedAmount = repository.calculateConvertedAmount(
                    existingEntry.key,
                    currency.code,
                    anchorAmount
                )
                val updatedFields = currentFields.toMutableMap()
                updatedFields[currency.code] = TextFieldValue(formatNumber(convertedAmount))
                _currencyTextFieldValues.value = updatedFields
            }
        }
    }

    open fun getAvailableCurrencies(): List<Currency> {
        return repository.getAvailableCurrencies()
    }

    open fun refreshExchangeRates() {
        viewModelScope.launch {
            repository.fetchExchangeRates() // Repository handles EUR-based fetching
        }
    }

    open fun clearError() {
        repository.clearError()
    }
}
