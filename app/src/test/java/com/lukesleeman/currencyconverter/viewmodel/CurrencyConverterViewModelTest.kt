package com.lukesleeman.currencyconverter.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import com.lukesleeman.currencyconverter.ui.state.CurrencyConverterUiState
import com.lukesleeman.currencyconverter.ui.state.CurrencyDisplayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

/**
 * Tests defining the IDEAL interface and behavior for CurrencyConverterViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyConverterViewModelTest {

    @Mock
    private lateinit var mockRepository: CurrencyRepository

    private lateinit var viewModel: CurrencyConverterViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val usdCurrency = Currency("USD", "US Dollar", "$")
    private val eurCurrency = Currency("EUR", "Euro", "€")
    private val testCurrencies = listOf(usdCurrency, eurCurrency)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        // Setup repository mocks
        whenever(mockRepository.selectedCurrencies).thenReturn(MutableStateFlow(testCurrencies))
        whenever(mockRepository.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockRepository.error).thenReturn(MutableStateFlow(null))
        whenever(mockRepository.calculateConvertedAmount("USD", "EUR", 100.0)).thenReturn(85.0)
        whenever(mockRepository.calculateConvertedAmount("EUR", "USD", 85.0)).thenReturn(100.0)
        whenever(mockRepository.calculateConvertedAmount("USD", "EUR", 200.0)).thenReturn(170.0)
        whenever(mockRepository.calculateConvertedAmount("USD", "EUR", 5.0)).thenReturn(4.25)

        // Create ViewModel after mocks are set up
        viewModel = CurrencyConverterViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setActiveCurrency should select all text`() = runTest {
        viewModel.setActiveCurrency("EUR")

        val uiState = viewModel.uiState.value
        val eurText = uiState.activeCurrency.textFieldValue.text
        val expectedSelection = TextRange(0, eurText.length)
        assertEquals(expectedSelection, uiState.activeCurrency.textFieldValue.selection)
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
        viewModel.updateActiveFieldText(TextFieldValue("200"))

        val uiState = viewModel.uiState.value
        val eurCurrency = uiState.currencies.find { it.currency.code == "EUR" }
        assertEquals("170.00", eurCurrency?.textFieldValue?.text) // 200 * 0.85
    }
}