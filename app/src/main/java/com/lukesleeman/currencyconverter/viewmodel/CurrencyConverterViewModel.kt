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

open class CurrencyConverterViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _currencyTextFieldValues = MutableStateFlow<Map<String, TextFieldValue>>(emptyMap())
    open val currencyTextFieldValues: StateFlow<Map<String, TextFieldValue>> = _currencyTextFieldValues.asStateFlow()

    // UserInputAnchor: Pair(currencyCode, numericAmount) - tracks the last field edited by the user that had a valid number.
    private val _userInputAnchor: MutableStateFlow<Pair<String, Double>>

    private val decimalFormat = DecimalFormat("#,##0.00")

    open val selectedCurrencies: StateFlow<List<Currency>> = repository.selectedCurrencies
    open val exchangeRates: StateFlow<Map<String, Double>> = repository.exchangeRates // These are EUR-based from repository
    open val isLoading: StateFlow<Boolean> = repository.isLoading
    open val error: StateFlow<String?> = repository.error

    init {
        val initialSelected = repository.selectedCurrencies.value
        _userInputAnchor = MutableStateFlow(
            if (initialSelected.isNotEmpty()) {
                Pair(initialSelected.first().code, 1.0) // Default to first selected currency, amount 1.0
            } else {
                Pair("USD", 1.0) // Fallback if no currencies are initially selected
            }
        )

        viewModelScope.launch {
            combine(
                repository.selectedCurrencies,
                repository.exchangeRates, // These are EUR-based rates from the repository
                _userInputAnchor
            ) { currencies, _, anchor -> // rates are observed to trigger recalculation, but not directly used here (repo.calculate uses them)
                Triple(currencies, anchor, System.nanoTime()) // Add nanoTime to ensure re-evaluation even if other params don't change by reference
            }.collect { (currencies, anchor, _) ->
                if (currencies.isEmpty()) {
                    _currencyTextFieldValues.value = emptyMap()
                    // Anchor can remain, will be reset/validated when a currency is added.
                    return@collect
                }

                var (currentAnchorCode, currentAnchorAmount) = anchor

                // Validate anchor: if its currency is not in the selected list, pick a new one.
                if (!currencies.any { it.code == currentAnchorCode }) {
                    val newAnchorCode = currencies.first().code
                    val newAnchorAmount = 1.0 // Reset amount for new anchor
                    // Update the anchor, this will re-trigger the combine flow.
                    _userInputAnchor.value = Pair(newAnchorCode, newAnchorAmount)
                    return@collect // Exit and let the new anchor value re-trigger processing.
                }

                val newTextFields = mutableMapOf<String, TextFieldValue>()
                currencies.forEach { currency ->
                    if (currency.code == currentAnchorCode) {
                        newTextFields[currency.code] = TextFieldValue(formatNumber(currentAnchorAmount))
                    } else {
                        val convertedAmount = repository.calculateConvertedAmount(
                            currentAnchorCode,    // From this currency (the anchor)
                            currency.code,        // To this currency
                            currentAnchorAmount   // This amount of the anchor currency
                        )
                        newTextFields[currency.code] = TextFieldValue(formatNumber(convertedAmount))
                    }
                }
                _currencyTextFieldValues.value = newTextFields
            }
        }

        // Initial fetch for exchange rates (EUR based by repository)
        viewModelScope.launch { repository.fetchExchangeRates() }
    }

    private fun formatNumber(number: Double): String {
        return decimalFormat.format(number)
    }

    open fun onCurrencyAmountChanged(currencyCode: String, newTfv: TextFieldValue) {
        val newText = newTfv.text
        val numericValue = if (newText.isEmpty() || newText == "." || newText == ",") 0.0 else newText.replace(",", "").toDoubleOrNull()

        if (numericValue != null) {
            // Valid number input, update the anchor. This will trigger the combine block for recalculations.
            if (_userInputAnchor.value.first != currencyCode || _userInputAnchor.value.second != numericValue) {
                 _userInputAnchor.value = Pair(currencyCode, numericValue)
            }
        } else {
            // Invalid number input (e.g., "abc"). Update only this specific text field to show the raw input.
            // Do not change the anchor, so other fields remain based on the last valid anchor.
            val currentMap = _currencyTextFieldValues.value.toMutableMap()
            currentMap[currencyCode] = newTfv
            _currencyTextFieldValues.value = currentMap
        }
    }

    open fun addCurrency(currency: Currency) {
        val wasEmpty = repository.selectedCurrencies.value.isEmpty()
        repository.addCurrency(currency) // Triggers selectedCurrencies flow

        if (wasEmpty) {
            // If list was empty, this new currency becomes the anchor with amount 1.0.
            _userInputAnchor.value = Pair(currency.code, 1.0)
            // No need to explicitly fetch rates here for the new currency, 
            // repo.fetchExchangeRates() is general now.
        }
        // The combine flow will handle UI updates for the new currency list.
    }

    open fun removeCurrency(currency: Currency) {
        val currentSelected = repository.selectedCurrencies.value.toMutableList()
        currentSelected.remove(currency)

        // If the removed currency was the anchor, combine's validation will pick a new one.
        // If the list becomes empty, combine will clear text fields.
        // If the anchor needs to change, it will be updated by the combine block.
        repository.removeCurrency(currency) // Triggers selectedCurrencies flow

        // For smoother immediate UI, remove the field from current display.
        // The combine flow will establish the final consistent state.
        val currentTextFields = _currencyTextFieldValues.value.toMutableMap()
        currentTextFields.remove(currency.code)
        _currencyTextFieldValues.value = currentTextFields
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
