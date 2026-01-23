package com.example.matematicaperbambini

enum class DivisionTargetType { QUOTIENT, PRODUCT, REMAINDER, BRING_DOWN }

data class DivisionTarget(
    val type: DivisionTargetType,
    val stepIndex: Int,
    val gridCol: Int,
    val idx: Int,
    val expected: Char?,
    val hint: String
)

data class DivisionStep(
    val endPos: Int,
    val partial: Int,
    val qDigit: Int,
    val product: Int,
    val remainder: Int,
    val bringDownDigit: Int?
)

data class DivisionPlan(
    val dividend: Int,
    val divisor: Int,
    val dividendDigits: List<Int>,
    val quotientDigits: List<Int>,
    val steps: List<DivisionStep>,
    val targets: List<DivisionTarget>,
    val finalQuotient: Int,
    val finalRemainder: Int
)

fun startColForEnd(endPos: Int, len: Int): Int {
    return (endPos - len + 1).coerceAtLeast(0)
}

fun estimateQuotientDigit(partial: Int, divisor: Int): Int {
    if (partial < divisor) return 0
    if (divisor < 10) return partial / divisor

    val divisorLeading = divisor / 10
    val partialStr = partial.toString()
    val partialLeading = if (partialStr.length >= 2) {
        partialStr.substring(0, 2).toInt()
    } else {
        partialStr.toInt()
    }
    var qDigit = minOf(9, partialLeading / divisorLeading)
    while (qDigit > 0 && divisor * qDigit > partial) {
        qDigit--
    }
    return qDigit
}

fun generateDivisionPlan(dividend: Int, divisor: Int): DivisionPlan {
    val digits = dividend.toString().map { it.digitToInt() }
    val n = digits.size

    val steps = mutableListOf<DivisionStep>()
    var index = 0
    var partial = digits.first()
    while (partial < divisor && index < n - 1) {
        index++
        partial = partial * 10 + digits[index]
    }

    while (true) {
        val qDigit = if (partial < divisor) 0 else estimateQuotientDigit(partial, divisor)
        val product = qDigit * divisor
        val remainder = partial - product
        val bringDownDigit = if (index < n - 1) digits[index + 1] else null

        steps += DivisionStep(
            endPos = index,
            partial = partial,
            qDigit = qDigit,
            product = product,
            remainder = remainder,
            bringDownDigit = bringDownDigit
        )

        if (index >= n - 1) {
            break
        }
        index++
        partial = remainder * 10 + digits[index]
    }

    val quotientDigits = steps.map { it.qDigit }
    val quotientString = quotientDigits.joinToString("")
    val finalQuotient = quotientString.trimStart('0').ifEmpty { "0" }.toInt()
    val finalRemainder = steps.lastOrNull()?.remainder ?: 0

    val targets = mutableListOf<DivisionTarget>()
    fun add(
        type: DivisionTargetType,
        stepIndex: Int,
        gridCol: Int,
        idx: Int,
        expected: Char?,
        hint: String
    ) {
        targets += DivisionTarget(
            type = type,
            stepIndex = stepIndex,
            gridCol = gridCol,
            idx = idx,
            expected = expected,
            hint = hint
        )
    }

    steps.forEachIndexed { si, step ->
        val qChar = step.qDigit.toString()[0]
        add(
            type = DivisionTargetType.QUOTIENT,
            stepIndex = si,
            gridCol = si,
            idx = si,
            expected = qChar,
            hint = "Trova la cifra del quoziente: il numero più grande che, moltiplicato per $divisor, dà un risultato ≤ ${step.partial}."
        )

        val productStr = step.product.toString()
        val productStart = startColForEnd(step.endPos, productStr.length)
        val productHint = "Moltiplica: $divisor × ${step.qDigit} = ${step.product}. Scrivi il prodotto sotto le cifre selezionate."
        productStr.forEachIndexed { idx, ch ->
            add(
                type = DivisionTargetType.PRODUCT,
                stepIndex = si,
                gridCol = productStart + idx,
                idx = idx,
                expected = ch,
                hint = productHint
            )
        }

        val remainderStr = step.remainder.toString()
        val remainderStart = startColForEnd(step.endPos, remainderStr.length)
        val remainderHint = "Sottrai: ${step.partial} − ${step.product} = ${step.remainder}. Scrivi il resto."
        remainderStr.forEachIndexed { idx, ch ->
            add(
                type = DivisionTargetType.REMAINDER,
                stepIndex = si,
                gridCol = remainderStart + idx,
                idx = idx,
                expected = ch,
                hint = remainderHint
            )
        }

        if (step.bringDownDigit != null) {
            val bringDownHint = "Abbassa la cifra successiva (${step.bringDownDigit}) accanto al resto per formare il nuovo parziale."
            add(
                type = DivisionTargetType.BRING_DOWN,
                stepIndex = si,
                gridCol = step.endPos + 1,
                idx = 0,
                expected = null,
                hint = bringDownHint
            )
        }
    }

    return DivisionPlan(
        dividend = dividend,
        divisor = divisor,
        dividendDigits = digits,
        quotientDigits = quotientDigits,
        steps = steps,
        targets = targets,
        finalQuotient = finalQuotient,
        finalRemainder = finalRemainder
    )
}
