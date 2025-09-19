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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.lukesleeman.currencyconverter.data.Currency
import kotlin.math.abs
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
    viewModel: CurrencyConverterViewModel = run {
        val context = LocalContext.current
        remember { AppModule.provideCurrencyConverterViewModel(context) }
    }
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

            // Status line showing last updated time with loading indicator
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = formatLastUpdatedText(uiState.lastUpdated, uiState.isLoading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        lastUpdated = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes ago
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
            getAvailableCurrencies = { availableCurrencies }
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CurrencyConverterScreenDarkPreview() {
    CurrencyConverterScreenPreview()
}

/**
 * Format the last updated text with time since last update
 */
private fun formatLastUpdatedText(lastUpdated: Long?, isLoading: Boolean): String {
    return when {
        isLoading -> "Updating rates..."
        lastUpdated == null -> "Rates last updated: never"
        else -> {
            val now = System.currentTimeMillis()
            val diffMinutes = (now - lastUpdated) / (1000 * 60)
            when {
                diffMinutes < 1 -> "Rates last updated: just now"
                diffMinutes == 1L -> "Rates last updated: 1 min ago"
                diffMinutes < 60 -> "Rates last updated: ${diffMinutes} mins ago"
                else -> {
                    val diffHours = diffMinutes / 60
                    if (diffHours == 1L) "Rates last updated: 1 hour ago"
                    else "Rates last updated: ${diffHours} hours ago"
                }
            }
        }
    }
}
