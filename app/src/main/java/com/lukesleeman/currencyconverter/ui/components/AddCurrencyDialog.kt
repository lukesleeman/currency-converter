package com.lukesleeman.currencyconverter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme

/**
 * Dialog for selecting and adding new currencies
 */
@Composable
fun AddCurrencyDialog(
    availableCurrencies: List<Currency>,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Add Currency",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (availableCurrencies.isEmpty()) {
                    Text(
                        text = "All available currencies are already added",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn {
                        items(availableCurrencies) { currency ->
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
                Currency("GBP", "British Pound", "🇬🇧")
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