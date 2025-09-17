package com.lukesleeman.currencyconverter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.ExchangeRateResponse
import com.lukesleeman.currencyconverter.di.AppModule
import com.lukesleeman.currencyconverter.network.CurrencyApi
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import com.lukesleeman.currencyconverter.ui.components.AddCurrencyDialog
import com.lukesleeman.currencyconverter.ui.components.AmountInput
import com.lukesleeman.currencyconverter.ui.components.CurrencyItem
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme
import com.lukesleeman.currencyconverter.viewmodel.CurrencyConverterViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import retrofit2.Response

/**
 * Main Currency Converter Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    viewModel: CurrencyConverterViewModel = remember { AppModule.provideCurrencyConverterViewModel() }
) {
    val inputAmount by viewModel.inputAmount.collectAsState()
    val selectedBaseCurrency by viewModel.selectedBaseCurrency.collectAsState()
    val selectedCurrencies by viewModel.selectedCurrencies.collectAsState()
    val convertedAmounts by viewModel.convertedAmounts.collectAsState(initial = emptyMap())
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showAddCurrencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Currency Converter",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            actions = {
                IconButton(onClick = { viewModel.refreshExchangeRates() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh rates"
                    )
                }
                IconButton(onClick = { showAddCurrencyDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add currency"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Amount Input
        AmountInput(
            value = inputAmount,
            onValueChange = viewModel::updateInputAmount,
            baseCurrencyCode = selectedBaseCurrency
        )

        // Info about using default rates (shown when no recent API call was successful)
        if (!isLoading && error == null && convertedAmounts.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Using approximate exchange rates. Tap refresh for latest rates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error message
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text(
                            text = "Dismiss",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Currency List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(selectedCurrencies) { currency ->
                val amount = convertedAmounts[currency] ?: 0.0
                val isBaseCurrency = currency.code == selectedBaseCurrency

                CurrencyItem(
                    currency = currency,
                    convertedAmount = amount,
                    isBaseCurrency = isBaseCurrency,
                    onBaseCurrencySelected = {
                        viewModel.setBaseCurrency(currency.code)
                    },
                    onRemoveClick = {
                        if (selectedCurrencies.size > 1) {
                            viewModel.removeCurrency(currency)
                        }
                    }
                )
            }

            // Add currency button at the bottom of the list
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    TextButton(
                        onClick = { showAddCurrencyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Add Currency")
                    }
                }
            }
        }
    }

    // Add Currency Dialog
    if (showAddCurrencyDialog) {
        AddCurrencyDialog(
            availableCurrencies = viewModel.getAvailableCurrencies(),
            onCurrencySelected = { currency ->
                viewModel.addCurrency(currency)
            },
            onDismiss = { showAddCurrencyDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyConverterScreenPreview() {
    // Dummy currencies
    val usd = Currency("USD", "United States Dollar", "$")
    val eur = Currency("EUR", "Euro", "€")
    val jpy = Currency("JPY", "Japanese Yen", "¥")
    val gbp = Currency("GBP", "British Pound", "£")

    // Fake API
    val fakeApi = object : CurrencyApi {
        override suspend fun getExchangeRates(baseCurrency: String): Response<ExchangeRateResponse> {
            return Response.success(ExchangeRateResponse(baseCurrency, mapOf(
                "USD" to 1.0, "EUR" to 0.912, "JPY" to 133.0, "GBP" to 0.78
            )))
        }
    }

    // Fake Repository
    val fakeRepository = object : CurrencyRepository(fakeApi) {
        override val selectedCurrencies: StateFlow<List<Currency>> = 
            MutableStateFlow(listOf(usd, eur, jpy, gbp)).asStateFlow()
        override val exchangeRates: StateFlow<Map<String, Double>> = 
            MutableStateFlow(mapOf("USD" to 1.0, "EUR" to 0.912, "JPY" to 133.0, "GBP" to 0.78)).asStateFlow()
        override val isLoading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
        override val error: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()

        override suspend fun fetchExchangeRates(baseCurrency: String) {}
        override fun addCurrency(currency: Currency) {}
        override fun removeCurrency(currency: Currency) {}
        override fun getAvailableCurrencies(): List<Currency> = listOf(usd, eur, jpy, gbp)
        override fun calculateConvertedAmount(
            baseCurrency: String,
            targetCurrency: String,
            amount: Double
        ): Double {
            val rates = exchangeRates.value
            return if (baseCurrency == targetCurrency) {
                amount
            } else {
                val rate = rates[targetCurrency] ?: 1.0
                amount * rate
            }
        }
        override fun clearError() {}
    }

    // Basic 'fake' ViewModel
    val fakeViewModel = object : CurrencyConverterViewModel(fakeRepository) {
        override val inputAmount: StateFlow<String> = MutableStateFlow("100").asStateFlow()
        override val selectedBaseCurrency: StateFlow<String> = MutableStateFlow("USD").asStateFlow()
        // Use selectedCurrencies from fakeRepository
        // Use exchangeRates from fakeRepository
        override val convertedAmounts: Flow<Map<Currency, Double>> = flowOf(
            mapOf(
                usd to 100.0,
                eur to 91.2,
                jpy to 13300.0,
                gbp to 78.0,
            )
        )
        // Use isLoading from fakeRepository
        // Use error from fakeRepository

        override fun updateInputAmount(amount: String) {}
        override fun setBaseCurrency(currencyCode: String) {}
        // addCurrency, removeCurrency, getAvailableCurrencies, clearError, refreshExchangeRates will use fakeRepository's impl
    }

    CurrencyConverterTheme {
        CurrencyConverterScreen(viewModel = fakeViewModel)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CurrencyConverterScreenDarkPreview() {
    CurrencyConverterScreenPreview()
}
