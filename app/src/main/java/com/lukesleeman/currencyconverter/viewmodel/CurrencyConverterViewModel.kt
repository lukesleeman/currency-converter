package com.lukesleeman.currencyconverter.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * ViewModel for the Currency Converter screen
 */
open class CurrencyConverterViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _selectedBaseCurrencyCode = MutableStateFlow("USD") // Default base currency
    open val selectedBaseCurrencyCode: StateFlow<String> = _selectedBaseCurrencyCode.asStateFlow()

    private val _currencyTextFieldValues = MutableStateFlow<Map<String, TextFieldValue>>(emptyMap())
    open val currencyTextFieldValues: StateFlow<Map<String, TextFieldValue>> = _currencyTextFieldValues.asStateFlow()

    // Internal state for calculations
    private var baseAmountForCalc: Double = 1.0 // Default numeric amount for the base currency (e.g. 1.00 USD)

    private val decimalFormat = DecimalFormat("#,##0.00") // Standard currency format

    // Repository state flows (unchanged)
    open val selectedCurrencies: StateFlow<List<Currency>> = repository.selectedCurrencies
    open val exchangeRates: StateFlow<Map<String, Double>> = repository.exchangeRates
    open val isLoading: StateFlow<Boolean> = repository.isLoading
    open val error: StateFlow<String?> = repository.error

    init {
        viewModelScope.launch {
            combine(repository.selectedCurrencies, repository.exchangeRates) { currencies, rates ->
                Pair(currencies, rates)
            }.collect { (currencies, _) -> // Rates trigger recalculation but values come from repo.calculateConvertedAmount
                if (currencies.isEmpty()) {
                    _currencyTextFieldValues.value = emptyMap()
                    baseAmountForCalc = 1.0 // Reset base amount
                    // Consider resetting _selectedBaseCurrencyCode if necessary, e.g., to a global default
                    return@collect
                }

                val currentBaseCode = _selectedBaseCurrencyCode.value
                var actualBaseCode = currentBaseCode
                var newBaseAmountForCalc = baseAmountForCalc

                if (!currencies.any { it.code == currentBaseCode }) {
                    // Current base currency is not in the list (e.g., was removed)
                    actualBaseCode = currencies.first().code // Pick the first as the new base
                    // Try to use the new base's current text field value, or default to 1.0
                    newBaseAmountForCalc = _currencyTextFieldValues.value[actualBaseCode]?.text?.toDoubleOrNull() ?: 1.0
                }

                if (actualBaseCode != currentBaseCode) {
                    _selectedBaseCurrencyCode.value = actualBaseCode // Update state
                    baseAmountForCalc = newBaseAmountForCalc // Update internal numeric base amount
                    viewModelScope.launch { repository.fetchExchangeRates(actualBaseCode) } // Fetch rates for new base
                }
                updateTextFieldValuesInternal(currencies, actualBaseCode, newBaseAmountForCalc)
            }
        }
        // Initial fetch for the default/initial base currency
        viewModelScope.launch { repository.fetchExchangeRates(_selectedBaseCurrencyCode.value) }
    }

    private fun formatNumber(number: Double): String {
        return decimalFormat.format(number)
    }

    private fun updateTextFieldValuesInternal(
        currencies: List<Currency>,
        baseCode: String,
        numericBaseAmount: Double,
        drivingFieldTfv: TextFieldValue? = null, // TextFieldValue of the field being edited
        drivingFieldCode: String? = null       // Code of the field being edited
    ) {
        val newTextFields = mutableMapOf<String, TextFieldValue>()
        currencies.forEach { currency ->
            if (currency.code == baseCode) {
                newTextFields[currency.code] = if (drivingFieldCode == baseCode && drivingFieldTfv != null) {
                    drivingFieldTfv
                } else {
                    TextFieldValue(formatNumber(numericBaseAmount))
                }
            } else {
                val convertedAmount = repository.calculateConvertedAmount(baseCode, currency.code, numericBaseAmount)
                newTextFields[currency.code] = TextFieldValue(formatNumber(convertedAmount))
            }
        }
        _currencyTextFieldValues.value = newTextFields
    }

    open fun onCurrencyAmountChanged(currencyCode: String, newTfv: TextFieldValue) {
        val newText = newTfv.text
        val numericValue = if (newText.isEmpty() || newText == "." || newText == ",") 0.0 else newText.replace(",", "").toDoubleOrNull()

        if (numericValue != null) {
            if (_selectedBaseCurrencyCode.value != currencyCode) {
                 _selectedBaseCurrencyCode.value = currencyCode
            }
            baseAmountForCalc = numericValue
            updateTextFieldValuesInternal(repository.selectedCurrencies.value, currencyCode, baseAmountForCalc, newTfv, currencyCode)
        } else {
            val currentMap = _currencyTextFieldValues.value.toMutableMap()
            currentMap[currencyCode] = newTfv
            _currencyTextFieldValues.value = currentMap
        }
    }

    open fun setBaseCurrency(newBaseCode: String) {
        if (_selectedBaseCurrencyCode.value == newBaseCode) return

        val newBaseAmountString = _currencyTextFieldValues.value[newBaseCode]?.text?.replace(",", "")
        val newBaseNumericAmount = newBaseAmountString?.toDoubleOrNull() ?: baseAmountForCalc // Fallback to current baseAmountForCalc if new one is invalid
        
        _selectedBaseCurrencyCode.value = newBaseCode
        baseAmountForCalc = newBaseNumericAmount

        updateTextFieldValuesInternal(repository.selectedCurrencies.value, newBaseCode, baseAmountForCalc, _currencyTextFieldValues.value[newBaseCode], newBaseCode)
        viewModelScope.launch { repository.fetchExchangeRates(newBaseCode) }
    }

    open fun addCurrency(currency: Currency) {
        repository.addCurrency(currency)
        // The `combine` collector in init will handle adding the new currency to _currencyTextFieldValues
        // and calculating its initial value based on the current base currency and amount.
    }

    open fun removeCurrency(currency: Currency) {
        val currentBase = _selectedBaseCurrencyCode.value
        repository.removeCurrency(currency) // This will trigger the `combine` collector
        // If the removed currency was the base, the `combine` collector will pick a new base.
        // We can immediately update the local map to remove the entry for a smoother UI transition.
        val currentTextFields = _currencyTextFieldValues.value.toMutableMap()
        currentTextFields.remove(currency.code)
        _currencyTextFieldValues.value = currentTextFields
         // If the removed currency was the base and it's the last one, specific handling in combine is needed.
        if (currentBase == currency.code && repository.selectedCurrencies.value.isEmpty()){
            _selectedBaseCurrencyCode.value = "USD" // Reset to a default if all are removed
            baseAmountForCalc = 1.0
            _currencyTextFieldValues.value = emptyMap()
        }

    }

    open fun getAvailableCurrencies(): List<Currency> {
        return repository.getAvailableCurrencies()
    }

    open fun refreshExchangeRates() {
        viewModelScope.launch {
            repository.fetchExchangeRates(_selectedBaseCurrencyCode.value)
        }
    }

    open fun clearError() {
        repository.clearError()
    }
}