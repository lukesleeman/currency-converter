package com.lukesleeman.currencyconverter.repository

import com.lukesleeman.currencyconverter.data.AVAILABLE_CURRENCIES
import com.lukesleeman.currencyconverter.data.DefaultRates
import com.lukesleeman.currencyconverter.data.ExchangeRateCache
import com.lukesleeman.currencyconverter.data.ExchangeRateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryTest {

    private lateinit var repository: CurrencyRepository
    private val testDispatcher = StandardTestDispatcher()

    // Test state for lambda dependencies - no mocks needed!
    private var apiResponse: Response<ExchangeRateResponse>? = null
    private var apiThrowsException: Exception? = null
    private var savedRates: Map<String, Double>? = null
    private var savedTimestamp: Long? = null
    private var cachedData: ExchangeRateCache? = null

    private val usdCurrency = AVAILABLE_CURRENCIES.first { it.code == "USD" }
    private val eurCurrency = AVAILABLE_CURRENCIES.first { it.code == "EUR" }
    private val gbpCurrency = AVAILABLE_CURRENCIES.first { it.code == "GBP" }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Reset test state
        apiResponse = null
        apiThrowsException = null
        savedRates = null
        savedTimestamp = null
        cachedData = null

        repository = CurrencyRepository(
            fetchExchangeRatesFromApi = { baseCurrency ->
                apiThrowsException?.let { throw it }
                apiResponse ?: Response.error(500, "No response configured".toResponseBody("text/plain".toMediaType()))
            },
            saveRates = { rates, timestamp ->
                savedRates = rates
                savedTimestamp = timestamp
            },
            loadRates = {
                cachedData ?: ExchangeRateCache(
                    rates = DefaultRates.getDefaultRates(),
                    timestamp = System.currentTimeMillis(),
                    baseCurrency = "EUR"
                )
            }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==== Basic Repository Functionality Tests ====

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
    fun `convertAllCurrencies should work with default rates when no API data fetched`() = runTest {
        // Wait for repository initialization to complete
        testDispatcher.scheduler.advanceUntilIdle()

        // Given: Repository with default rates (no API call made)
        val currencies = listOf(usdCurrency, eurCurrency, gbpCurrency)

        // When: Convert currencies
        val result = repository.convertAllCurrencies("EUR", 100.0, currencies)

        // Then: Should use default rates from getDefaultRates()
        assertEquals(3, result.size)
        assertEquals(100.0, result["EUR"]) // Same currency

        // Should have USD and GBP conversions (using default rates)
        assertNotNull(result["USD"])
        assertNotNull(result["GBP"])
        assertTrue(result["USD"]!! > 0)
        assertTrue(result["GBP"]!! > 0)
    }

    @Test
    fun `convertAllCurrencies should convert all currencies in list`() = runTest {
        // Wait for repository initialization to complete
        testDispatcher.scheduler.advanceUntilIdle()

        val currencies = listOf(usdCurrency, eurCurrency, gbpCurrency)
        val result = repository.convertAllCurrencies("EUR", 100.0, currencies)

        assertEquals(3, result.size)
        assertEquals(117.93, result["USD"]!!, 0.01) // 100 EUR * 1.1793
        assertEquals(100.0, result["EUR"]) // Same currency
        assertEquals(86.90, result["GBP"]!!, 0.01)  // 100 EUR * 0.8690
    }

    // ==== API and Persistence Tests ====

    @Test
    fun `fetchExchangeRates should save to cache when API succeeds`() = runTest {
        // Given: Successful API response
        val apiRates = mapOf("USD" to 1.18, "GBP" to 0.86, "JPY" to 129.4)
        val response = ExchangeRateResponse("EUR", apiRates)
        apiResponse = Response.success(response)

        // When: Fetch exchange rates
        val result = repository.fetchExchangeRates()

        // Then: Should save rates to cache via lambda
        assertTrue(result.isSuccess)
        assertNotNull(savedRates)
        assertEquals(mapOf("USD" to 1.18, "GBP" to 0.86, "JPY" to 129.4, "EUR" to 1.0), savedRates)
        assertNotNull(savedTimestamp)
        assertTrue(savedTimestamp!! > 0)

        // And: Repository should use the fetched rates for conversions
        val convertedAmount = repository.convertAllCurrencies("EUR", 100.0, listOf(usdCurrency))["USD"]!!
        assertEquals(118.0, convertedAmount, 0.01)
    }

    @Test
    fun `fetchExchangeRates should fail when API throws exception but keep existing rates`() = runTest {
        // Given: API throws exception, repository already initialized with cached data
        apiThrowsException = RuntimeException("Network error")
        val freshTimestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        cachedData = ExchangeRateCache(
            rates = mapOf("USD" to 1.15, "GBP" to 0.88, "EUR" to 1.0),
            timestamp = freshTimestamp,
            baseCurrency = "EUR"
        )

        // When: Fetch exchange rates
        val result = repository.fetchExchangeRates()

        // Then: Should fail due to exception
        assertTrue(result.isFailure)

        // No new rates should be saved (API failed)
        assertNull(savedRates)

        // Repository should keep using initialized rates (from cache in constructor)
        val convertedAmount = repository.convertAllCurrencies("EUR", 100.0, listOf(usdCurrency))["USD"]!!
        assertEquals(115.0, convertedAmount, 0.01)
    }

    @Test
    fun `fetchExchangeRates should fail when API throws exception with expired cache`() = runTest {
        // Given: API throws exception, repository initialized with expired cached data
        apiThrowsException = RuntimeException("Network error")
        val expiredTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
        cachedData = ExchangeRateCache(
            rates = mapOf("USD" to 1.20, "EUR" to 1.0),
            timestamp = expiredTimestamp,
            baseCurrency = "EUR"
        )

        // When: Fetch exchange rates
        val result = repository.fetchExchangeRates()

        // Then: Should fail due to exception
        assertTrue(result.isFailure)

        // No new rates should be saved (API failed)
        assertNull(savedRates)

        // Repository should keep using initialized rates
        val convertedAmount = repository.convertAllCurrencies("EUR", 100.0, listOf(usdCurrency))["USD"]!!
        assertEquals(120.0, convertedAmount, 0.01)
    }

    @Test
    fun `fetchExchangeRates should fail when API throws exception but have default rates`() = runTest {
        // Given: API throws exception and no cached data (will use default rates)
        apiThrowsException = RuntimeException("Network error")
        cachedData = null

        // When: Fetch exchange rates
        val result = repository.fetchExchangeRates()

        // Then: Should fail due to exception
        assertTrue(result.isFailure)

        // No new rates should be saved (API failed)
        assertNull(savedRates)

        // But repository still works with default rates from initialization
        val convertedAmount = repository.convertAllCurrencies("EUR", 100.0, listOf(usdCurrency))["USD"]!!
        assertTrue(convertedAmount > 0) // Should have some conversion using default rates
    }

    @Test
    fun `fetchExchangeRates should handle API success response but null body`() = runTest {
        // Given: API success but null response body
        apiResponse = Response.success(null)

        // When: Fetch exchange rates
        val result = repository.fetchExchangeRates()

        // Then: Should succeed with default rates from cache
        assertTrue(result.isSuccess)
        assertNull(savedRates)
    }

    @Test
    fun `fetchExchangeRates should succeed when API returns error and keep existing rates`() = runTest {
        // Given: API returns unsuccessful response, repository initialized with cached data
        apiResponse = Response.error(500, "Server Error".toResponseBody("text/plain".toMediaType()))
        cachedData = ExchangeRateCache(
            rates = mapOf("USD" to 1.22, "EUR" to 1.0),
            timestamp = System.currentTimeMillis(),
            baseCurrency = "EUR"
        )

        // When: Fetch exchange rates
        val result = repository.fetchExchangeRates()

        // Then: Should succeed (always succeeds now)
        assertTrue(result.isSuccess)

        // Should keep using initialized rates
        val convertedAmount = repository.convertAllCurrencies("EUR", 100.0, listOf(usdCurrency))["USD"]!!
        assertEquals(122.0, convertedAmount, 0.01)
    }
}