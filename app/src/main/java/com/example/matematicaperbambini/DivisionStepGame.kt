package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlin.math.pow
import kotlin.random.Random


private enum class DivMode { ONE_DIGIT, TWO_DIGIT }

private data class DivConfig(
    val dividendDigitsRange: IntRange,
    val divisorRange: IntRange
)

private fun configFor(mode: DivMode): DivConfig = when (mode) {
    DivMode.ONE_DIGIT -> DivConfig(dividendDigitsRange = 2..3, divisorRange = 2..9)
    DivMode.TWO_DIGIT -> DivConfig(dividendDigitsRange = 3..3, divisorRange = 10..79)
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
    startMode: StartMode = StartMode.RANDOM,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val debugHL = true
    val debugScenario = true
    val rng = remember { Random(System.currentTimeMillis()) }
    var mode by remember { mutableStateOf(DivMode.ONE_DIGIT) }

    fun newPlan(): DivisionPlan {
        val (dividend, divisor) = generateDivision(rng, mode)
        return generateDivisionPlan(dividend, divisor)
    }

    val debugPlan = remember { generateDivisionPlan(854, 2) }
    var plan by remember(mode, startMode, debugScenario) {
        mutableStateOf(
            if (debugScenario) debugPlan
            else if (startMode == StartMode.RANDOM) newPlan()
            else null
        )
    }

    var manualDividend by remember { mutableStateOf("") }
    var manualDivisor by remember { mutableStateOf("") }
    var manualNumbers by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var targetIndex by remember(plan) { mutableStateOf(0) }
    val p = plan
    val currentTarget = p?.targets?.getOrNull(targetIndex)
    val done = p != null && currentTarget == null

    var correctCount by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val quotientSlotCount = p?.dividendDigits?.size ?: 0
    val quotientInputs = remember(plan) { List(quotientSlotCount) { mutableStateOf("") } }
    val quotientErrors = remember(plan) { List(quotientSlotCount) { mutableStateOf(false) } }
    val productInputs = remember(plan) {
        p?.steps?.map { step ->
            List(step.product.toString().length) { mutableStateOf("") }
        } ?: emptyList()
    }
    val productErrors = remember(plan) {
        p?.steps?.map { step ->
            List(step.product.toString().length) { mutableStateOf(false) }
        } ?: emptyList()
    }
    val remainderInputs = remember(plan) {
        p?.steps?.map { step ->
            List(step.remainder.toString().length) { mutableStateOf("") }
        } ?: emptyList()
    }
    val remainderErrors = remember(plan) {
        p?.steps?.map { step ->
            List(step.remainder.toString().length) { mutableStateOf(false) }
        } ?: emptyList()
    }
    val bringDownDone = remember(plan) { List(p?.steps?.size ?: 0) { mutableStateOf(false) } }

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
        if (debugScenario) {
            plan = debugPlan
            targetIndex = 0
            message = null
            showSuccessDialog = false
        } else if (startMode == StartMode.MANUAL) {
            manualDividend = ""
            manualDivisor = ""
            manualNumbers = null
            plan = null
            resetSame()
        } else {
            plan = newPlan()
            targetIndex = 0
            message = null
            showSuccessDialog = false
        }
    }

    fun playCorrect() {
        if (soundEnabled) fx.correct()
    }

    fun playWrong() {
        if (soundEnabled) fx.wrong()
    }

    fun advanceTarget() {
        val p = plan ?: return
        targetIndex++
        if (targetIndex >= p.targets.size) {
            correctCount++
            message = "✅ Finito! Quoziente ${p.finalQuotient} resto ${p.finalRemainder}"
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
        val activePlan = plan ?: return
        activePlan.steps.forEachIndexed { si, step ->
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
        targetIndex = activePlan.targets.size
        message = "✅ Soluzione completata! Quoziente ${activePlan.finalQuotient} resto ${activePlan.finalRemainder}"
        showSuccessDialog = true
    }

    val hint = when {
        done && p != null -> "Bravo! Quoziente ${p.finalQuotient} con resto ${p.finalRemainder}."
        p == null -> "Inserisci dividendo e divisore e premi Avvia."
        else -> currentTarget?.hint.orEmpty()
    }

    LaunchedEffect(done) {
        if (done && p != null) {
            showSuccessDialog = true
        }
    }

    val activeStepNumber = currentTarget?.stepIndex?.plus(1) ?: (p?.steps?.size ?: 0)
    val ui = rememberUiSizing()

    val digitW = if (ui.isCompact) 36.dp else 44.dp
    val digitH = if (ui.isCompact) 48.dp else 56.dp
    val digitSmallW = if (ui.isCompact) 34.dp else 40.dp
    val digitSmallH = if (ui.isCompact) 46.dp else 52.dp
    val fontLarge = if (ui.isCompact) 18.sp else 22.sp
    val fontSmall = if (ui.isCompact) 16.sp else 20.sp
    val gap = if (ui.isCompact) 4.dp else 6.dp
    val columns = p?.dividendDigits?.size ?: 0
    val quotientDigitW = if (columns == 4) {
        if (ui.isCompact) 32.dp else 38.dp
    } else {
        digitW
    }
    val divisorDigits = p?.divisor?.toString().orEmpty()
    val divisorWidth = digitW * divisorDigits.length + gap * (divisorDigits.length - 1)
    val dividerHeight = digitH + digitH + gap
    val stepGap = if (ui.isCompact) 6.dp else 8.dp

    val hlSet = remember(currentTarget) { currentTarget?.highlights?.toSet() ?: emptySet() }
    fun shouldHighlight(zone: HLZone, step: Int, col: Int, t: DivisionTarget?): Boolean {
        return t?.highlights?.contains(HLCell(zone, step, col)) == true
    }

    fun debugMismatch(zone: HLZone, step: Int, col: Int): Boolean {
        if (!debugHL) return false
        val inPlan = HLCell(zone, step, col) in hlSet
        val inUi = shouldHighlight(zone, step, col, currentTarget)
        if (inPlan != inUi) {
            Log.d("DivisionHL", "MISMATCH zone=$zone step=$step col=$col")
        }
        return inPlan != inUi
    }

    fun debugLabel(zone: HLZone, step: Int, col: Int): String {
        val zoneLabel = when (zone) {
            HLZone.DIVIDEND -> "D"
            HLZone.DIVISOR -> "S"
            HLZone.QUOTIENT -> "Q"
            HLZone.PRODUCT -> "P"
            HLZone.REMAINDER -> "R"
            HLZone.BRING -> "B"
        }
        return "$zoneLabel s$step c$col"
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
                                    if (startMode == StartMode.RANDOM && !debugScenario) {
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
                    }

                                    if (startMode == StartMode.MANUAL && !debugScenario) {
                                        val dividendValue = manualDividend.toIntOrNull()
                                        val divisorValue = manualDivisor.toIntOrNull()
                        val manualValid = dividendValue in 2..999 &&
                            divisorValue in 2..99 &&
                            (dividendValue ?: 0) > (divisorValue ?: 0)
                        val manualError = if (manualValid || (manualDividend.isBlank() && manualDivisor.isBlank())) {
                            null
                        } else {
                            "Inserisci un dividendo (max 3 cifre) maggiore del divisore (max 2 cifre)."
                        }

                        SeaGlassPanel(title = "Inserimento") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = manualDividend,
                                    onValueChange = { manualDividend = it.filter { c -> c.isDigit() }.take(3) },
                                    label = { Text("Dividendo") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = manualDivisor,
                                    onValueChange = { manualDivisor = it.filter { c -> c.isDigit() }.take(2) },
                                    label = { Text("Divisore") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                androidx.compose.material3.Button(
                                    onClick = {
                                        val dividend = dividendValue ?: return@Button
                                        val divisor = divisorValue ?: return@Button
                                        manualNumbers = dividend to divisor
                                        plan = generateDivisionPlan(dividend, divisor)
                                        resetSame()
                                    },
                                    enabled = manualValid,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Avvia")
                                }
                                if (manualError != null) {
                                    Text(manualError, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                                    SeaGlassPanel(title = "Calcolo") {
                                        if (p == null) {
                                            Text("Inserisci dividendo e divisore e premi Avvia.")
                                        } else {
                                            val activePlan = p
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
                                                    val digit = activePlan.dividendDigits[col]
                                                    val step = currentTarget?.stepIndex ?: 0
                                                    val highlightDividendCell = shouldHighlight(
                                                        HLZone.DIVIDEND,
                                                        step,
                                                        col,
                                                        currentTarget
                                                    )
                                                    DivisionFixedDigit(
                                                        text = digit.toString(),
                                                        w = digitW,
                                                        h = digitH,
                                                        fontSize = fontLarge,
                                                        highlight = highlightDividendCell,
                                                        debugLabel = if (debugHL) debugLabel(HLZone.DIVIDEND, step, col) else null,
                                                        debugMismatch = debugMismatch(HLZone.DIVIDEND, step, col)
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
                                                divisorDigits.forEachIndexed { idx, ch ->
                                                    val step = currentTarget?.stepIndex ?: 0
                                                    DivisionFixedDigit(
                                                        text = ch.toString(),
                                                        w = digitW,
                                                        h = digitH,
                                                        fontSize = fontLarge,
                                                        highlight = shouldHighlight(HLZone.DIVISOR, step, idx, currentTarget),
                                                        debugLabel = if (debugHL) debugLabel(HLZone.DIVISOR, step, idx) else null,
                                                        debugMismatch = debugMismatch(HLZone.DIVISOR, step, idx)
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
                                            val target = activePlan.targets.firstOrNull {
                                                it.type == DivisionTargetType.QUOTIENT && it.gridCol == col
                                            }
                                            val step = currentTarget?.stepIndex ?: 0
                                            val highlightQuotient = shouldHighlight(HLZone.QUOTIENT, step, col, currentTarget)
                                            val mismatch = debugMismatch(HLZone.QUOTIENT, step, col)
                                            if (target != null) {
                                                val active = target == currentTarget
                                                DivisionDigitBox(
                                                    value = quotientInputs[col].value,
                                                    enabled = active,
                                                    active = active,
                                                    isError = quotientErrors[col].value,
                                                    highlight = highlightQuotient,
                                                    microLabel = target.microLabel,
                                                    onValueChange = { onDigitInput(target, it) },
                                                    w = quotientDigitW,
                                                    h = digitH,
                                                    fontSize = fontLarge,
                                                    debugLabel = if (debugHL) debugLabel(HLZone.QUOTIENT, step, col) else null,
                                                    debugMismatch = mismatch
                                                )
                                            } else {
                                                DivisionDigitBox(
                                                    value = "",
                                                    enabled = false,
                                                    active = false,
                                                    isError = false,
                                                    highlight = highlightQuotient,
                                                    onValueChange = {},
                                                    w = quotientDigitW,
                                                    h = digitH,
                                                    fontSize = fontLarge,
                                                    debugLabel = if (debugHL) debugLabel(HLZone.QUOTIENT, step, col) else null,
                                                    debugMismatch = mismatch
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(stepGap)) {
                                    activePlan.steps.forEachIndexed { si, step ->
                                        val productTargets = activePlan.targets.filter {
                                            it.type == DivisionTargetType.PRODUCT && it.stepIndex == si
                                        }
                                        val remainderTargets = activePlan.targets.filter {
                                            it.type == DivisionTargetType.REMAINDER && it.stepIndex == si
                                        }
                                        val bringDownTarget = activePlan.targets.firstOrNull {
                                            it.type == DivisionTargetType.BRING_DOWN && it.stepIndex == si
                                        }
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
                                                    highlight = shouldHighlight(HLZone.PRODUCT, si, col, currentTarget),
                                                    microLabel = target.microLabel,
                                                    onValueChange = { onDigitInput(target, it) },
                                                    w = digitSmallW,
                                                    h = digitSmallH,
                                                    fontSize = fontSmall,
                                                    debugLabel = if (debugHL) debugLabel(HLZone.PRODUCT, si, col) else null,
                                                    debugMismatch = debugMismatch(HLZone.PRODUCT, si, col)
                                                )
                                            } else if (debugHL) {
                                                DivisionDebugCell(
                                                    w = digitSmallW,
                                                    h = digitSmallH,
                                                    debugLabel = debugLabel(HLZone.PRODUCT, si, col),
                                                    debugMismatch = debugMismatch(HLZone.PRODUCT, si, col)
                                                )
                                            }
                                        }

                                        val remainderRow: @Composable () -> Unit = {
                                            DivisionDigitRow(
                                                columns = columns,
                                                cellW = digitSmallW,
                                                cellH = digitSmallH,
                                                gap = gap
                                            ) { col ->
                                                val target = remainderTargets.firstOrNull { it.gridCol == col }
                                                when {
                                                    target != null -> {
                                                        val active = target == currentTarget
                                                        DivisionDigitBox(
                                                            value = remainderInputs[si][target.idx].value,
                                                            enabled = active,
                                                            active = active,
                                                            isError = remainderErrors[si][target.idx].value,
                                                            highlight = shouldHighlight(HLZone.REMAINDER, si, col, currentTarget),
                                                            microLabel = target.microLabel,
                                                            onValueChange = { onDigitInput(target, it) },
                                                            w = digitSmallW,
                                                            h = digitSmallH,
                                                            fontSize = fontSmall,
                                                            debugLabel = if (debugHL) debugLabel(HLZone.REMAINDER, si, col) else null,
                                                            debugMismatch = debugMismatch(HLZone.REMAINDER, si, col)
                                                        )
                                                    }
                                                    bringDownTarget != null &&
                                                        step.bringDownDigit != null &&
                                                        col == bringDownTarget.gridCol -> {
                                                        val active = bringDownTarget == currentTarget
                                                        DivisionActionDigit(
                                                            text = step.bringDownDigit.toString(),
                                                            active = active,
                                                            microLabel = bringDownTarget.microLabel,
                                                            w = digitSmallW,
                                                            h = digitSmallH,
                                                            fontSize = fontSmall,
                                                            highlight = shouldHighlight(HLZone.BRING, si, col, currentTarget),
                                                            debugLabel = if (debugHL) debugLabel(HLZone.BRING, si, col) else null,
                                                            debugMismatch = debugMismatch(HLZone.BRING, si, col)
                                                        )
                                                    }
                                                    debugHL -> {
                                                        DivisionDebugCell(
                                                            w = digitSmallW,
                                                            h = digitSmallH,
                                                            debugLabel = debugLabel(HLZone.REMAINDER, si, col),
                                                            debugMismatch = debugMismatch(HLZone.REMAINDER, si, col)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (step.bringDownDigit != null && bringDownTarget != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(gap)
                                            ) {
                                                remainderRow()
                                                OutlinedButton(
                                                    onClick = { onBringDown(bringDownTarget) },
                                                    enabled = bringDownTarget == currentTarget
                                                ) {
                                                    Text(if (bringDownDone[si].value) "Abbassato" else "Abbassa")
                                                }
                                            }
                                        } else {
                                            remainderRow()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SeaGlassPanel(title = "Aiuto") {
                        Column(verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)) {
                            if (debugScenario && p != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            targetIndex = (targetIndex - 1).coerceAtLeast(0)
                                        }
                                    ) {
                                        Text("Prev")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            targetIndex = (targetIndex + 1).coerceAtMost(p.targets.size)
                                        }
                                    ) {
                                        Text("Next")
                                    }
                                    Text("Target ${targetIndex}/${p.targets.size}")
                                }
                            }

                            val highlights = currentTarget?.highlights.orEmpty()
                            val debugText = buildString {
                                append("type=")
                                append(currentTarget?.type?.name ?: "-")
                                append(" step=")
                                append(currentTarget?.stepIndex ?: "-")
                                append(" col=")
                                append(currentTarget?.gridCol ?: "-")
                                append(" HL: D=")
                                append(highlights.count { it.zone == HLZone.DIVIDEND })
                                append(" P=")
                                append(highlights.count { it.zone == HLZone.PRODUCT })
                                append(" R=")
                                append(highlights.count { it.zone == HLZone.REMAINDER })
                                append(" B=")
                                append(highlights.count { it.zone == HLZone.BRING })
                                append(" Q=")
                                append(highlights.count { it.zone == HLZone.QUOTIENT })
                                append(" S=")
                                append(highlights.count { it.zone == HLZone.DIVISOR })
                            }
                            Text(debugText)
                            if (debugHL) {
                                val highlightsText = highlights.joinToString(", ") {
                                    debugLabel(it.zone, it.stepIndex, it.col)
                                }
                                Column {
                                    Text("highlights:")
                                    Box(
                                        modifier = Modifier
                                            .heightIn(max = 120.dp)
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(6.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(highlightsText)
                                    }
                                }
                            }

                            Text(
                                text = hint,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (p == null) "Passo 0/0"
                                else if (done) "Passo ${p.steps.size}/${p.steps.size}"
                                else "Passo $activeStepNumber/${p.steps.size}",
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
                    onLeft = {
                        if (startMode == StartMode.MANUAL) {
                            val manual = manualNumbers
                            if (manual != null && p != null) {
                                plan = generateDivisionPlan(manual.first, manual.second)
                                resetSame()
                            } else {
                                manualDividend = ""
                                manualDivisor = ""
                                manualNumbers = null
                                plan = null
                                resetSame()
                            }
                        } else {
                            resetSame()
                        }
                    },
                    rightText = "Nuovo",
                    onRight = { resetNew() },
                    modifier = Modifier.fillMaxWidth(),
                    center = {
                        OutlinedButton(onClick = { fillSolution() }, enabled = p != null) {
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
            resultText = p?.let { "${it.finalQuotient} r. ${it.finalRemainder}" }.orEmpty()
        )
    }
}
