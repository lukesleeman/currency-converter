package com.lukesleeman.currencyconverter.data

/**
 * Utility functions for working with Currency objects
 */
object CurrencyUtils {

    /**
     * Filters a list of currencies based on a search query.
     * Searches both currency code and name (case-insensitive).
     *
     * @param currencies The list of currencies to filter
     * @param searchQuery The search query (empty/blank returns all currencies)
     * @return Filtered list of currencies matching the search query
     */
    fun filterCurrencies(currencies: List<Currency>, searchQuery: String): List<Currency> {
        val trimmedQuery = searchQuery.trim()
        return if (trimmedQuery.isBlank()) {
            currencies
        } else {
            currencies.filter { currency ->
                currency.code.contains(trimmedQuery, ignoreCase = true) ||
                currency.name.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
}