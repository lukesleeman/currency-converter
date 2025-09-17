package com.lukesleeman.currencyconverter.network

import com.lukesleeman.currencyconverter.data.ExchangeRateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API interface for fetching currency exchange rates
 * Uses exchangerate-api.com which provides free exchange rates
 */
interface CurrencyApi {

    @GET("v6/latest/{baseCurrency}")
    suspend fun getExchangeRates(
        @Path("baseCurrency") baseCurrency: String
    ): Response<ExchangeRateResponse>

    companion object {
        const val BASE_URL = "https://api.exchangerate-api.com/"
    }
}