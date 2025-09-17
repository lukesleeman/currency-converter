package com.lukesleeman.currencyconverter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukesleeman.currencyconverter.ui.theme.CurrencyConverterTheme

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: 7, 8, 9
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumberButton("7", onNumberClick, Modifier.weight(1f))
                NumberButton("8", onNumberClick, Modifier.weight(1f))
                NumberButton("9", onNumberClick, Modifier.weight(1f))
            }

            // Second row: 4, 5, 6
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumberButton("4", onNumberClick, Modifier.weight(1f))
                NumberButton("5", onNumberClick, Modifier.weight(1f))
                NumberButton("6", onNumberClick, Modifier.weight(1f))
            }

            // Third row: 1, 2, 3
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumberButton("1", onNumberClick, Modifier.weight(1f))
                NumberButton("2", onNumberClick, Modifier.weight(1f))
                NumberButton("3", onNumberClick, Modifier.weight(1f))
            }

            // Fourth row: 0, ., backspace
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumberButton("0", onNumberClick, Modifier.weight(1f))
                ActionButton(
                    text = ".",
                    onClick = onDecimalClick,
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                ActionButton(
                    text = "⌫",
                    onClick = onBackspaceClick,
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun NumberButton(
    number: String,
    onNumberClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onNumberClick(number) },
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = number,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NumericKeyboardPreview() {
    CurrencyConverterTheme {
        NumericKeyboard(
            onNumberClick = { },
            onDecimalClick = { },
            onBackspaceClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NumericKeyboardDarkPreview() {
    NumericKeyboardPreview()
}