package com.lukesleeman.currencyconverter.di

import android.content.Context
import com.lukesleeman.currencyconverter.cache.ExchangeRateFileCache
import com.lukesleeman.currencyconverter.network.CurrencyApi
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import com.lukesleeman.currencyconverter.viewmodel.CurrencyConverterViewModel
import kotlinx.serialization.json.Json
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

    /**
     * Create file cache for exchange rates
     */
    private fun createFileCache(context: Context): ExchangeRateFileCache {
        return ExchangeRateFileCache(context.cacheDir, Json.Default)
    }

    /**
     * Create repository with file cache and API dependencies
     */
    private fun createRepository(context: Context): CurrencyRepository {
        val fileCache = createFileCache(context)

        return CurrencyRepository(
            fetchExchangeRatesFromApi = currencyApi::getExchangeRates,
            saveRates = fileCache::saveRates,
            loadRates = fileCache::loadRates
        )
    }

    /**
     * Provides the CurrencyConverterViewModel instance
     */
    fun provideCurrencyConverterViewModel(context: Context): CurrencyConverterViewModel {
        val repository = createRepository(context)

        return CurrencyConverterViewModel(
            selectedCurrenciesFlow = repository.selectedCurrencies,
            addCurrency = repository::addCurrency,
            getAllAvailableCurrencies = repository::getAvailableCurrencies,
            convertAllCurrencies = repository::convertAllCurrencies,
            onFetchRates = repository::fetchExchangeRates
        )
    }
}