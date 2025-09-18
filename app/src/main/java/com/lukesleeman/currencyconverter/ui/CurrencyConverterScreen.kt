package com.lukesleeman.currencyconverter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.lukesleeman.currencyconverter.ui.components.NumericKeyboard
import com.lukesleeman.currencyconverter.ui.state.CurrencyConverterUiState
import com.lukesleeman.currencyconverter.ui.state.CurrencyDisplayItem
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
    val uiState by viewModel.uiState.collectAsState()

    CurrencyConverterScreenContent(
        uiState = uiState,
        onSetActiveCurrency = viewModel::setActiveCurrency,
        onUpdateActiveFieldText = viewModel::updateActiveFieldText,
        onAddDigit = viewModel::addDigit,
        onAddDecimalPoint = viewModel::addDecimalPoint,
        onBackspace = viewModel::backspace,
        onAddCurrency = viewModel::addCurrency,
        onClearError = viewModel::clearError,
        getAvailableCurrencies = viewModel::getAvailableCurrencies
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterScreenContent(
    uiState: CurrencyConverterUiState,
    onSetActiveCurrency: (String) -> Unit,
    onUpdateActiveFieldText: (TextFieldValue) -> Unit,
    onAddDigit: (String) -> Unit,
    onAddDecimalPoint: () -> Unit,
    onBackspace: () -> Unit,
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Info about using default rates (shown when no recent API call was successful)
        if (!uiState.isLoading && uiState.error == null && uiState.currencies.isNotEmpty() && uiState.currencies.all { it.textFieldValue.text == "0.00" || it.textFieldValue.text == "0,00" }) {
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

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.error?.let { errorMessage ->
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.currencies, key = { it.currency.code }) { currencyDisplayItem ->
                CurrencyItem(
                    currency = currencyDisplayItem.currency,
                    amount = currencyDisplayItem.textFieldValue,
                    onFocusRequest = {
                        onSetActiveCurrency(currencyDisplayItem.currency.code)
                    },
                    onValueChange = { newValue ->
                        onUpdateActiveFieldText(newValue)
                    },
                    isActive = currencyDisplayItem.currency.code == uiState.activeCurrency.currency.code
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

        // Custom Numeric Keyboard - Always visible
        if (uiState.currencies.isNotEmpty()) {
            NumericKeyboard(
                onNumberClick = { digit ->
                    onAddDigit(digit)
                },
                onDecimalClick = {
                    onAddDecimalPoint()
                },
                onBackspaceClick = {
                    onBackspace()
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(300.dp)
            )
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
    val currencies = listOf(
        Currency("USD", "United States Dollar", "$"),
        Currency("EUR", "Euro", "€"),
        Currency("JPY", "Japanese Yen", "¥")
    )

    val currencyDisplayItems = listOf(
        CurrencyDisplayItem(currencies[0], TextFieldValue("100.00")),
        CurrencyDisplayItem(currencies[1], TextFieldValue("84.75")),
        CurrencyDisplayItem(currencies[2], TextFieldValue("10,967.00"))
    )

    val uiState = CurrencyConverterUiState(
        currencies = currencyDisplayItems,
        activeCurrency = currencyDisplayItems[0],
        isLoading = false,
        error = null
    )

    val availableCurrencies = listOf(
        Currency("GBP", "British Pound", "£"),
        Currency("CAD", "Canadian Dollar", "C$")
    )

    CurrencyConverterTheme {
        CurrencyConverterScreenContent(
            uiState = uiState,
            onSetActiveCurrency = { },
            onUpdateActiveFieldText = { },
            onAddDigit = { },
            onAddDecimalPoint = { },
            onBackspace = { },
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
