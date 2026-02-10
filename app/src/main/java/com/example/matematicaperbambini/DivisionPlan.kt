package com.example.matematicaperbambini

enum class DivisionTargetType { QUOTIENT, PRODUCT, REMAINDER, BRING_DOWN }

enum class HLZone { DIVIDEND, DIVISOR, QUOTIENT, PRODUCT, REMAINDER, BRING }

data class HLCell(val zone: HLZone, val stepIndex: Int, val col: Int)

data class DivisionTarget(
    val type: DivisionTargetType,
    val stepIndex: Int,
    val gridCol: Int,
    val idx: Int,
    val expected: Char?,
    val hint: String,
    val microLabel: String?,
    val highlights: List<HLCell>
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
    require(dividend >= 0) { "Dividend must be >= 0" }
    require(divisor > 0) { "Divisor must be > 0" }

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
    val divisorDigits = divisor.toString()
    fun add(
        type: DivisionTargetType,
        stepIndex: Int,
        gridCol: Int,
        idx: Int,
        expected: Char?,
        hint: String,
        microLabel: String?,
        highlights: List<HLCell>
    ) {
        targets += DivisionTarget(
            type = type,
            stepIndex = stepIndex,
            gridCol = gridCol,
            idx = idx,
            expected = expected,
            hint = hint,
            microLabel = microLabel,
            highlights = highlights
        )
    }

    steps.forEachIndexed { si, step ->
        val qChar = step.qDigit.toString()[0]
        val quotientCol = step.endPos
        val partialStr = step.partial.toString()
        val partialStart = startColForEnd(step.endPos, partialStr.length)
        val partialRange = partialStart..step.endPos

        val productStr = step.product.toString()
        val productStart = startColForEnd(step.endPos, productStr.length)
        val productRange = productStart..step.endPos

        val remainderStr = step.remainder.toString()
        val remainderStart = startColForEnd(step.endPos, remainderStr.length)
        val remainderRange = remainderStart..step.endPos

        val bringCol = step.bringDownDigit?.let { step.endPos + 1 }
        val divisorHighlights = divisorDigits.indices.map { HLCell(HLZone.DIVISOR, si, it) }

        fun dividendHighlights(stepIndex: Int, range: IntRange) =
            range.map { HLCell(HLZone.DIVIDEND, stepIndex, it) }

        fun productHighlightCells(stepIndex: Int, range: IntRange) =
            range.map { HLCell(HLZone.PRODUCT, stepIndex, it) }

        fun remainderHighlightCells(stepIndex: Int, range: IntRange) =
            range.map { HLCell(HLZone.REMAINDER, stepIndex, it) }

        val partialHighlights = if (si == 0) {
            dividendHighlights(si, partialRange)
        } else {
            val prevStep = steps[si - 1]
            val prevRemainderStr = prevStep.remainder.toString()
            val prevRemainderStart = startColForEnd(prevStep.endPos, prevRemainderStr.length)
            val prevRemainderRange = prevRemainderStart..prevStep.endPos
            val prevBringCol = prevStep.bringDownDigit?.let { prevStep.endPos + 1 }
            val bringHighlight = prevBringCol?.let { listOf(HLCell(HLZone.BRING, si - 1, it)) }.orEmpty()
            remainderHighlightCells(si - 1, prevRemainderRange) + bringHighlight
        }

        add(
            type = DivisionTargetType.QUOTIENT,
            stepIndex = si,
            gridCol = quotientCol,
            idx = quotientCol,
            expected = qChar,
            hint = "Trova la cifra del quoziente: il numero più grande che, moltiplicato per $divisor, dà un risultato ≤ ${step.partial}.",
            microLabel = "${step.partial}÷$divisor",
            highlights = partialHighlights + divisorHighlights
        )

        val productHint = "Moltiplica: $divisor × ${step.qDigit} = ${step.product}. Scrivi il prodotto sotto le cifre selezionate."
        val productHighlights = divisorHighlights +
            HLCell(HLZone.QUOTIENT, si, quotientCol)
        productStr.forEachIndexed { idx, ch ->
            add(
                type = DivisionTargetType.PRODUCT,
                stepIndex = si,
                gridCol = productStart + idx,
                idx = idx,
                expected = ch,
                hint = productHint,
                microLabel = "$divisor×${step.qDigit}",
                highlights = productHighlights
            )
        }

        val remainderHint = "Sottrai: ${step.partial} − ${step.product} = ${step.remainder}. Scrivi il resto."
        val remainderHighlights = partialHighlights +
            productHighlightCells(si, productRange)
        remainderStr.forEachIndexed { idx, ch ->
            add(
                type = DivisionTargetType.REMAINDER,
                stepIndex = si,
                gridCol = remainderStart + idx,
                idx = idx,
                expected = ch,
                hint = remainderHint,
                microLabel = "${step.partial}−${step.product}",
                highlights = remainderHighlights
            )
        }

        if (step.bringDownDigit != null && bringCol != null) {
            val bringDownHint = "Abbassa la cifra successiva (${step.bringDownDigit}) accanto al resto per formare il nuovo parziale."
            add(
                type = DivisionTargetType.BRING_DOWN,
                stepIndex = si,
                gridCol = bringCol,
                idx = 0,
                expected = null,
                hint = bringDownHint,
                microLabel = "↓ ${step.bringDownDigit}",
                highlights = listOf(
                    HLCell(HLZone.DIVIDEND, si, bringCol),
                    HLCell(HLZone.BRING, si, bringCol)
                ) + remainderHighlightCells(si, remainderRange)
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
