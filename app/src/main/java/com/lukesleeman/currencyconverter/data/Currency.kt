package com.lukesleeman.currencyconverter.data

/**
 * Represents a currency with its code, full name, and flag emoji
 */
data class Currency(
    val code: String,
    val name: String,
    val flag: String
)

/**
 * Predefined list of commonly used currencies with their flags
 */
val AVAILABLE_CURRENCIES = listOf(
    Currency("USD", "US Dollar", "🇺🇸"),
    Currency("EUR", "Euro", "🇪🇺"),
    Currency("GBP", "British Pound", "🇬🇧"),
    Currency("JPY", "Japanese Yen", "🇯🇵"),
    Currency("CNY", "Chinese Yuan", "🇨🇳"),
    Currency("CAD", "Canadian Dollar", "🇨🇦"),
    Currency("AUD", "Australian Dollar", "🇦🇺"),
    Currency("CHF", "Swiss Franc", "🇨🇭"),
    Currency("INR", "Indian Rupee", "🇮🇳"),
    Currency("KRW", "South Korean Won", "🇰🇷"),
    Currency("MXN", "Mexican Peso", "🇲🇽"),
    Currency("BRL", "Brazilian Real", "🇧🇷"),
    Currency("RUB", "Russian Ruble", "🇷🇺"),
    Currency("ZAR", "South African Rand", "🇿🇦"),
    Currency("SEK", "Swedish Krona", "🇸🇪"),
    Currency("NOK", "Norwegian Krone", "🇳🇴"),
    Currency("DKK", "Danish Krone", "🇩🇰"),
    Currency("SGD", "Singapore Dollar", "🇸🇬"),
    Currency("HKD", "Hong Kong Dollar", "🇭🇰"),
    Currency("NZD", "New Zealand Dollar", "🇳🇿")
)