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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.di.AppModule
import com.lukesleeman.currencyconverter.ui.components.AddCurrencyDialog
import com.lukesleeman.currencyconverter.ui.components.CurrencyItem
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme
import com.lukesleeman.currencyconverter.viewmodel.CurrencyConverterViewModel

/**
 * Main Currency Converter Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    viewModel: CurrencyConverterViewModel = remember { AppModule.provideCurrencyConverterViewModel() }
) {
    val selectedCurrencies by viewModel.selectedCurrencies.collectAsState()
    val currencyTextFieldValues by viewModel.currencyTextFieldValues.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    CurrencyConverterScreenContent(
        selectedCurrencies = selectedCurrencies,
        currencyTextFieldValues = currencyTextFieldValues,
        isLoading = isLoading,
        error = error,
        onAmountChange = viewModel::onCurrencyAmountChanged,
        onRefresh = viewModel::refreshExchangeRates,
        onAddCurrency = viewModel::addCurrency,
        onClearError = viewModel::clearError,
        getAvailableCurrencies = viewModel::getAvailableCurrencies
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterScreenContent(
    selectedCurrencies: List<Currency>,
    currencyTextFieldValues: Map<String, TextFieldValue>,
    isLoading: Boolean,
    error: String?,
    onAmountChange: (String, TextFieldValue) -> Unit,
    onRefresh: () -> Unit,
    onAddCurrency: (Currency) -> Unit,
    onClearError: () -> Unit,
    getAvailableCurrencies: () -> List<Currency>
) {
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
                IconButton(onClick = onRefresh) {
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
                        onClick = onClearError
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

                CurrencyItem(
                    currency = currency,
                    amount = textFieldValue,
                    onAmountChange = { newTfv ->
                        onAmountChange(currency.code, newTfv)
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
            availableCurrencies = getAvailableCurrencies(),
            onCurrencySelected = { currency ->
                onAddCurrency(currency)
            },
            onDismiss = { showAddCurrencyDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyConverterScreenPreview() {
    val selectedCurrencies = listOf(
        Currency("USD", "United States Dollar", "$"),
        Currency("EUR", "Euro", "€"),
        Currency("JPY", "Japanese Yen", "¥")
    )

    val currencyValues = mapOf(
        "USD" to TextFieldValue("100.00"),
        "EUR" to TextFieldValue("84.75"),
        "JPY" to TextFieldValue("10,967.00")
    )

    val availableCurrencies = listOf(
        Currency("GBP", "British Pound", "£"),
        Currency("CAD", "Canadian Dollar", "C$")
    )

    CurrencyConverterTheme {
        CurrencyConverterScreenContent(
            selectedCurrencies = selectedCurrencies,
            currencyTextFieldValues = currencyValues,
            isLoading = false,
            error = null,
            onAmountChange = { _, _ -> },
            onRefresh = { },
            onAddCurrency = { },
            onClearError = { },
            getAvailableCurrencies = { availableCurrencies }
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CurrencyConverterScreenDarkPreview() {
    CurrencyConverterScreenPreview()
}
