package com.lukesleeman.currencyconverter.di

import com.lukesleeman.currencyconverter.network.CurrencyApi
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import com.lukesleeman.currencyconverter.viewmodel.CurrencyConverterViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Simple dependency injection module for the app
 */
object AppModule {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(CurrencyApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val currencyApi = retrofit.create(CurrencyApi::class.java)

    private val currencyRepository = CurrencyRepository(currencyApi)

    /**
     * Provides the CurrencyConverterViewModel instance
     */
    fun provideCurrencyConverterViewModel(): CurrencyConverterViewModel {
        return CurrencyConverterViewModel(currencyRepository)
    }
}