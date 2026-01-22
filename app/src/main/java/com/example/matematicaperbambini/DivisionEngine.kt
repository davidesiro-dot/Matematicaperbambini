package com.example.matematicaperbambini

internal enum class DivTargetType { QUOTIENT, PRODUCT, REMAINDER, BRING_DOWN }

internal data class DivTarget(
    val type: DivTargetType,
    val stepIndex: Int,
    val col: Int,
    val expected: Char?,
    val hint: String
)

internal data class DivStep(
    val endPos: Int,
    val partial: Int,
    val qDigit: Int,
    val product: Int,
    val remainder: Int,
    val bringDownDigit: Int?
)

internal data class DivPlan(
    val dividend: Int,
    val divisor: Int,
    val dividendDigits: List<Int>,
    val quotientDigits: List<Int>,
    val steps: List<DivStep>,
    val targets: List<DivTarget>,
    val finalQuotient: Int,
    val finalRemainder: Int
)

internal fun startColForEnd(endPos: Int, len: Int): Int {
    return (endPos - len + 1).coerceAtLeast(0)
}

internal fun estimateQuotientDigit(partial: Int, divisor: Int): Int {
    if (partial < divisor) return 0
    if (divisor < 10) return partial / divisor

    val leadingDivisor = divisor / 10
    val partialStr = partial.toString()
    val leadingPartial = if (partialStr.length >= 2) {
        partialStr.substring(0, 2).toInt()
    } else {
        partialStr.toInt()
    }
    var qDigit = minOf(9, leadingPartial / leadingDivisor)
    while (qDigit > 0 && divisor * qDigit > partial) {
        qDigit--
    }
    return qDigit
}

internal fun generateDivisionPlan(dividend: Int, divisor: Int): DivPlan {
    val ds = dividend.toString().map { it.digitToInt() }
    val n = ds.size

    val steps = mutableListOf<DivStep>()
    var index = 0
    var partial = ds.first()
    while (partial < divisor && index < n - 1) {
        index++
        partial = partial * 10 + ds[index]
    }

    while (true) {
        val qDigit = if (partial < divisor) 0 else estimateQuotientDigit(partial, divisor)
        val product = qDigit * divisor
        val rem = partial - product
        val bringDownDigit = if (index < n - 1) ds[index + 1] else null

        steps += DivStep(
            endPos = index,
            partial = partial,
            qDigit = qDigit,
            product = product,
            remainder = rem,
            bringDownDigit = bringDownDigit
        )

        if (index >= n - 1) {
            break
        }
        index++
        partial = rem * 10 + ds[index]
    }

    val quotientDigits = steps.map { it.qDigit }
    val quotientString = quotientDigits.joinToString("")
    val finalQuotient = quotientString.trimStart('0').ifEmpty { "0" }.toInt()
    val finalRemainder = steps.lastOrNull()?.remainder ?: 0

    val targets = mutableListOf<DivTarget>()
    fun add(type: DivTargetType, stepIndex: Int, col: Int, expected: Char?, hint: String) {
        targets += DivTarget(type, stepIndex, col, expected, hint)
    }

    steps.forEachIndexed { si, st ->
        val qCh = st.qDigit.toString()[0]
        add(
            type = DivTargetType.QUOTIENT,
            stepIndex = si,
            col = si,
            expected = qCh,
            hint = "Trova la cifra del quoziente: il numero più grande che, moltiplicato per $divisor, dà un risultato ≤ ${st.partial}."
        )

        val prodStr = st.product.toString()
        val prodStart = startColForEnd(st.endPos, prodStr.length)
        for (k in prodStr.indices) {
            add(
                type = DivTargetType.PRODUCT,
                stepIndex = si,
                col = prodStart + k,
                expected = prodStr[k],
                hint = "Moltiplica: $divisor × ${st.qDigit} = ${st.product}. Scrivi il prodotto sotto le cifre selezionate."
            )
        }

        val remStr = st.remainder.toString()
        val remStart = startColForEnd(st.endPos, remStr.length)
        val remainderHint = buildString {
            append("Sottrai: ${st.partial} − ${st.product} = ${st.remainder}. Scrivi il resto.")
        }
        for (k in remStr.indices) {
            add(
                type = DivTargetType.REMAINDER,
                stepIndex = si,
                col = remStart + k,
                expected = remStr[k],
                hint = remainderHint
            )
        }

        if (st.bringDownDigit != null) {
            val bringDownHint = "Abbassa la cifra successiva (${st.bringDownDigit}) accanto al resto per formare il nuovo parziale."
            add(
                type = DivTargetType.BRING_DOWN,
                stepIndex = si,
                col = st.endPos + 1,
                expected = null,
                hint = bringDownHint
            )
        }
    }

    return DivPlan(
        dividend = dividend,
        divisor = divisor,
        dividendDigits = ds,
        quotientDigits = quotientDigits,
        steps = steps,
        targets = targets,
        finalQuotient = finalQuotient,
        finalRemainder = finalRemainder
    )
}
