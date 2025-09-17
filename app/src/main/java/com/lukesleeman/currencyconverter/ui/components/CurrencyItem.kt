package com.lukesleeman.currencyconverter.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme

/**
 * Composable for displaying a currency item with flag, code, and converted amount
 */
@Composable
fun CurrencyItem(
    currency: Currency,
    amount: TextFieldValue,
    modifier: Modifier = Modifier,
    onFocusRequest: (() -> Unit)? = null,
    isActive: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable {
                onFocusRequest?.invoke()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currency.flag,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .width(50.dp)
                    .padding(end = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f)) // This spacer will take up the flexible space to the left

            OutlinedTextField(
                value = amount,
                onValueChange = { },
                readOnly = true,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Default (Large Amount)")
@Preview(showBackground = true, name = "Wide Screen (Large Amount)", widthDp = 800, heightDp = 200)
@Preview(showBackground = true, name = "Large Font (Large Amount)", fontScale = 1.5f)
@Preview(showBackground = true, name = "Dark Mode (Large Amount)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CurrencyItemVariationsPreview() {
    var amount by remember { mutableStateOf(TextFieldValue("1234567890.12")) }
    val eur = Currency("EUR", "Euro", "€") // Using EUR as a sample currency
    CurrencyConverterTheme {
        // For the wide screen preview, CurrencyItem will naturally fill the width.
        // If specific narrower behavior on wide screens is needed for the item itself,
        // the parent composable calling CurrencyItem would constrain its width.
        CurrencyItem(
            currency = eur,
            amount = amount,
            onFocusRequest = { }
        )
    }
}
