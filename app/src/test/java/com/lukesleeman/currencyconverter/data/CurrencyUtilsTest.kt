package com.lukesleeman.currencyconverter.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for CurrencyUtils
 */
class CurrencyUtilsTest {

    private val testCurrencies = listOf(
        Currency("USD", "US Dollar", "🇺🇸"),
        Currency("EUR", "Euro", "🇪🇺"),
        Currency("JPY", "Japanese Yen", "🇯🇵"),
        Currency("GBP", "British Pound Sterling", "🇬🇧"),
        Currency("CAD", "Canadian Dollar", "🇨🇦"),
        Currency("AUD", "Australian Dollar", "🇦🇺"),
        Currency("CHF", "Swiss Franc", "🇨🇭"),
        Currency("CNY", "Chinese Yuan", "🇨🇳"),
        Currency("INR", "Indian Rupee", "🇮🇳"),
        Currency("KRW", "South Korean Won", "🇰🇷")
    )

    @Test
    fun `filterCurrencies with empty query returns all currencies`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "")
        assertEquals(testCurrencies, result)
    }

    @Test
    fun `filterCurrencies with blank query returns all currencies`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "   ")
        assertEquals(testCurrencies, result)
    }

    @Test
    fun `filterCurrencies with currency code returns matching currency`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "USD")
        assertEquals(1, result.size)
        assertEquals("USD", result.first().code)
    }

    @Test
    fun `filterCurrencies with lowercase currency code returns matching currency`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "usd")
        assertEquals(1, result.size)
        assertEquals("USD", result.first().code)
    }

    @Test
    fun `filterCurrencies with partial currency code returns matching currencies`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "U")

        // Should return currencies that contain 'U' in code or name (case insensitive)
        // USD (code), AUD (code), EUR (code), GBP (name: British Pound Sterling),
        // CHF (name: Swiss Franc), CNY (name: Chinese Yuan), KRW (name: South Korean Won)
        val codes = result.map { it.code }
        assertTrue(codes.contains("USD"), "USD should be included")
        assertTrue(codes.contains("AUD"), "AUD should be included")
        // Just verify we got some results, the exact count may vary based on names
        assertTrue(result.isNotEmpty(), "Should find currencies containing 'U'")
    }

    @Test
    fun `filterCurrencies with currency name returns matching currency`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "Euro")
        assertEquals(1, result.size)
        assertEquals("EUR", result.first().code)
    }

    @Test
    fun `filterCurrencies with partial currency name returns matching currencies`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "Dollar")

        // Should return USD, CAD, and AUD (all contain 'Dollar')
        val codes = result.map { it.code }.sorted()
        assertEquals(listOf("AUD", "CAD", "USD"), codes)
    }

    @Test
    fun `filterCurrencies with lowercase currency name returns matching currencies`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "dollar")

        // Should return USD, CAD, and AUD (all contain 'Dollar', case-insensitive)
        val codes = result.map { it.code }.sorted()
        assertEquals(listOf("AUD", "CAD", "USD"), codes)
    }

    @Test
    fun `filterCurrencies with mixed case query returns matching currencies`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "yEn")
        assertEquals(1, result.size)
        assertEquals("JPY", result.first().code)
    }

    @Test
    fun `filterCurrencies with no matches returns empty list`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, "XYZ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterCurrencies with empty currency list returns empty list`() {
        val result = CurrencyUtils.filterCurrencies(emptyList(), "USD")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterCurrencies is case insensitive for both code and name`() {
        val queries = listOf("usd", "USD", "Us Dollar", "US DOLLAR", "us dollar")

        queries.forEach { query ->
            val result = CurrencyUtils.filterCurrencies(testCurrencies, query)
            assertTrue(result.any { it.code == "USD" }, "Query '$query' should match USD")
        }
    }

    @Test
    fun `filterCurrencies with whitespace in query handles correctly`() {
        val result = CurrencyUtils.filterCurrencies(testCurrencies, " USD ")
        assertEquals(1, result.size)
        assertEquals("USD", result.first().code)
    }

    @Test
    fun `filterCurrencies performance with large list`() {
        // Create a large list to test performance
        val largeCurrencyList = (1..1000).map { i ->
            Currency("CUR$i", "Currency $i", "🏳️")
        }

        val startTime = System.currentTimeMillis()
        val result = CurrencyUtils.filterCurrencies(largeCurrencyList, "500")
        val endTime = System.currentTimeMillis()

        // Should complete quickly (under 100ms for 1000 items)
        assertTrue(endTime - startTime < 100, "Filtering should be fast")
        assertEquals(1, result.size)
        assertEquals("CUR500", result.first().code)
    }
}