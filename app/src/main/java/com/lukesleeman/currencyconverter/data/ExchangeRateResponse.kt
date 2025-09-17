package com.lukesleeman.currencyconverter.data

import com.google.gson.annotations.SerializedName

/**
 * Response from the exchange rate API
 */
data class ExchangeRateResponse(
    @SerializedName("base_code")
    val baseCode: String,
    @SerializedName("conversion_rates")
    val conversionRates: Map<String, Double>
)
