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

        if (index >= n - 1) break

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

        val divisorHL = divisorDigits.indices.map { HLCell(HLZone.DIVISOR, si, it) }

        fun dividendHL(range: IntRange) = range.map { c -> HLCell(HLZone.DIVIDEND, si, c) }
        fun productHL(range: IntRange) = range.map { c -> HLCell(HLZone.PRODUCT, si, c) }
        fun remainderHL(range: IntRange) = range.map { c -> HLCell(HLZone.REMAINDER, si, c) }

        /**
         * FIX IMPORTANTISSIMA:
         * - step 0: parziale = cifre del DIVIDENDO (highlight DIVIDEND)
         * - step >= 1: parziale = RESTO step precedente + CIFRA ABBASSATA step precedente (highlight REMAINDER + BRING)
         */
        fun partialSourceHL(siLocal: Int): List<HLCell> {
            if (siLocal == 0) return dividendHL(partialRange)

            val prev = steps[siLocal - 1]
            val prevRemStr = prev.remainder.toString()
            val prevRemStart = startColForEnd(prev.endPos, prevRemStr.length)
            val prevRemRange = prevRemStart..prev.endPos
            val bringCol = prev.bringDownDigit?.let { prev.endPos + 1 }

            val out = mutableListOf<HLCell>()
            // resto precedente (sulla riga REMAINDER dello step precedente)
            prevRemRange.forEach { c -> out += HLCell(HLZone.REMAINDER, siLocal - 1, c) }
            // cifra abbassata (sulla "colonna bring" dello step precedente)
            if (bringCol != null) out += HLCell(HLZone.BRING, siLocal - 1, bringCol)
            return out
        }

        val parzialeHL = partialSourceHL(si)

        // --- QUOTIENT target (una cifra) ---
        val qChar = step.qDigit.toString()[0]
        add(
            type = DivisionTargetType.QUOTIENT,
            stepIndex = si,
            gridCol = quotientCol,
            idx = quotientCol,
            expected = qChar,
            hint = "Trova la cifra del quoziente: il numero più grande che, moltiplicato per $divisor, dà un risultato ≤ ${step.partial}.",
            microLabel = "${step.partial}÷$divisor",
            highlights = parzialeHL + divisorHL + HLCell(HLZone.QUOTIENT, si, quotientCol)
        )

        // --- PRODUCT targets (cifre del prodotto) ---
        val productHint = "Moltiplica: $divisor × ${step.qDigit} = ${step.product}. Scrivi il prodotto sotto le cifre selezionate."
        val productHighlights = parzialeHL + divisorHL + HLCell(HLZone.QUOTIENT, si, quotientCol) + productHL(productRange)
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

        // --- REMAINDER targets (cifre del resto) ---
        val remainderHint = "Sottrai: ${step.partial} − ${step.product} = ${step.remainder}. Scrivi il resto."
        val remainderHighlights = productHL(productRange) + remainderHL(remainderRange)
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

        // --- BRING DOWN target (azione) ---
        if (step.bringDownDigit != null) {
            val bringCol = step.endPos + 1
            val bringDownHint =
                "Abbassa la cifra successiva (${step.bringDownDigit}) accanto al resto per formare il nuovo parziale."
            add(
                type = DivisionTargetType.BRING_DOWN,
                stepIndex = si,
                gridCol = bringCol,
                idx = 0,
                expected = null,
                hint = bringDownHint,
                microLabel = "↓ ${step.bringDownDigit}",
                highlights = listOf(
                    HLCell(HLZone.BRING, si, bringCol)
                ) + remainderHL(remainderRange)
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
