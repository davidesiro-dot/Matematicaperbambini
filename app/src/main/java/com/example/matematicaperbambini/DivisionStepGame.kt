package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.random.Random
import androidx.compose.foundation.layout.Spacer

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
        if (nonTrivial || remainderNonZero) return dividend to divisor
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
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    exercise: ExerciseInstance? = null,
    helps: HelpSettings? = null,
    onExerciseFinished: ((ExerciseResultPartial) -> Unit)? = null
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var mode by remember { mutableStateOf(DivMode.ONE_DIGIT) }
    val isHomeworkMode = exercise != null || onExerciseFinished != null

    fun newPlan(): DivisionPlan {
        val (dividend, divisor) = generateDivision(rng, mode)
        return generateDivisionPlan(dividend, divisor)
    }

    var plan by remember(startMode) {
        mutableStateOf(if (startMode == StartMode.RANDOM) newPlan() else null)
    }

    var manualDividend by remember { mutableStateOf("") }
    var manualDivisor by remember { mutableStateOf("") }
    var manualNumbers by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var targetIndex by remember(plan) { mutableStateOf(0) }
    val p = plan
    val currentTarget = p?.targets?.getOrNull(targetIndex)
    val done = p != null && currentTarget == null

    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var solutionUsed by remember { mutableStateOf(false) }
    var attempts by remember(exercise?.a, exercise?.b) { mutableStateOf(0) }
    val wrongAnswers = remember(exercise?.a, exercise?.b) { mutableStateListOf<String>() }
    val stepErrors = remember(exercise?.a, exercise?.b) { mutableStateListOf<StepError>() }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    val inputGuard = remember { StepInputGuard() }

    val quotientSlotCount = p?.dividendDigits?.size ?: 0
    val quotientInputs = remember(plan) { List(quotientSlotCount) { mutableStateOf("") } }
    val quotientErrors = remember(plan) { List(quotientSlotCount) { mutableStateOf(false) } }

    val productInputs = remember(plan) {
        p?.steps?.map { step -> List(step.product.toString().length) { mutableStateOf("") } } ?: emptyList()
    }
    val productErrors = remember(plan) {
        p?.steps?.map { step -> List(step.product.toString().length) { mutableStateOf(false) } } ?: emptyList()
    }

    val remainderInputs = remember(plan) {
        p?.steps?.map { step -> List(step.remainder.toString().length) { mutableStateOf("") } } ?: emptyList()
    }
    val remainderErrors = remember(plan) {
        p?.steps?.map { step -> List(step.remainder.toString().length) { mutableStateOf(false) } } ?: emptyList()
    }

    val bringDownDone = remember(plan) { List(p?.steps?.size ?: 0) { mutableStateOf(false) } }

    fun resetSame() {
        targetIndex = 0
        message = null
        showSuccessDialog = false
        solutionUsed = false
        attempts = 0
        wrongAnswers.clear()
        stepErrors.clear()
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
        gameState = if (plan == null) GameState.INIT else GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    fun resetNew() {
        if (startMode == StartMode.MANUAL) {
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
            solutionUsed = false
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
        }
    }

    fun playCorrect() { if (soundEnabled) fx.correct() }
    fun playWrong() { if (soundEnabled) fx.wrong() }

    fun advanceTarget() {
        val pp = plan ?: return
        targetIndex++
        if (targetIndex >= pp.targets.size) {
            if (!isHomeworkMode) {
                correctCount++
            }
            message = "✅ Finito! Quoziente ${pp.finalQuotient} resto ${pp.finalRemainder}"
            gameState = GameState.GAME_COMPLETED
        } else {
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
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
        if (activeTarget != target) {
            val digit = value.firstOrNull() ?: return
            val expected = target.expected ?: return
            attempts += 1
            wrongAnswers += digit.toString()
            stepErrors += StepError(
                stepLabel = "Passo fuori ordine",
                expected = expected.toString(),
                actual = digit.toString()
            )
            playWrong()
            return
        }
        val digit = value.firstOrNull() ?: return
        val expected = activeTarget.expected ?: return
        val stepId = "div-${activeTarget.stepIndex}-${activeTarget.type}-${activeTarget.idx}"
        val validation = validateUserInput(
            stepId = stepId,
            value = digit.toString(),
            expectedRange = 0..9,
            gameState = gameState,
            guard = inputGuard,
            onInit = {
                gameState = GameState.AWAITING_INPUT
                inputGuard.reset()
            }
        )
        if (!validation.isValid) {
            if (validation.failure == ValidationFailure.TOO_FAST ||
                validation.failure == ValidationFailure.NOT_AWAITING_INPUT
            ) {
                return
            }
            return
        }

        updateCellValue(activeTarget, digit.toString())
        if (digit != expected) {
            updateCellError(activeTarget, true)
            updateCellValue(activeTarget, "")
            message = "❌ Riprova"
            attempts += 1
            wrongAnswers += digit.toString()
            val stepLabel = when (activeTarget.type) {
                DivisionTargetType.QUOTIENT -> "Quoziente (passo ${activeTarget.stepIndex + 1})"
                DivisionTargetType.PRODUCT -> "Prodotto (passo ${activeTarget.stepIndex + 1}, cifra ${activeTarget.idx + 1})"
                DivisionTargetType.REMAINDER -> "Resto (passo ${activeTarget.stepIndex + 1}, cifra ${activeTarget.idx + 1})"
                DivisionTargetType.BRING_DOWN -> "Abbasso cifra (passo ${activeTarget.stepIndex + 1})"
            }
            stepErrors += StepError(
                stepLabel = stepLabel,
                expected = expected.toString(),
                actual = digit.toString()
            )
            playWrong()
            val locked = inputGuard.registerAttempt(stepId)
            if (locked) {
                updateCellError(activeTarget, false)
                updateCellValue(activeTarget, expected.toString())
                message = "Continuiamo con il prossimo passo."
                advanceTarget()
            }
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
        val stepId = "div-bring-${target.stepIndex}"
        val validation = validateUserInput(
            stepId = stepId,
            value = "0",
            expectedRange = 0..1,
            gameState = gameState,
            guard = inputGuard,
            onInit = {
                gameState = GameState.AWAITING_INPUT
                inputGuard.reset()
            }
        )
        if (!validation.isValid) {
            if (validation.failure == ValidationFailure.TOO_FAST ||
                validation.failure == ValidationFailure.NOT_AWAITING_INPUT
            ) {
                return
            }
        }
        bringDownDone[target.stepIndex].value = true
        message = null
        playCorrect()
        advanceTarget()
    }

    fun fillSolution() {
        val activePlan = plan ?: return
        solutionUsed = true
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
            if (step.bringDownDigit != null) bringDownDone[si].value = true
        }
        targetIndex = activePlan.targets.size
        message = null
        showSuccessDialog = false
    }

    val hint = when {
        done && p != null -> {
            if (solutionUsed) {
                "Quoziente ${p.finalQuotient} con resto ${p.finalRemainder}."
            } else {
                "Bravo! Quoziente ${p.finalQuotient} con resto ${p.finalRemainder}."
            }
        }
        p == null -> "Inserisci dividendo e divisore e premi Avvia."
        helps?.hintsEnabled == false -> "Completa i passaggi della divisione."
        else -> currentTarget?.hint.orEmpty()
    }
    val showCellHelper = helps?.showCellHelper == true

    LaunchedEffect(done) {
        if (done && p != null && !solutionUsed) showSuccessDialog = true
    }

    LaunchedEffect(exercise?.a, exercise?.b) {
        val dividend = exercise?.a
        val divisor = exercise?.b
        if (dividend != null && divisor != null && divisor != 0) {
            mode = if (divisor >= 10) DivMode.TWO_DIGIT else DivMode.ONE_DIGIT
            plan = generateDivisionPlan(dividend, divisor)
            manualNumbers = dividend to divisor
            resetSame()
        }
    }

    LaunchedEffect(plan) {
        if (plan != null && gameState == GameState.INIT) {
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
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
    val stepGap = if (ui.isCompact) 6.dp else 8.dp

    val columns = p?.dividendDigits?.size ?: 0
    val quotientDigitW = if (columns == 4) {
        if (ui.isCompact) 32.dp else 38.dp
    } else digitW

    // ✅ overlay divider: parametri + posizione
    val dividerGap = if (ui.isCompact) 10.dp else 12.dp
    val dividendRowWidth = digitW * columns + gap * (columns - 1)
    val dividerLineW = if (ui.isCompact) 2.dp else 3.dp
    val dividerX = dividendRowWidth + (dividerGap / 2) - (dividerLineW / 2)

    // ✅ divisore allineato alla griglia del quoziente (stessa “riga” di celle)
    val divisorDigits = p?.divisor?.toString().orEmpty()
    val divisorColumns = maxOf(columns, divisorDigits.length, 1)
    val divisorCellW = if (columns == 4) quotientDigitW else digitW
    val divisorWidth = divisorCellW * divisorColumns + gap * (divisorColumns - 1)
    val divisorOffset = if (columns > divisorDigits.length) gap else 0.dp

    fun isHL(zone: HLZone, step: Int, col: Int): Boolean =
        currentTarget?.highlights?.contains(HLCell(zone, step, col)) == true

    // ✅ FIX: misuro altezza reale del contenuto e disegno la barra con height(...) (NO fillMaxHeight)
    var calcContentHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val calcContentHeightDp = with(density) { calcContentHeightPx.toDp() }

    Box(Modifier.fillMaxSize()) {
        GameScreenFrame(
            title = "Divisioni passo passo",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            bonusTarget = BONUS_TARGET_LONG_MULT_DIV,
            hintText = hint,
            ui = ui,
            message = message,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {

                    if (startMode == StartMode.RANDOM && !isHomeworkMode) {
                        SeaGlassPanel(title = "Modalità") {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val oneDigitSelected = mode == DivMode.ONE_DIGIT
                                val twoDigitSelected = mode == DivMode.TWO_DIGIT

                                if (oneDigitSelected) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        androidx.compose.material3.Button(
                                            onClick = { mode = DivMode.ONE_DIGIT; resetNew() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Divisore 1 cifra") }
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { mode = DivMode.ONE_DIGIT; resetNew() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Divisore 1 cifra") }
                                    }
                                }

                                if (twoDigitSelected) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        androidx.compose.material3.Button(
                                            onClick = { mode = DivMode.TWO_DIGIT; resetNew() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Divisore 2 cifre") }
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { mode = DivMode.TWO_DIGIT; resetNew() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Divisore 2 cifre") }
                                    }
                                }
                            }
                        }
                    }

                    if (startMode == StartMode.MANUAL && !isHomeworkMode) {
                        val dividendValue = manualDividend.toIntOrNull()
                        val divisorValue = manualDivisor.toIntOrNull()
                        val manualValid = dividendValue in 2..999 &&
                                divisorValue in 2..99 &&
                                (dividendValue ?: 0) > (divisorValue ?: 0)

                        val manualError = if (manualValid || (manualDividend.isBlank() && manualDivisor.isBlank())) null
                        else "Inserisci un dividendo (max 3 cifre) maggiore del divisore (max 2 cifre)."

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
                                ) { Text("Avvia") }

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

                            // ✅ Contenitore unico: la barra verticale è overlay e NON altera le misure
                            Box {
                                Column(
                                    modifier = Modifier.onSizeChanged { calcContentHeightPx = it.height },
                                    verticalArrangement = Arrangement.spacedBy(stepGap)
                                ) {

                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        // DIVIDENDO (sinistra) – larghezza “fissa” per calcolare dividerX
                                        Column(
                                            modifier = Modifier.width(dividendRowWidth),
                                            verticalArrangement = Arrangement.spacedBy(stepGap)
                                        ) {
                                            DivisionDigitRow(
                                                columns = columns,
                                                cellW = digitW,
                                                cellH = digitH,
                                                gap = gap
                                            ) { col ->
                                                val digit = activePlan.dividendDigits[col]
                                                val step = currentTarget?.stepIndex ?: 0
                                                DivisionFixedDigit(
                                                    text = digit.toString(),
                                                    w = digitW,
                                                    h = digitH,
                                                    fontSize = fontLarge,
                                                    highlight = isHL(HLZone.DIVIDEND, step, col)
                                                )
                                            }
                                        }

                                        // spazio “vuoto” dove passa la barra overlay
                                        Spacer(Modifier.width(dividerGap))

                                        // DIVISORE + QUOZIENTE (destra)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)
                                        ) {
                                            // Divisore nella stessa griglia “allineata”
                                            DivisionDigitRow(
                                                columns = divisorColumns,
                                                cellW = divisorCellW,
                                                cellH = digitH,
                                                gap = gap
                                            ) { col ->
                                                val digitIndex = col - (divisorColumns - divisorDigits.length)
                                                val step = currentTarget?.stepIndex ?: 0
                                                if (digitIndex in divisorDigits.indices) {
                                                    val ch = divisorDigits[digitIndex]
                                                    DivisionFixedDigit(
                                                        text = ch.toString(),
                                                        w = divisorCellW,
                                                        h = digitH,
                                                        fontSize = fontLarge,
                                                        highlight = isHL(HLZone.DIVISOR, step, digitIndex)
                                                    )
                                                } else {
                                                    DivisionFixedDigit(
                                                        text = "",
                                                        w = divisorCellW,
                                                        h = digitH,
                                                        fontSize = fontLarge,
                                                        highlight = false
                                                    )
                                                }
                                            }

                                            // linea orizzontale sotto divisore
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = divisorOffset)
                                                    .width(divisorWidth)
                                                    .height(if (ui.isCompact) 2.dp else 3.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )

                                            // quoziente
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
                                                val highlightQuotient = isHL(HLZone.QUOTIENT, step, col)

                                                if (target != null) {
                                                    val active = target == currentTarget
                                                    DivisionDigitBox(
                                                        value = quotientInputs[col].value,
                                                        enabled = active,
                                                        active = active,
                                                        isError = quotientErrors[col].value,
                                                        highlight = highlightQuotient,
                                                        microLabel = if (showCellHelper) target.microLabel else null,
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
                                                        highlight = highlightQuotient,
                                                        microLabel = null,
                                                        onValueChange = {},
                                                        w = quotientDigitW,
                                                        h = digitH,
                                                        fontSize = fontLarge
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // STEPS
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

                                            // riga prodotto
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
                                                        highlight = isHL(HLZone.PRODUCT, si, col),
                                                        microLabel = if (showCellHelper) target.microLabel else null,
                                                        onValueChange = { onDigitInput(target, it) },
                                                        w = digitSmallW,
                                                        h = digitSmallH,
                                                        fontSize = fontSmall
                                                    )
                                                }
                                            }

                                            // linea orizzontale (come su carta) sotto prodotto
                                            Box(
                                                modifier = Modifier
                                                    .width(digitSmallW * columns + gap * (columns - 1))
                                                    .height(if (ui.isCompact) 2.dp else 3.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )

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
                                                                highlight = isHL(HLZone.REMAINDER, si, col),
                                                                microLabel = if (showCellHelper) target.microLabel else null,
                                                                onValueChange = { onDigitInput(target, it) },
                                                                w = digitSmallW,
                                                                h = digitSmallH,
                                                                fontSize = fontSmall
                                                            )
                                                        }

                                                        bringDownTarget != null &&
                                                                step.bringDownDigit != null &&
                                                                bringDownDone[si].value &&
                                                                col == bringDownTarget.gridCol -> {
                                                            val active = bringDownTarget == currentTarget
                                                            DivisionActionDigit(
                                                                text = step.bringDownDigit.toString(),
                                                                active = active,
                                                                microLabel = if (showCellHelper) bringDownTarget.microLabel else null,
                                                                w = digitSmallW,
                                                                h = digitSmallH,
                                                                fontSize = fontSmall,
                                                                highlight = isHL(HLZone.BRING, si, col)
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

                                                    // ✅ scanso extra per non stare “sulla traiettoria” della barra overlay
                                                    OutlinedButton(
                                                        onClick = { onBringDown(bringDownTarget) },
                                                        enabled = bringDownTarget == currentTarget,
                                                        modifier = Modifier.padding(start = dividerGap + 6.dp)
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

                                // ✅ LINEA VERTICALE OVERLAY: altezza = contenuto reale (non sposta niente)
                                if (calcContentHeightDp > 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = dividerX)
                                            .width(dividerLineW)
                                            .height(calcContentHeightDp)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .align(Alignment.TopStart)
                                    )
                                }
                            }
                        }
                    }

                    SeaGlassPanel(title = "Aiuto") {
                        Column(verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)) {
                            Text(text = hint, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (p == null) "Passo 0/0"
                                else if (done) "Passo ${p.steps.size}/${p.steps.size}"
                                else "Passo $activeStepNumber/${p.steps.size}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
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
                    rightText = if (isHomeworkMode) "Avanti" else "Nuovo",
                    onRight = {
                        if (isHomeworkMode) {
                            if (done) {
                                onExerciseFinished?.invoke(
                                    ExerciseResultPartial(
                                        correct = true,
                                        attempts = attempts,
                                        wrongAnswers = wrongAnswers.toList(),
                                        stepErrors = stepErrors.toList(),
                                        solutionUsed = solutionUsed
                                    )
                                )
                            }
                        } else {
                            resetNew()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    center = {
                        Button(
                            onClick = { fillSolution() },
                            enabled = p != null && (helps?.allowSolution != false),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Soluzione",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                fontSize = 15.sp
                            )
                        }
                    }
                )
            }
        )

        if (!isHomeworkMode) {
            BonusRewardHost(
                correctCount = correctCount,
                rewardsEarned = rewardsEarned,
                rewardEvery = BONUS_TARGET_LONG_MULT_DIV,
                soundEnabled = soundEnabled,
                fx = fx,
                onOpenLeaderboard = onOpenLeaderboardFromBonus,
                onBonusPromptAction = { showSuccessDialog = false },
                onRewardEarned = { rewardsEarned += 1 },
                onRewardSkipped = { rewardsEarned += 1 }
            )
        }

        SuccessDialog(
            show = showSuccessDialog,
            onNew = {
                showSuccessDialog = false
                if (isHomeworkMode) {
                    onExerciseFinished?.invoke(
                        ExerciseResultPartial(
                            correct = true,
                            attempts = attempts,
                            wrongAnswers = wrongAnswers.toList(),
                            stepErrors = stepErrors.toList(),
                            solutionUsed = solutionUsed
                        )
                    )
                } else {
                    resetNew()
                }
            },
            onDismiss = { showSuccessDialog = false },
            resultText = p?.let { "${it.finalQuotient} r. ${it.finalRemainder}" }.orEmpty(),
            confirmText = if (isHomeworkMode) "Avanti" else "Nuova operazione"
        )
    }
}
