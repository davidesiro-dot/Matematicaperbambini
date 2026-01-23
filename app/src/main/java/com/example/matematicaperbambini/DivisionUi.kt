package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
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
fun DivisionDigitRow(
    columns: Int,
    cellW: Dp,
    cellH: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
    cell: @Composable (Int) -> Unit
) {
    val rowWidth = cellW * columns + gap * (columns - 1)
    Row(
        modifier = modifier.width(rowWidth),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        for (col in 0 until columns) {
            Box(
                modifier = Modifier.width(cellW).height(cellH),
                contentAlignment = Alignment.Center
            ) {
                cell(col)
            }
        }
    }
}

@Composable
fun DivisionFixedDigit(
    text: String,
    w: Dp,
    h: Dp,
    fontSize: TextUnit,
    highlight: Boolean = false,
    debugLabel: String? = null,
    debugMismatch: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val highlightColor = Color(0xFF22C55E)
    val borderColor = when {
        debugMismatch -> MaterialTheme.colorScheme.error
        highlight -> highlightColor
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderW = if (highlight || debugMismatch) 3.dp else 1.dp
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderW, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (debugLabel != null) {
            DivisionDebugBadge(text = debugLabel)
        }
    }
}

@Composable
fun DivisionActionDigit(
    text: String,
    active: Boolean,
    w: Dp,
    h: Dp,
    fontSize: TextUnit,
    highlight: Boolean = false,
    microLabel: String? = null,
    debugLabel: String? = null,
    debugMismatch: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = Color(0xFF22C55E)
    val border = when {
        debugMismatch -> MaterialTheme.colorScheme.error
        highlight -> highlightColor
        active -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderW = if (highlight || active || debugMismatch) 3.dp else 2.dp
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(borderW, border, shape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (active && microLabel != null) {
            androidx.compose.material3.Text(
                text = microLabel,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
        if (debugLabel != null) {
            DivisionDebugBadge(text = debugLabel)
        }
    }
}

@Composable
fun DivisionDigitBox(
    value: String,
    enabled: Boolean,
    active: Boolean,
    isError: Boolean,
    highlight: Boolean,
    w: Dp,
    h: Dp,
    fontSize: TextUnit,
    microLabel: String? = null,
    onValueChange: (String) -> Unit,
    debugLabel: String? = null,
    debugMismatch: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val focusRequester = remember { FocusRequester() }
    val highlightColor = Color(0xFF22C55E)

    val bg = when {
        active -> MaterialTheme.colorScheme.tertiaryContainer
        value.isNotBlank() && !isError -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val border = when {
        debugMismatch -> MaterialTheme.colorScheme.error
        isError -> MaterialTheme.colorScheme.error
        highlight -> highlightColor
        active -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderW = when {
        debugMismatch -> 3.dp
        isError -> 2.dp
        highlight || active -> 3.dp
        else -> 2.dp
    }

    LaunchedEffect(active, enabled) {
        if (active && enabled) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(borderW, border, shape)
            .clickable(enabled = enabled) { focusRequester.requestFocus() },
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
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable(enabled),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            }
        )
        if (active && microLabel != null) {
            androidx.compose.material3.Text(
                text = microLabel,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
        if (debugLabel != null) {
            DivisionDebugBadge(text = debugLabel)
        }
    }
}

@Composable
fun DivisionDebugBadge(text: String) {
    androidx.compose.material3.Text(
        text = text,
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 2.dp, end = 4.dp)
    )
}

@Composable
fun DivisionDebugCell(
    w: Dp,
    h: Dp,
    debugLabel: String,
    debugMismatch: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (debugMismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val borderW = if (debugMismatch) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .border(borderW, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        DivisionDebugBadge(text = debugLabel)
    }
}
