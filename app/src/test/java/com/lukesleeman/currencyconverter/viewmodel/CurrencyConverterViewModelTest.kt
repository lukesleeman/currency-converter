package com.lukesleeman.currencyconverter.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CurrencyConverterViewModel using clean dependency injection.
 * No mocking required - just simple lambdas and flows!
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyConverterViewModelTest {

    private lateinit var viewModel: CurrencyConverterViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val usdCurrency = Currency("USD", "US Dollar", "$")
    private val eurCurrency = Currency("EUR", "Euro", "€")
    private val gbpCurrency = Currency("GBP", "British Pound", "£")

    private val testSelectedCurrencies = listOf(usdCurrency, eurCurrency)
    private val testAllCurrencies = listOf(usdCurrency, eurCurrency, gbpCurrency)

    private lateinit var addedCurrencies: MutableList<Currency>
    private var fetchRatesCallCount = 0
    private var testActiveCurrencyCode: String = "USD"
    private var testCurrentInputValue: String = "1.00"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addedCurrencies = mutableListOf()
        fetchRatesCallCount = 0
        testActiveCurrencyCode = "USD"
        testCurrentInputValue = "1.00"

        viewModel = CurrencyConverterViewModel(
            selectedCurrenciesFlow = flowOf(testSelectedCurrencies),
            addCurrency = { currency -> addedCurrencies.add(currency) },
            getAllAvailableCurrencies = { testAllCurrencies },
            convertAllCurrencies = { anchorCode, amount, currencies ->
                // Simple test conversion: USD=1.0, EUR=0.85, GBP=0.75
                val rates = mapOf("USD" to 1.0, "EUR" to 0.85, "GBP" to 0.75)
                val anchorRate = rates[anchorCode] ?: 1.0

                currencies.associate { currency ->
                    val targetRate = rates[currency.code] ?: 1.0
                    val convertedAmount = amount * (targetRate / anchorRate)
                    currency.code to convertedAmount
                }
            },
            onFetchRates = {
                fetchRatesCallCount++
                Result.success(Unit)
            },
            getPreferences = {
                UserPreferences(
                    selectedCurrencyCodes = testSelectedCurrencies.map { it.code },
                    activeCurrencyCode = testActiveCurrencyCode,
                    currentInputValue = testCurrentInputValue
                )
            },
            updatePreferences = { update ->
                val currentPrefs = UserPreferences(
                    selectedCurrencyCodes = testSelectedCurrencies.map { it.code },
                    activeCurrencyCode = testActiveCurrencyCode,
                    currentInputValue = testCurrentInputValue
                )
                val updated = update(currentPrefs)
                testActiveCurrencyCode = updated.activeCurrencyCode
                testCurrentInputValue = updated.currentInputValue
            }
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should initialize with selected currencies as UI state`() = runTest {
        val uiState = viewModel.uiState.value

        assertEquals(2, uiState.currencies.size)
        assertEquals("USD", uiState.activeCurrency.currency.code)
        assertEquals("USD", uiState.currencies[0].currency.code)
        assertEquals("EUR", uiState.currencies[1].currency.code)
    }

    @Test
    fun `should initialize with first currency text selected on app launch`() = runTest {
        // When: ViewModel is initialized (happens in setup)
        val uiState = viewModel.uiState.value

        // Then: The active currency (first currency) should have all its text selected
        val activeTextFieldValue = uiState.activeCurrency.textFieldValue
        val expectedSelection = TextRange(0, activeTextFieldValue.text.length)

        assertEquals(expectedSelection, activeTextFieldValue.selection,
            "First currency text should be fully selected on app launch so user can immediately type to overwrite")
    }

    @Test
    fun `setActiveCurrency should select all text`() = runTest {
        // When: Set EUR as active
        viewModel.setActiveCurrency("EUR")

        val uiState = viewModel.uiState.value

        // Then: EUR should be active with all text selected
        assertEquals("EUR", uiState.activeCurrency.currency.code)
        val eurText = uiState.activeCurrency.textFieldValue.text
        assertEquals(TextRange(0, eurText.length), uiState.activeCurrency.textFieldValue.selection)
    }

    // Test the key "overwrite when selected" behavior
    @Test
    fun `addDigit when text is selected should replace entire selection`() = runTest {
        // Given: Active currency has "100.00" with ALL text selected
        val selectedValue = TextFieldValue("100.00", selection = TextRange(0, 6))
        viewModel.updateActiveFieldText(selectedValue)

        // When: User types "7" (should replace entire selection)
        viewModel.addDigit("7")

        val uiState = viewModel.uiState.value

        // Then: Should become "7", not "7100.00" or anything else
        assertEquals("7", uiState.activeCurrency.textFieldValue.text)
    }

    @Test
    fun `addDigit when text has cursor position should append`() = runTest {
        // Given: Active currency has "100" with cursor at end (no selection)
        val cursorAtEnd = TextFieldValue("100", selection = TextRange(3))
        viewModel.updateActiveFieldText(cursorAtEnd)

        // When: User types "5" (should append)
        viewModel.addDigit("5")

        val uiState = viewModel.uiState.value

        // Then: Should become "1005"
        assertEquals("1005", uiState.activeCurrency.textFieldValue.text)
    }

    @Test
    fun `addDigit when partial text is selected should replace selection`() = runTest {
        // Given: "123.45" with ".45" selected (positions 3-6)
        val partialSelection = TextFieldValue("123.45", selection = TextRange(3, 6))
        viewModel.updateActiveFieldText(partialSelection)

        // When: User types "9"
        viewModel.addDigit("9")

        val uiState = viewModel.uiState.value

        // Then: Should become "1239" (replaced ".45" with "9")
        assertEquals("1239", uiState.activeCurrency.textFieldValue.text)
    }

    @Test
    fun `updateActiveFieldText with selection should behave like direct typing`() = runTest {
        // Given: User directly types in TextField, replacing selected text
        val newValue = TextFieldValue("42", selection = TextRange(2))

        // When: Update through direct typing
        viewModel.updateActiveFieldText(newValue)

        val uiState = viewModel.uiState.value

        // Then: Should update to new value
        assertEquals("42", uiState.activeCurrency.textFieldValue.text)
    }

    @Test
    fun `addDecimalPoint when text selected should replace with decimal`() = runTest {
        // Given: "100.00" with all text selected
        val selectedValue = TextFieldValue("100.00", selection = TextRange(0, 6))
        viewModel.updateActiveFieldText(selectedValue)

        // When: User clicks decimal point
        viewModel.addDecimalPoint()

        val uiState = viewModel.uiState.value

        // Then: Should become "0." (replace selection with decimal)
        assertEquals("0.", uiState.activeCurrency.textFieldValue.text)
    }

    @Test
    fun `backspace when text selected should delete selection`() = runTest {
        // Given: "123.45" with ".45" selected
        val partialSelection = TextFieldValue("123.45", selection = TextRange(3, 6))
        viewModel.updateActiveFieldText(partialSelection)

        // When: User hits backspace
        viewModel.backspace()

        val uiState = viewModel.uiState.value

        // Then: Should become "123" (deleted the selection)
        assertEquals("123", uiState.activeCurrency.textFieldValue.text)
    }

    // Additional core behavior tests
    @Test
    fun `addDigit on default 0 dot 00 should replace`() = runTest {
        viewModel.updateActiveFieldText(TextFieldValue("0.00"))
        viewModel.addDigit("5")

        val uiState = viewModel.uiState.value
        assertEquals("5", uiState.activeCurrency.textFieldValue.text)
    }

    @Test
    fun `changing active currency should update all others`() = runTest {
        // When: Set USD to "100"
        viewModel.updateActiveFieldText(TextFieldValue("100"))

        val uiState = viewModel.uiState.value

        // Then: EUR should be converted (100 USD * 0.85 = 85 EUR)
        val eurCurrency = uiState.currencies.find { it.currency.code == "EUR" }
        assertEquals("85.00", eurCurrency?.textFieldValue?.text)
    }

    @Test
    fun `addCurrency should call addCurrency callback`() = runTest {
        // When: Add GBP currency
        viewModel.addCurrency(gbpCurrency)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should call the callback
        assertEquals(1, addedCurrencies.size)
        assertEquals("GBP", addedCurrencies[0].code)
    }

    @Test
    fun `refreshExchangeRates should call onFetchRates and update lastUpdated on success`() = runTest {
        // Given: Reset counter after init
        fetchRatesCallCount = 0
        val initialLastUpdated = viewModel.uiState.value.lastUpdated

        // When: Refresh rates
        viewModel.refreshExchangeRates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should call fetch rates
        assertEquals(1, fetchRatesCallCount)

        // And: Loading state should be false and lastUpdated should be updated
        val uiState = viewModel.uiState.value
        assertEquals(false, uiState.isLoading)
        assertTrue(uiState.lastUpdated != null && uiState.lastUpdated != initialLastUpdated,
            "lastUpdated should be set to current timestamp on successful refresh")
    }

    @Test
    fun `refreshExchangeRates should not update lastUpdated on failure`() = runTest {
        // Given: ViewModel that fails on refresh
        val failingViewModel = CurrencyConverterViewModel(
            selectedCurrenciesFlow = flowOf(testSelectedCurrencies),
            addCurrency = { currency -> addedCurrencies.add(currency) },
            getAllAvailableCurrencies = { testAllCurrencies },
            convertAllCurrencies = { anchorCode, amount, currencies ->
                val rates = mapOf("USD" to 1.0, "EUR" to 0.85)
                val anchorRate = rates[anchorCode] ?: 1.0
                currencies.associate { currency ->
                    val targetRate = rates[currency.code] ?: 1.0
                    val convertedAmount = amount * (targetRate / anchorRate)
                    currency.code to convertedAmount
                }
            },
            onFetchRates = { Result.failure(Exception("Network error")) },
            getPreferences = {
                UserPreferences(
                    selectedCurrencyCodes = testSelectedCurrencies.map { it.code },
                    activeCurrencyCode = testActiveCurrencyCode,
                    currentInputValue = testCurrentInputValue
                )
            },
            updatePreferences = { update ->
                val currentPrefs = UserPreferences(
                    selectedCurrencyCodes = testSelectedCurrencies.map { it.code },
                    activeCurrencyCode = testActiveCurrencyCode,
                    currentInputValue = testCurrentInputValue
                )
                val updated = update(currentPrefs)
                testActiveCurrencyCode = updated.activeCurrencyCode
                testCurrentInputValue = updated.currentInputValue
            }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val initialLastUpdated = failingViewModel.uiState.value.lastUpdated

        // When: Refresh rates (which will fail)
        failingViewModel.refreshExchangeRates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Loading should be false but lastUpdated should remain unchanged
        val uiState = failingViewModel.uiState.value
        assertEquals(false, uiState.isLoading)
        assertEquals(initialLastUpdated, uiState.lastUpdated,
            "lastUpdated should not change when refresh fails")
    }

    @Test
    fun `setActiveCurrency should update currentInputValue in preferences to match new active currency text`() = runTest {
        // Given: USD is active with "100.00"
        viewModel.updateActiveFieldText(TextFieldValue("100.00"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Switch to EUR (which should have converted value "85.00")
        viewModel.setActiveCurrency("EUR")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: currentInputValue in preferences should be updated to EUR's text value
        assertEquals("85.00", testCurrentInputValue,
            "currentInputValue should be updated to match the new active currency's text value")
    }
}