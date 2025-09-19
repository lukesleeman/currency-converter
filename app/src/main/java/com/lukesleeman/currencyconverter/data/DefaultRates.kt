package com.lukesleeman.currencyconverter.data

/**
 * Default exchange rates for offline functionality.
 * These rates are EUR-based (EUR = 1.0) and fetched from exchangerate-api.com on 2025-09-19.
 */
object DefaultRates {

    /**
     * Provides fallback exchange rates when API is unavailable.
     * All rates are relative to EUR (1 EUR equals X units of other currency).
     */
    fun getDefaultRates(): Map<String, Double> {
        return mapOf(
            "EUR" to 1.0,
            "USD" to 1.1793,
            "GBP" to 0.8690,
            "JPY" to 174.1276,
            "CNY" to 8.3868,
            "CAD" to 1.6264,
            "AUD" to 1.7804,
            "CHF" to 0.9337,
            "INR" to 103.9524,
            "KRW" to 1637.5544,
            "MXN" to 21.6245,
            "BRL" to 6.2466,
            "RUB" to 98.0597,
            "ZAR" to 20.4675,
            "SEK" to 11.0211,
            "NOK" to 11.6369,
            "DKK" to 7.4622,
            "SGD" to 1.5113,
            "HKD" to 9.1735,
            "NZD" to 2.0028,
            "THB" to 37.6106
        )
    }
}