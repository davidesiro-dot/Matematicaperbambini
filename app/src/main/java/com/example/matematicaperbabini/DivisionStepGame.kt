package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private enum class DivRow { QUOTIENT, PRODUCT, REMAINDER }

private data class DivTarget(
    val row: DivRow,
    val stepIndex: Int,
    val col: Int,
    val expected: Char,
    val hint: String
)

private data class DivStep(
    val endPos: Int,
    val chunk: Int,
    val qDigit: Int,
    val product: Int,
    val remainder: Int
)

private data class DivPlan(
    val dividend: Int,
    val divisor: Int,
    val dividendDigits: List<Int>,
    val quotientDigits: List<Int?>,
    val steps: List<DivStep>,
    val targets: List<DivTarget>,
    val finalQuotient: Int,
    val finalRemainder: Int
)

private fun generateDivision(rng: Random): Pair<Int, Int> {
    val divisor = rng.nextInt(2, 10)
    val digits = if (rng.nextBoolean()) 2 else 3
    val min = if (digits == 2) 10 else 100
    val max = if (digits == 2) 99 else 999
    val dividend = rng.nextInt(min, max + 1)
    return dividend to divisor
}

private fun computeDivisionPlan(dividend: Int, divisor: Int): DivPlan {
    val ds = dividend.toString().map { it.digitToInt() }
    val n = ds.size

    val steps = mutableListOf<DivStep>()
    val quotient = MutableList<Int?>(n) { null }

    var acc = 0
    var started = false
    var qValue = 0

    for (i in 0 until n) {
        acc = acc * 10 + ds[i]

        if (!started && acc < divisor) continue

        started = true
        val qDigit = acc / divisor
        val product = qDigit * divisor
        val rem = acc - product

        quotient[i] = qDigit
        steps += DivStep(i, acc, qDigit, product, rem)

        acc = rem
        qValue = qValue * 10 + qDigit
    }

    val finalQuotient = if (qValue == 0) 0 else qValue
    val finalRemainder = acc

    val targets = mutableListOf<DivTarget>()
    fun add(row: DivRow, stepIndex: Int, col: Int, expected: Char, hint: String) {
        targets += DivTarget(row, stepIndex, col, expected, hint)
    }

    steps.forEachIndexed { si, st ->
        val qCh = st.qDigit.toString()[0]
        add(
            row = DivRow.QUOTIENT,
            stepIndex = si,
            col = 0,
            expected = qCh,
            hint = "Trova il numero più grande che, moltiplicato per $divisor, dà un risultato ≤ ${st.chunk}. Scrivi la cifra del quoziente."
        )

        val prodStr = st.product.toString()
        for (k in prodStr.indices.reversed()) {
            add(
                row = DivRow.PRODUCT,
                stepIndex = si,
                col = k,
                expected = prodStr[k],
                hint = "Moltiplica: $divisor × ${st.qDigit} = ${st.product}. Scrivi il prodotto sotto le cifre selezionate."
            )
        }

        val remStr = st.remainder.toString()
        val nextDigit = ds.getOrNull(st.endPos + 1)
        val remainderHint = buildString {
            append("Sottrai: ${st.chunk} − ${st.product} = ${st.remainder}. Scrivi il resto.")
            if (nextDigit != null) append(" Poi abbassa la cifra successiva ($nextDigit) e ripeti.")
        }
        for (k in remStr.indices.reversed()) {
            add(
                row = DivRow.REMAINDER,
                stepIndex = si,
                col = k,
                expected = remStr[k],
                hint = remainderHint
            )
        }
    }

    return DivPlan(
        dividend = dividend,
        divisor = divisor,
        dividendDigits = ds,
        quotientDigits = quotient,
        steps = steps,
        targets = targets,
        finalQuotient = finalQuotient,
        finalRemainder = finalRemainder
    )
}

@Composable
private fun DigitBox(
    value: String,
    enabled: Boolean,
    active: Boolean,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    w: Dp = 44.dp,
    h: Dp = 56.dp,
    fontSize: Int = 22
) {
    val shape = RoundedCornerShape(12.dp)

    val bg = when {
        active -> Color(0xFFFFF3CC)
        value.isNotBlank() && !isError -> Color(0xFFDCFCE7)
        else -> Color.White.copy(alpha = 0.85f)
    }

    val border = when {
        isError -> Color(0xFFEF4444)
        active -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(if (active) 3.dp else 2.dp, border, shape),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val d = it.filter { ch -> ch.isDigit() }.takeLast(1)
                onValueChange(d)
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF111827)
            ),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            }
        )
    }
}
