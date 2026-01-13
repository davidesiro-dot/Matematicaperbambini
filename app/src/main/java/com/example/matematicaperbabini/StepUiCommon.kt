package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GridRowRight(
    signW: Dp,
    gap: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.wrapContentWidth(Alignment.End),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun SignCell(text: String, w: Dp) {
    Box(modifier = Modifier.width(w), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FixedDigit(ch: Char, w: Dp, h: Dp) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .width(w).height(h)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        val t = if (ch == ' ') "" else ch.toString()
        androidx.compose.material3.Text(
            t,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InputBox(
    value: String,
    enabled: Boolean,
    isActive: Boolean,
    isError: Boolean,
    w: Dp,
    h: Dp,
    fontSize: TextUnit,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        isActive -> MaterialTheme.colorScheme.tertiaryContainer
        value.isNotBlank() && !isError -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isActive -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderW = if (isActive) 3.dp else 2.dp

    Box(
        modifier = Modifier
            .width(w).height(h)
            .clip(shape)
            .background(bg)
            .border(borderW, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val cleaned = it.filter { ch -> ch.isDigit() }.takeLast(1)
                onValueChange(cleaned)
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            }
        )
    }
}
