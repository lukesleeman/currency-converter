package com.lukesleeman.currencyconverter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.data.CurrencyUtils
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme

/**
 * Dialog for selecting and adding new currencies
 */
@Composable
fun AddCurrencyDialog(
    availableCurrencies: List<Currency>,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit,
    filterCurrencies: (List<Currency>, String) -> List<Currency> = CurrencyUtils::filterCurrencies
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter currencies based on search query
    val filteredCurrencies = remember(availableCurrencies, searchQuery, filterCurrencies) {
        filterCurrencies(availableCurrencies, searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f), // Use 80% of screen height
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Currency",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = { Text("Search currencies...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                // Currency list
                when {
                    availableCurrencies.isEmpty() -> {
                        Text(
                            text = "All available currencies are already added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    filteredCurrencies.isEmpty() -> {
                        Text(
                            text = "No currencies match your search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "${filteredCurrencies.size} currencies available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredCurrencies) { currency ->
                                CurrencySelectionItem(
                                    currency = currency,
                                    onClick = {
                                        onCurrencySelected(currency)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

/**
 * Individual currency item in the selection dialog
 */
@Composable
private fun CurrencySelectionItem(
    currency: Currency,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency.flag,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column {
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Preview of AddCurrencyDialog with available currencies
 */
@Preview(showBackground = true)
@Composable
fun PreviewAddCurrencyDialogWithItems() {
    CurrencyConverterTheme {
        AddCurrencyDialog(
            availableCurrencies = listOf(
                Currency("USD", "US Dollar", "🇺🇸"),
                Currency("EUR", "Euro", "🇪🇺"),
                Currency("JPY", "Japanese Yen", "🇯🇵"),
                Currency("GBP", "British Pound", "🇬🇧"),
                Currency("CAD", "Canadian Dollar", "🇨🇦"),
                Currency("AUD", "Australian Dollar", "🇦🇺"),
                Currency("CHF", "Swiss Franc", "🇨🇭"),
                Currency("CNY", "Chinese Yuan", "🇨🇳"),
                Currency("INR", "Indian Rupee", "🇮🇳"),
                Currency("KRW", "South Korean Won", "🇰🇷"),
                Currency("MXN", "Mexican Peso", "🇲🇽"),
                Currency("BRL", "Brazilian Real", "🇧🇷"),
                Currency("RUB", "Russian Ruble", "🇷🇺"),
                Currency("ZAR", "South African Rand", "🇿🇦"),
                Currency("SEK", "Swedish Krona", "🇸🇪")
            ),
            onCurrencySelected = {},
            onDismiss = {}
        )
    }
}

/**
 * Preview of AddCurrencyDialog with no available currencies
 */
@Preview(showBackground = true)
@Composable
fun PreviewAddCurrencyDialogEmpty() {
    CurrencyConverterTheme {
        AddCurrencyDialog(
            availableCurrencies = emptyList(),
            onCurrencySelected = {},
            onDismiss = {}
        )
    }
}

/**
 * Preview of AddCurrencyDialog showing search functionality
 */
@Preview(showBackground = true, name = "Large Currency List")
@Composable
fun PreviewAddCurrencyDialogLarge() {
    val largeCurrencyList = listOf(
        Currency("AED", "UAE Dirham", "🇦🇪"),
        Currency("AFN", "Afghan Afghani", "🇦🇫"),
        Currency("ALL", "Albanian Lek", "🇦🇱"),
        Currency("AMD", "Armenian Dram", "🇦🇲"),
        Currency("AUD", "Australian Dollar", "🇦🇺"),
        Currency("BGN", "Bulgarian Lev", "🇧🇬"),
        Currency("BRL", "Brazilian Real", "🇧🇷"),
        Currency("CAD", "Canadian Dollar", "🇨🇦"),
        Currency("CHF", "Swiss Franc", "🇨🇭"),
        Currency("CNY", "Chinese Yuan", "🇨🇳"),
        Currency("CZK", "Czech Republic Koruna", "🇨🇿"),
        Currency("DKK", "Danish Krone", "🇩🇰"),
        Currency("EUR", "Euro", "🇪🇺"),
        Currency("GBP", "British Pound Sterling", "🇬🇧"),
        Currency("HKD", "Hong Kong Dollar", "🇭🇰"),
        Currency("HUF", "Hungarian Forint", "🇭🇺"),
        Currency("IDR", "Indonesian Rupiah", "🇮🇩"),
        Currency("ILS", "Israeli New Sheqel", "🇮🇱"),
        Currency("INR", "Indian Rupee", "🇮🇳"),
        Currency("JPY", "Japanese Yen", "🇯🇵"),
        Currency("KRW", "South Korean Won", "🇰🇷"),
        Currency("MXN", "Mexican Peso", "🇲🇽"),
        Currency("MYR", "Malaysian Ringgit", "🇲🇾"),
        Currency("NOK", "Norwegian Krone", "🇳🇴"),
        Currency("NZD", "New Zealand Dollar", "🇳🇿"),
        Currency("PHP", "Philippine Peso", "🇵🇭"),
        Currency("PLN", "Polish Zloty", "🇵🇱"),
        Currency("RON", "Romanian Leu", "🇷🇴"),
        Currency("RUB", "Russian Ruble", "🇷🇺"),
        Currency("SEK", "Swedish Krona", "🇸🇪"),
        Currency("SGD", "Singapore Dollar", "🇸🇬"),
        Currency("THB", "Thai Baht", "🇹🇭"),
        Currency("TRY", "Turkish Lira", "🇹🇷"),
        Currency("USD", "US Dollar", "🇺🇸"),
        Currency("ZAR", "South African Rand", "🇿🇦")
    )

    CurrencyConverterTheme {
        AddCurrencyDialog(
            availableCurrencies = largeCurrencyList,
            onCurrencySelected = {},
            onDismiss = {}
        )
    }
}

/**
 * Preview showing custom filter function (code-only search)
 */
@Preview(showBackground = true, name = "Custom Filter - Code Only")
@Composable
fun PreviewAddCurrencyDialogCustomFilter() {
    val currencies = listOf(
        Currency("USD", "US Dollar", "🇺🇸"),
        Currency("EUR", "Euro", "🇪🇺"),
        Currency("JPY", "Japanese Yen", "🇯🇵"),
        Currency("GBP", "British Pound Sterling", "🇬🇧"),
        Currency("CAD", "Canadian Dollar", "🇨🇦"),
        Currency("AUD", "Australian Dollar", "🇦🇺")
    )

    // Custom filter that only searches currency codes
    val codeOnlyFilter: (List<Currency>, String) -> List<Currency> = { currencyList, query ->
        if (query.isBlank()) {
            currencyList
        } else {
            currencyList.filter { it.code.contains(query, ignoreCase = true) }
        }
    }

    CurrencyConverterTheme {
        AddCurrencyDialog(
            availableCurrencies = currencies,
            onCurrencySelected = {},
            onDismiss = {},
            filterCurrencies = codeOnlyFilter
        )
    }
}