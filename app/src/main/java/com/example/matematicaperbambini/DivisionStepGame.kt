package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.random.Random


private enum class DivMode { ONE_DIGIT, TWO_DIGIT }

private data class DivConfig(
    val dividendDigitsRange: IntRange,
    val divisorRange: IntRange
)

private fun configFor(mode: DivMode): DivConfig = when (mode) {
    DivMode.ONE_DIGIT -> DivConfig(dividendDigitsRange = 2..4, divisorRange = 2..9)
    DivMode.TWO_DIGIT -> DivConfig(dividendDigitsRange = 3..4, divisorRange = 10..79)
}

private fun pow10(exp: Int): Int = 10.0.pow(exp.toDouble()).toInt()

private fun generateDivision(rng: Random, mode: DivMode): Pair<Int, Int> {
    val config = configFor(mode)
    val digitsRange = config.dividendDigitsRange
    repeat(200) {
        val digits = rng.nextInt(digitsRange.first, digitsRange.last + 1)
        val minDividend = pow10(digits - 1)
        val maxDividend = pow10(digits) - 1
        val dividend = rng.nextInt(minDividend, maxDividend + 1)
        val divisor = rng.nextInt(config.divisorRange.first, config.divisorRange.last + 1)
        val plan = generateDivisionPlan(dividend, divisor)
        val nonTrivial = plan.steps.size >= 2 || plan.finalQuotient >= 10
        val remainderNonZero = plan.finalRemainder != 0
        if (nonTrivial || remainderNonZero) {
            return dividend to divisor
        }
    }

    val fallbackDigits = digitsRange.first
    val fallbackDividend = pow10(fallbackDigits - 1)
    return fallbackDividend to config.divisorRange.first
}

@Composable
fun DivisionStepGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var mode by remember { mutableStateOf(DivMode.ONE_DIGIT) }

    fun newPlan(): DivisionPlan {
        val (dividend, divisor) = generateDivision(rng, mode)
        return generateDivisionPlan(dividend, divisor)
    }

    var plan by remember { mutableStateOf(newPlan()) }

    var targetIndex by remember(plan) { mutableStateOf(0) }
    val currentTarget = plan.targets.getOrNull(targetIndex)
    val done = currentTarget == null

    var correctCount by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val quotientSlotCount = plan.dividendDigits.size
    val quotientInputs = remember(plan) { List(quotientSlotCount) { mutableStateOf("") } }
    val quotientErrors = remember(plan) { List(quotientSlotCount) { mutableStateOf(false) } }
    val productInputs = remember(plan) {
        plan.steps.map { step ->
            List(step.product.toString().length) { mutableStateOf("") }
        }
    }
    val productErrors = remember(plan) {
        plan.steps.map { step ->
            List(step.product.toString().length) { mutableStateOf(false) }
        }
    }
    val remainderInputs = remember(plan) {
        plan.steps.map { step ->
            List(step.remainder.toString().length) { mutableStateOf("") }
        }
    }
    val remainderErrors = remember(plan) {
        plan.steps.map { step ->
            List(step.remainder.toString().length) { mutableStateOf(false) }
        }
    }
    val bringDownDone = remember(plan) { List(plan.steps.size) { mutableStateOf(false) } }

    fun resetSame() {
        targetIndex = 0
        message = null
        showSuccessDialog = false
        quotientInputs.forEachIndexed { idx, state ->
            state.value = ""
            quotientErrors[idx].value = false
        }
        productInputs.forEachIndexed { si, row ->
            row.forEachIndexed { idx, state ->
                state.value = ""
                productErrors[si][idx].value = false
            }
        }
        remainderInputs.forEachIndexed { si, row ->
            row.forEachIndexed { idx, state ->
                state.value = ""
                remainderErrors[si][idx].value = false
            }
        }
        bringDownDone.forEach { it.value = false }
    }

    fun resetNew() {
        plan = newPlan()
        targetIndex = 0
        message = null
        showSuccessDialog = false
    }

    fun playCorrect() {
        if (soundEnabled) fx.correct()
    }

    fun playWrong() {
        if (soundEnabled) fx.wrong()
    }

    fun advanceTarget() {
        targetIndex++
        if (targetIndex >= plan.targets.size) {
            correctCount++
            message = "✅ Finito! Quoziente ${plan.finalQuotient} resto ${plan.finalRemainder}"
        }
    }

    fun updateCellError(target: DivisionTarget, isError: Boolean) {
        when (target.type) {
            DivisionTargetType.QUOTIENT -> quotientErrors[target.idx].value = isError
            DivisionTargetType.PRODUCT -> productErrors[target.stepIndex][target.idx].value = isError
            DivisionTargetType.REMAINDER -> remainderErrors[target.stepIndex][target.idx].value = isError
            DivisionTargetType.BRING_DOWN -> Unit
        }
    }

    fun updateCellValue(target: DivisionTarget, value: String) {
        when (target.type) {
            DivisionTargetType.QUOTIENT -> quotientInputs[target.idx].value = value
            DivisionTargetType.PRODUCT -> productInputs[target.stepIndex][target.idx].value = value
            DivisionTargetType.REMAINDER -> remainderInputs[target.stepIndex][target.idx].value = value
            DivisionTargetType.BRING_DOWN -> Unit
        }
    }

    fun onDigitInput(target: DivisionTarget, value: String) {
        val activeTarget = currentTarget ?: return
        if (activeTarget != target) return
        val digit = value.firstOrNull() ?: return
        val expected = activeTarget.expected ?: return

        updateCellValue(activeTarget, digit.toString())
        if (digit != expected) {
            updateCellError(activeTarget, true)
            updateCellValue(activeTarget, "")
            message = "❌ Riprova"
            playWrong()
            return
        }

        updateCellError(activeTarget, false)
        message = null
        playCorrect()
        advanceTarget()
    }

    fun onBringDown(target: DivisionTarget) {
        val activeTarget = currentTarget ?: return
        if (activeTarget != target || activeTarget.type != DivisionTargetType.BRING_DOWN) return
        bringDownDone[target.stepIndex].value = true
        message = null
        playCorrect()
        advanceTarget()
    }

    fun fillSolution() {
        plan.steps.forEachIndexed { si, step ->
            val quotientCol = step.endPos
            quotientInputs[quotientCol].value = step.qDigit.toString()
            quotientErrors[quotientCol].value = false
            step.product.toString().forEachIndexed { idx, ch ->
                productInputs[si][idx].value = ch.toString()
                productErrors[si][idx].value = false
            }
            step.remainder.toString().forEachIndexed { idx, ch ->
                remainderInputs[si][idx].value = ch.toString()
                remainderErrors[si][idx].value = false
            }
            if (step.bringDownDigit != null) {
                bringDownDone[si].value = true
            }
        }
        targetIndex = plan.targets.size
        message = "✅ Soluzione completata! Quoziente ${plan.finalQuotient} resto ${plan.finalRemainder}"
        showSuccessDialog = true
    }

    val hint = if (done) {
        "Bravo! Quoziente ${plan.finalQuotient} con resto ${plan.finalRemainder}."
    } else {
        currentTarget!!.hint
    }

    LaunchedEffect(done) {
        if (done) {
            showSuccessDialog = true
        }
    }

    val activeStepNumber = currentTarget?.stepIndex?.plus(1) ?: plan.steps.size
    val ui = rememberUiSizing()

    val digitW = if (ui.isCompact) 36.dp else 44.dp
    val digitH = if (ui.isCompact) 48.dp else 56.dp
    val digitSmallW = if (ui.isCompact) 34.dp else 40.dp
    val digitSmallH = if (ui.isCompact) 46.dp else 52.dp
    val fontLarge = if (ui.isCompact) 18.sp else 22.sp
    val fontSmall = if (ui.isCompact) 16.sp else 20.sp
    val gap = if (ui.isCompact) 4.dp else 6.dp
    val columns = plan.dividendDigits.size
    val quotientDigitW = if (columns == 4) {
        if (ui.isCompact) 32.dp else 38.dp
    } else {
        digitW
    }
    val divisorDigits = plan.divisor.toString()
    val divisorWidth = digitW * divisorDigits.length + gap * (divisorDigits.length - 1)
    val dividerHeight = digitH + digitH + gap
    val stepGap = if (ui.isCompact) 6.dp else 8.dp

    val activeStep = currentTarget?.let { plan.steps[it.stepIndex] }
    val activePartialRange = activeStep?.let {
        val len = it.partial.toString().length
        startColForEnd(it.endPos, len)..it.endPos
    }
    val highlightDividend: (Int) -> Boolean = { col ->
        val type = currentTarget?.type
        val range = activePartialRange
        (type == DivisionTargetType.QUOTIENT || type == DivisionTargetType.REMAINDER) &&
            range != null && col in range
    }
    val highlightDivisor = currentTarget?.type == DivisionTargetType.QUOTIENT ||
        currentTarget?.type == DivisionTargetType.PRODUCT
    val highlightQuotientCol = if (currentTarget?.type == DivisionTargetType.PRODUCT) {
        activeStep?.endPos
    } else {
        null
    }

    Box(Modifier.fillMaxSize()) {
        GameScreenFrame(
            title = "Divisioni passo passo",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = hint,
            ui = ui,
            message = message,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                    SeaGlassPanel(title = "Modalità") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val oneDigitSelected = mode == DivMode.ONE_DIGIT
                            val twoDigitSelected = mode == DivMode.TWO_DIGIT
                            if (oneDigitSelected) {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        mode = DivMode.ONE_DIGIT
                                        resetNew()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Divisore 1 cifra")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        mode = DivMode.ONE_DIGIT
                                        resetNew()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Divisore 1 cifra")
                                }
                            }
                            if (twoDigitSelected) {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        mode = DivMode.TWO_DIGIT
                                        resetNew()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Divisore 2 cifre")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        mode = DivMode.TWO_DIGIT
                                        resetNew()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Divisore 2 cifre")
                                }
                            }
                        }
                    }

                    SeaGlassPanel(title = "Calcolo") {
                        Column(verticalArrangement = Arrangement.spacedBy(stepGap)) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(stepGap)) {
                                    DivisionDigitRow(
                                        columns = columns,
                                        cellW = digitW,
                                        cellH = digitH,
                                        gap = gap
                                    ) { col ->
                                        val digit = plan.dividendDigits[col]
                                        DivisionFixedDigit(
                                            text = digit.toString(),
                                            w = digitW,
                                            h = digitH,
                                            fontSize = fontLarge,
                                            highlight = highlightDividend(col)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .width(if (ui.isCompact) 2.dp else 3.dp)
                                        .height(dividerHeight)
                                        .background(MaterialTheme.colorScheme.primary)
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                        divisorDigits.forEach { ch ->
                                            DivisionFixedDigit(
                                                text = ch.toString(),
                                                w = digitW,
                                                h = digitH,
                                                fontSize = fontLarge,
                                                highlight = highlightDivisor
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(divisorWidth)
                                            .height(if (ui.isCompact) 2.dp else 3.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    DivisionDigitRow(
                                        columns = columns,
                                        cellW = quotientDigitW,
                                        cellH = digitH,
                                        gap = gap
                                    ) { col ->
                                        val target = plan.targets.firstOrNull {
                                            it.type == DivisionTargetType.QUOTIENT && it.gridCol == col
                                        }
                                        if (target != null) {
                                            val active = target == currentTarget
                                            DivisionDigitBox(
                                                value = quotientInputs[col].value,
                                                enabled = active,
                                                active = active,
                                                isError = quotientErrors[col].value,
                                                highlight = highlightQuotientCol == col,
                                                microLabel = target.microLabel,
                                                onValueChange = { onDigitInput(target, it) },
                                                w = quotientDigitW,
                                                h = digitH,
                                                fontSize = fontLarge
                                            )
                                        } else {
                                            DivisionDigitBox(
                                                value = "",
                                                enabled = false,
                                                active = false,
                                                isError = false,
                                                highlight = highlightQuotientCol == col,
                                                onValueChange = {},
                                                w = quotientDigitW,
                                                h = digitH,
                                                fontSize = fontLarge
                                            )
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(stepGap)) {
                                plan.steps.forEachIndexed { si, step ->
                                    val productTargets = plan.targets.filter {
                                        it.type == DivisionTargetType.PRODUCT && it.stepIndex == si
                                    }
                                    val remainderTargets = plan.targets.filter {
                                        it.type == DivisionTargetType.REMAINDER && it.stepIndex == si
                                    }
                                    val bringDownTarget = plan.targets.firstOrNull {
                                        it.type == DivisionTargetType.BRING_DOWN && it.stepIndex == si
                                    }
                                    val highlightProduct = currentTarget?.type == DivisionTargetType.REMAINDER &&
                                        currentTarget?.stepIndex == si
                                    val highlightRemainder = currentTarget?.type == DivisionTargetType.BRING_DOWN &&
                                        currentTarget?.stepIndex == si

                                    DivisionDigitRow(
                                        columns = columns,
                                        cellW = digitSmallW,
                                        cellH = digitSmallH,
                                        gap = gap
                                    ) { col ->
                                        val target = productTargets.firstOrNull { it.gridCol == col }
                                        if (target != null) {
                                            val active = target == currentTarget
                                            DivisionDigitBox(
                                                value = productInputs[si][target.idx].value,
                                                enabled = active,
                                                active = active,
                                                isError = productErrors[si][target.idx].value,
                                                highlight = highlightProduct,
                                                microLabel = target.microLabel,
                                                onValueChange = { onDigitInput(target, it) },
                                                w = digitSmallW,
                                                h = digitSmallH,
                                                fontSize = fontSmall
                                            )
                                        }
                                    }

                                    DivisionDigitRow(
                                        columns = columns,
                                        cellW = digitSmallW,
                                        cellH = digitSmallH,
                                        gap = gap
                                    ) { col ->
                                        val target = remainderTargets.firstOrNull { it.gridCol == col }
                                        if (target != null) {
                                            val active = target == currentTarget
                                            DivisionDigitBox(
                                                value = remainderInputs[si][target.idx].value,
                                                enabled = active,
                                                active = active,
                                                isError = remainderErrors[si][target.idx].value,
                                                highlight = highlightRemainder,
                                                microLabel = target.microLabel,
                                                onValueChange = { onDigitInput(target, it) },
                                                w = digitSmallW,
                                                h = digitSmallH,
                                                fontSize = fontSmall
                                            )
                                        }
                                    }

                                    if (step.bringDownDigit != null && bringDownTarget != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(gap)
                                        ) {
                                            DivisionDigitRow(
                                                columns = columns,
                                                cellW = digitSmallW,
                                                cellH = digitSmallH,
                                                gap = gap
                                            ) { col ->
                                                val remainderTarget = remainderTargets.firstOrNull { it.gridCol == col }
                                                when {
                                                    remainderTarget != null -> DivisionFixedDigit(
                                                        text = remainderInputs[si][remainderTarget.idx].value,
                                                        w = digitSmallW,
                                                        h = digitSmallH,
                                                        fontSize = fontSmall,
                                                        highlight = highlightRemainder
                                                    )
                                                    col == bringDownTarget.gridCol -> {
                                                        val active = bringDownTarget == currentTarget
                                                        DivisionActionDigit(
                                                            text = step.bringDownDigit.toString(),
                                                            active = active,
                                                            microLabel = bringDownTarget.microLabel,
                                                            w = digitSmallW,
                                                            h = digitSmallH,
                                                            fontSize = fontSmall,
                                                            highlight = highlightRemainder
                                                        )
                                                    }
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { onBringDown(bringDownTarget) },
                                                enabled = bringDownTarget == currentTarget
                                            ) {
                                                Text(if (bringDownDone[si].value) "Abbassato" else "Abbassa")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SeaGlassPanel(title = "Aiuto") {
                        Column(verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)) {
                            val showDebug = true // <-- temporaneo, poi lo metti a false

                            if (showDebug) {
                                Text("DEBUG current = ...")
                            }

                            Text(
                                text = hint,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (done) "Passo ${plan.steps.size}/${plan.steps.size}"
                                else "Passo $activeStepNumber/${plan.steps.size}",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            bottomBar = {
                GameBottomActions(
                    leftText = "Ricomincia",
                    onLeft = { resetSame() },
                    rightText = "Nuovo",
                    onRight = { resetNew() },
                    modifier = Modifier.fillMaxWidth(),
                    center = {
                        OutlinedButton(onClick = { fillSolution() }) {
                            Text("Soluzione")
                        }
                    }
                )
            }
        )

        SuccessDialog(
            show = showSuccessDialog,
            onNew = {
                showSuccessDialog = false
                resetNew()
            },
            onDismiss = { showSuccessDialog = false },
            resultText = "${plan.finalQuotient} r. ${plan.finalRemainder}"
        )
    }
}
