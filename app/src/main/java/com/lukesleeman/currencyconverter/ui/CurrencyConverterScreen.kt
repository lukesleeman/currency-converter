package com.lukesleeman.currencyconverter.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.ExchangeRateResponse
import com.lukesleeman.currencyconverter.di.AppModule
import com.lukesleeman.currencyconverter.network.CurrencyApi
import com.lukesleeman.currencyconverter.repository.CurrencyRepository
import com.lukesleeman.currencyconverter.ui.components.AddCurrencyDialog
import com.lukesleeman.currencyconverter.ui.components.CurrencyItem
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme
import com.lukesleeman.currencyconverter.viewmodel.CurrencyConverterViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import java.text.DecimalFormat

/**
 * Main Currency Converter Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    viewModel: CurrencyConverterViewModel = remember { AppModule.provideCurrencyConverterViewModel() }
) {
    val selectedBaseCurrencyCode by viewModel.selectedBaseCurrencyCode.collectAsState()
    val selectedCurrencies by viewModel.selectedCurrencies.collectAsState()
    val currencyTextFieldValues by viewModel.currencyTextFieldValues.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showAddCurrencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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

        // Info about using default rates (shown when no recent API call was successful)
        if (!isLoading && error == null && selectedCurrencies.isNotEmpty() && currencyTextFieldValues.values.all { it.text == "0.00" || it.text == "0,00" }) {
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

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(selectedCurrencies, key = { it.code }) { currency ->
                val textFieldValue = currencyTextFieldValues[currency.code] ?: TextFieldValue("0.00")
                val isBaseCurrency = currency.code == selectedBaseCurrencyCode

                CurrencyItem(
                    currency = currency,
                    amount = textFieldValue,
                    onAmountChange = { newTfv ->
                        viewModel.onCurrencyAmountChanged(currency.code, newTfv)
                    },
                    modifier = Modifier.clickable { // Allow making a currency base by clicking the item
                        if (!isBaseCurrency) {
                            viewModel.setBaseCurrency(currency.code)
                        }
                    }
                )
            }

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
    val usd = Currency("USD", "United States Dollar", "$")
    val eur = Currency("EUR", "Euro", "€")
    val jpy = Currency("JPY", "Japanese Yen", "¥")

    val fakeApi = object : CurrencyApi {
        override suspend fun getExchangeRates(baseCurrency: String): Response<ExchangeRateResponse> {
            return Response.success(ExchangeRateResponse(baseCurrency, mapOf("EUR" to 0.9, "JPY" to 130.0, "USD" to 1.0)))
        }
    }

    val fakeRepository = object : CurrencyRepository(fakeApi) {
        private val _selectedCurrencies = MutableStateFlow(listOf(usd, eur, jpy))
        override val selectedCurrencies: StateFlow<List<Currency>> = _selectedCurrencies.asStateFlow()
        private val _exchangeRates = MutableStateFlow(mapOf("EUR" to 0.9, "JPY" to 130.0, "USD" to 1.0))
        override val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()
        override val isLoading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
        override val error: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
        override suspend fun fetchExchangeRates(baseCurrency: String) { /* No-op for preview */ }
        override fun addCurrency(currency: Currency) {
            _selectedCurrencies.value = (_selectedCurrencies.value + currency).distinctBy { it.code }
        }
        override fun removeCurrency(currency: Currency) {
            _selectedCurrencies.value = _selectedCurrencies.value.filterNot { it.code == currency.code }
        }
        override fun getAvailableCurrencies(): List<Currency> = listOf(Currency("GBP", "British Pound", "£"))
        override fun clearError() {}
    }

    val decimalFormat = DecimalFormat("#,##0.00")
    val fakeViewModel = object : CurrencyConverterViewModel(fakeRepository) {
        override val selectedBaseCurrencyCode: StateFlow<String> = MutableStateFlow("USD").asStateFlow()
        override val currencyTextFieldValues: StateFlow<Map<String, TextFieldValue>> = 
            MutableStateFlow(
                mapOf(
                    "USD" to TextFieldValue(decimalFormat.format(100.0)),
                    "EUR" to TextFieldValue(decimalFormat.format(90.0)),
                    "JPY" to TextFieldValue(decimalFormat.format(13000.0))
                )
            ).asStateFlow()
        override fun onCurrencyAmountChanged(currencyCode: String, newTfv: TextFieldValue) { /* No-op for preview */ }
        override fun setBaseCurrency(newBaseCode: String) { /* No-op for preview */ }
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
