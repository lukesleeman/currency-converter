package com.lukesleeman.currencyconverter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukesleeman.currencyconverter.data.Currency
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme
import java.text.DecimalFormat

/**
 * Composable for displaying a currency item with flag, code, and converted amount
 */
@Composable
fun CurrencyItem(
    currency: Currency,
    convertedAmount: Double,
    isBaseCurrency: Boolean = false,
    onBaseCurrencySelected: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val decimalFormat = DecimalFormat("#,##0.000")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { if (!isBaseCurrency) onBaseCurrencySelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBaseCurrency) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag emoji
            Text(
                text = currency.flag,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Currency code
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.width(50.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Converted amount
            Text(
                text = decimalFormat.format(convertedAmount),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isBaseCurrency) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )

            // Remove button (only show if not base currency and list has more than 1 item)
            if (!isBaseCurrency) {
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${currency.code}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}