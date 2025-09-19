package com.lukesleeman.currencyconverter.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukesleeman.currencyconverter.data.Currency
import kotlinx.coroutines.flow.Flow
import com.lukesleeman.currencyconverter.ui.state.CurrencyConverterUiState
import com.lukesleeman.currencyconverter.ui.state.CurrencyDisplayItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class CurrencyConverterViewModel(
    private val selectedCurrenciesFlow: Flow<List<Currency>>,
    private val addCurrency: (Currency) -> Unit,
    private val getAllAvailableCurrencies: () -> List<Currency>,
    private val convertAllCurrencies: (anchorCode: String, amount: Double, currencies: List<Currency>) -> Map<String, Double>,
    private val onFetchRates: suspend () -> Result<Unit>
) : ViewModel() {

    companion object {
        private const val DECIMAL_FORMAT_PATTERN = "#,##0.00"
    }

    private val decimalFormat = DecimalFormat(DECIMAL_FORMAT_PATTERN)

    private val _uiState = MutableStateFlow(
        CurrencyConverterUiState(
            currencies = emptyList(),
            activeCurrency = CurrencyDisplayItem(
                Currency("USD", "US Dollar", "$"),
                TextFieldValue("0.00")
            ),
            isLoading = true
        )
    )
    val uiState: StateFlow<CurrencyConverterUiState> = _uiState.asStateFlow()

    init {
        // Observe selected currencies and initialize UI state
        viewModelScope.launch {
            selectedCurrenciesFlow.collect { currencies ->
                if (currencies.isNotEmpty()) {
                    initializeWithCurrencies(currencies)
                }
            }
        }

        // Fetch exchange rates
        refreshExchangeRates()
    }

    private fun initializeWithCurrencies(currencies: List<Currency>) {
        val firstCurrency = currencies.first()
        val conversionResults = convertAllCurrencies(firstCurrency.code, 1.0, currencies)

        val displayItems = currencies.map { currency ->
            val convertedAmount = if (currency.code == firstCurrency.code) 1.0
                                 else conversionResults[currency.code] ?: 1.0
            CurrencyDisplayItem(currency, TextFieldValue(formatNumber(convertedAmount)))
        }

        _uiState.value = _uiState.value.copy(
            currencies = displayItems,
            activeCurrency = displayItems.first(),
            isLoading = false,
            lastUpdated = System.currentTimeMillis()
        )

        // Use existing method to select all text in the first currency
        setActiveCurrency(firstCurrency.code)
    }

    /**
     * Set which currency is active and select all its text
     */
    fun setActiveCurrency(currencyCode: String) {
        val currentState = _uiState.value
        val targetCurrency = currentState.currencies.find { it.currency.code == currencyCode }
            ?: return

        // Select all text in the target currency
        val activeCurrencyWithSelection = targetCurrency.copy(
            textFieldValue = targetCurrency.textFieldValue.copy(
                selection = TextRange(0, targetCurrency.textFieldValue.text.length)
            )
        )

        // Update currencies list with the selected text
        val updatedCurrencies = currentState.currencies.map { item ->
            if (item.currency.code == currencyCode) activeCurrencyWithSelection else item
        }

        _uiState.value = currentState.copy(
            currencies = updatedCurrencies,
            activeCurrency = activeCurrencyWithSelection
        )
    }

    /**
     * Update the active currency's text field (handles direct typing)
     */
    fun updateActiveFieldText(newValue: TextFieldValue) {
        val currentState = _uiState.value
        val activeCurrencyCode = currentState.activeCurrency.currency.code
        val updatedActiveCurrency = currentState.activeCurrency.copy(textFieldValue = newValue)

        // Replace the active currency in the list with updated value
        val updatedCurrencies = currentState.currencies.replaceWhere(
            predicate = { it.currency.code == activeCurrencyCode },
            replacement = updatedActiveCurrency
        )

        val finalCurrencies = parseAmount(newValue.text)?.let { amount ->
            updateOtherCurrencies(updatedCurrencies, activeCurrencyCode, amount)
        } ?: updatedCurrencies

        _uiState.value = currentState.copy(
            currencies = finalCurrencies,
            activeCurrency = updatedActiveCurrency
        )
    }

    /**
     * Add a digit to the active currency (handles numeric keyboard and text selection)
     */
    fun addDigit(digit: String) {
        val currentState = _uiState.value
        val currentValue = currentState.activeCurrency.textFieldValue

        val newText = when {
            // If text is selected, replace the selected portion with the digit
            !currentValue.selection.collapsed -> {
                val text = currentValue.text
                text.substring(0, currentValue.selection.start) + digit + text.substring(currentValue.selection.end)
            }
            // If current text is default "0.00", replace it
            currentValue.text == "0.00" -> digit
            // Otherwise append
            else -> currentValue.text + digit
        }

        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )

        updateActiveFieldText(newValue)
    }

    /**
     * Add decimal point to the active currency
     */
    fun addDecimalPoint() {
        val currentState = _uiState.value
        val currentValue = currentState.activeCurrency.textFieldValue

        val newText = when {
            // If text is selected, replace with "0."
            !currentValue.selection.collapsed -> "0."
            // If already has decimal, do nothing
            currentValue.text.contains(".") -> currentValue.text
            // If empty or just "0", make it "0."
            currentValue.text.isEmpty() || currentValue.text == "0" -> "0."
            // Otherwise append decimal
            else -> currentValue.text + "."
        }

        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )

        updateActiveFieldText(newValue)
    }

    /**
     * Handle backspace on the active currency
     */
    fun backspace() {
        val currentState = _uiState.value
        val currentValue = currentState.activeCurrency.textFieldValue

        val newText = when {
            // If text is selected, delete the selection
            !currentValue.selection.collapsed -> {
                val text = currentValue.text
                text.substring(0, currentValue.selection.start) +
                text.substring(currentValue.selection.end)
            }
            // If single character or empty, reset to default
            currentValue.text.length <= 1 -> "0.00"
            // Otherwise remove last character
            else -> currentValue.text.dropLast(1)
        }

        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )

        updateActiveFieldText(newValue)
    }

    private fun formatNumber(number: Double): String {
        return decimalFormat.format(number)
    }

    private fun parseAmount(text: String): Double? {
        return if (text.isEmpty() || text == "." || text == ",") 0.0
               else text.replace(",", "").toDoubleOrNull()
    }

    private fun updateOtherCurrencies(
        currencies: List<CurrencyDisplayItem>,
        anchorCurrencyCode: String,
        anchorAmount: Double
    ): List<CurrencyDisplayItem> {
        val currencyList = currencies.map { it.currency }
        val conversionResults = convertAllCurrencies(anchorCurrencyCode, anchorAmount, currencyList)

        return currencies.map { item ->
            if (item.currency.code == anchorCurrencyCode) {
                item // Keep the anchor currency as is
            } else {
                val convertedAmount = conversionResults[item.currency.code] ?: anchorAmount
                item.copy(
                    textFieldValue = TextFieldValue(
                        text = formatNumber(convertedAmount),
                        selection = TextRange(0) // Cursor at start for non-active currencies
                    )
                )
            }
        }
    }

    fun addCurrency(currency: Currency) {
        addCurrency.invoke(currency)
    }


    fun getAvailableCurrencies(): List<Currency> {
        return getAllAvailableCurrencies()
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = onFetchRates()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lastUpdated = if (result.isSuccess) System.currentTimeMillis() else _uiState.value.lastUpdated
            )
        }
    }
}

/**
 * Extension function to replace an item in a list based on a predicate
 */
private fun <T> List<T>.replaceWhere(predicate: (T) -> Boolean, replacement: T): List<T> {
    return map { if (predicate(it)) replacement else it }
}