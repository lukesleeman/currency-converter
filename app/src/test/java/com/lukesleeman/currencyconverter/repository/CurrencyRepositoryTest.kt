package com.lukesleeman.currencyconverter.repository

import com.lukesleeman.currencyconverter.data.AVAILABLE_CURRENCIES
import com.lukesleeman.currencyconverter.network.CurrencyApi
import com.lukesleeman.currencyconverter.data.ExchangeRateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryTest {

    @Mock
    private lateinit var mockApi: CurrencyApi

    private lateinit var repository: CurrencyRepository
    private val testDispatcher = StandardTestDispatcher()

    private val usdCurrency = AVAILABLE_CURRENCIES.first { it.code == "USD" }
    private val eurCurrency = AVAILABLE_CURRENCIES.first { it.code == "EUR" }
    private val gbpCurrency = AVAILABLE_CURRENCIES.first { it.code == "GBP" }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        repository = CurrencyRepository(mockApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectedCurrencies should start with default currencies`() = runTest {
        val selectedCurrencies = repository.selectedCurrencies.first()

        assertEquals(4, selectedCurrencies.size)
        assertTrue(selectedCurrencies.any { it.code == "USD" })
        assertTrue(selectedCurrencies.any { it.code == "EUR" })
        assertTrue(selectedCurrencies.any { it.code == "GBP" })
        assertTrue(selectedCurrencies.any { it.code == "JPY" })
    }

    @Test
    fun `addCurrency should add new currency to selected list`() = runTest {
        val cnyCurrency = AVAILABLE_CURRENCIES.first { it.code == "CNY" }

        repository.addCurrency(cnyCurrency)
        testDispatcher.scheduler.advanceUntilIdle()

        val selectedCurrencies = repository.selectedCurrencies.value
        assertTrue(selectedCurrencies.contains(cnyCurrency))
    }

    @Test
    fun `addCurrency should not add duplicate currency`() = runTest {
        val initialCount = repository.selectedCurrencies.value.size

        repository.addCurrency(usdCurrency) // USD should already be in the default list
        testDispatcher.scheduler.advanceUntilIdle()

        val finalCount = repository.selectedCurrencies.value.size
        assertEquals(initialCount, finalCount)
    }


    @Test
    fun `getAvailableCurrencies should return non-selected currencies`() {
        val availableCurrencies = repository.getAvailableCurrencies()

        // Should not contain the default selected currencies
        assertFalse(availableCurrencies.any { it.code == "USD" })
        assertFalse(availableCurrencies.any { it.code == "EUR" })
        assertFalse(availableCurrencies.any { it.code == "GBP" })
        assertFalse(availableCurrencies.any { it.code == "JPY" })

        // But should contain others like CNY, CAD, etc.
        assertTrue(availableCurrencies.any { it.code == "CNY" })
        assertTrue(availableCurrencies.any { it.code == "CAD" })
    }


    @Test
    fun `convertAllCurrencies should convert all currencies in list`() {
        val currencies = listOf(usdCurrency, eurCurrency, gbpCurrency)
        val result = repository.convertAllCurrencies("EUR", 100.0, currencies)

        assertEquals(3, result.size)
        assertEquals(118.0, result["USD"]) // 100 EUR * 1.18
        assertEquals(100.0, result["EUR"]) // Same currency
        assertEquals(86.0, result["GBP"])  // 100 EUR * 0.86
    }

    @Test
    fun `fetchExchangeRates should return success when API succeeds`() = runTest {
        val mockResponse = ExchangeRateResponse(
            baseCode = "EUR",
            conversionRates = mapOf("USD" to 1.20, "GBP" to 0.85, "EUR" to 1.0)
        )
        whenever(mockApi.getExchangeRates("EUR")).thenReturn(Response.success(mockResponse))

        val result = repository.fetchExchangeRates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fetchExchangeRates should return failure when API throws exception`() = runTest {
        whenever(mockApi.getExchangeRates("EUR")).thenThrow(RuntimeException("Network error"))

        val result = repository.fetchExchangeRates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isFailure)
    }
}