package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import androidx.compose.foundation.layout.Spacer

/* -----------------------------------------------------------------------
   LONG SUBTRACTION (guidata): con prestito e anche senza
   - Una sola casella attiva (verde/bordo evidenziato)
   - Se sbagli: casella rossa e cancella cifra
   - A fine operazione: overlay “Corretto! Tappa per continuare”
   ----------------------------------------------------------------------- */

private data class SubProblem(val a: Int, val b: Int)

private data class ExpectedSub(
    val topDigitsOriginal: List<Int>,
    val bottomDigits: List<Int>,
    val topDigitsAfterBorrow: List<Int>, // come devono diventare dopo i prestiti
    val topDigitsBeforeBorrow: List<Int>, // cifre disponibili al momento del confronto
    val borrowNeeded: List<Boolean>,
    val resultDigits: List<Int>,
    val borrowSteps: List<List<Int>>      // colonne da aggiornare prima della cifra risultato
)

private enum class SubStepType {
    COMPARE,
    BORROW_DECISION,
    BORROW_VALUE,
    RESULT_DIGIT
}
private data class SubStep(val type: SubStepType, val colIndexFromLeft: Int)

private const val BORROW_DECISION_ERROR = "BORROW_DECISION_ERROR"
private const val BORROW_VALUE_ERROR = "BORROW_VALUE_ERROR"
private const val SUBTRACTION_CALCULATION_ERROR = "SUBTRACTION_CALCULATION_ERROR"

private fun digitsToInt(ds: IntArray): Int {
    var n = 0
    for (d in ds) n = n * 10 + d
    return n
}

private fun randomNDigits(digits: Int, rng: Random): Int {
    val min = if (digits <= 1) 0 else (1..digits - 1).fold(1) { acc, _ -> acc * 10 }
    val max = (1..digits).fold(1) { acc, _ -> acc * 10 } - 1
    return rng.nextInt(min, max + 1)
}

/**
 * Genera una sottrazione a N cifre:
 * - a >= b sempre
 * - a volte con prestito, a volte senza (mix)
 */
private fun generateSubtractionMixed(digits: Int, rng: Random = Random.Default): SubProblem {
    require(digits >= 2)

    // 50% proviamo a forzare un prestito, 50% no (ma comunque casuale)
    val wantBorrow = rng.nextBoolean()

    fun makeWithBorrow(): SubProblem {
        // Costruisco cifre con un prestito garantito almeno in una colonna non di sinistra.
        val a = IntArray(digits)
        val b = IntArray(digits)

        // base random
        for (i in 0 until digits) {
            a[i] = rng.nextInt(0, 10)
            b[i] = rng.nextInt(0, 10)
        }

        // prima cifra non zero
        a[0] = rng.nextInt(1, 10)

        // scelgo una posizione (da destra) dove b > a => prestito
        val borrowPosFromRight = rng.nextInt(0, digits - 1) // escludo la colonna più a sinistra
        val idx = digits - 1 - borrowPosFromRight

        a[idx] = rng.nextInt(0, 5) // piccolo
        b[idx] = rng.nextInt((a[idx] + 1).coerceAtMost(9), 10) // più grande => prestito

        // assicuro che a sia >= b come numeri
        var A = digitsToInt(a)
        var B = digitsToInt(b)
        if (A <= B) {
            // se succede, abbasso B
            B = B % (A.coerceAtLeast(2) - 1)
        }
        return SubProblem(A, B)
    }

    fun makeWithoutBorrow(): SubProblem {
        // Creo cifre tali che per ogni colonna aDigit >= bDigit
        val a = IntArray(digits)
        val b = IntArray(digits)

        a[0] = rng.nextInt(1, 10)
        b[0] = rng.nextInt(0, a[0] + 1)

        for (i in 1 until digits) {
            a[i] = rng.nextInt(0, 10)
            b[i] = rng.nextInt(0, a[i] + 1) // <= a[i] => niente prestito in quella colonna
        }

        val A = digitsToInt(a)
        val B = digitsToInt(b)
        return SubProblem(A, B)
    }

    return if (wantBorrow) {
        // ogni tanto può uscire comunque senza prestito (capita). Va benissimo.
        makeWithBorrow()
    } else {
        makeWithoutBorrow()
    }
}

private fun computeExpectedSub(problem: SubProblem, digits: Int): ExpectedSub {
    val top = problem.a.toString().padStart(digits, '0').map { it.digitToInt() }.toMutableList()
    val bottom = problem.b.toString().padStart(digits, '0').map { it.digitToInt() }
    val topAfter = top.toMutableList()
    val topBefore = MutableList(digits) { 0 }
    val borrowNeeded = MutableList(digits) { false }
    val borrowSteps = MutableList(digits) { mutableListOf<Int>() }
    val res = MutableList(digits) { 0 }

    for (i in digits - 1 downTo 0) {
        val b = bottom[i]
        topBefore[i] = topAfter[i]
        if (topAfter[i] < b) {
            borrowNeeded[i] = true
            var j = i - 1
            while (j >= 0 && topAfter[j] == 0) j--
            if (j >= 0) {
                val updatedColumns = mutableListOf<Int>()
                topAfter[j] -= 1
                updatedColumns += j
                for (k in j + 1 until i) {
                    if (topAfter[k] == 0) {
                        topAfter[k] = 9
                        updatedColumns += k
                    }
                }
                topAfter[i] += 10
                updatedColumns += i
                borrowSteps[i].addAll(updatedColumns)
            }
        }
        res[i] = topAfter[i] - b
    }

    return ExpectedSub(
        topDigitsOriginal = top,
        bottomDigits = bottom,
        topDigitsAfterBorrow = topAfter,
        topDigitsBeforeBorrow = topBefore,
        borrowNeeded = borrowNeeded,
        resultDigits = res,
        borrowSteps = borrowSteps
    )
}

private fun buildStepsSub(expected: ExpectedSub): List<SubStep> {
    val digits = expected.topDigitsOriginal.size
    val steps = mutableListOf<SubStep>()
    for (i in digits - 1 downTo 0) {
        steps += SubStep(SubStepType.COMPARE, i)
        if (expected.borrowNeeded[i]) {
            steps += SubStep(SubStepType.BORROW_DECISION, i)
            steps += SubStep(SubStepType.BORROW_VALUE, i)
        }
        steps += SubStep(SubStepType.RESULT_DIGIT, i)
    }
    return steps
}

private fun colNameFromRight(posFromRight: Int): String = when (posFromRight) {
    0 -> "unità"
    1 -> "decine"
    2 -> "centinaia"
    3 -> "migliaia"
    else -> "colonna ${posFromRight + 1}"
}

private fun instructionSub(
    step: SubStep,
    digits: Int,
    expected: ExpectedSub,
    borrowVisible: List<Boolean>
): String {
    val col = step.colIndexFromLeft
    val posFromRight = (digits - 1) - col
    val colName = colNameFromRight(posFromRight)
    return when (step.type) {
        SubStepType.COMPARE -> {
            val top = expected.topDigitsBeforeBorrow[col]
            val bottom = expected.bottomDigits[col]
            "Nelle $colName: puoi fare $top − $bottom?"
        }
        SubStepType.BORROW_DECISION -> "Devi chiedere in prestito?"
        SubStepType.BORROW_VALUE -> {
            val top = expected.topDigitsBeforeBorrow[col]
            "Scrivi il prestito sopra il $top nelle $colName."
        }
        SubStepType.RESULT_DIGIT -> {
            val topAfterBorrow = expected.topDigitsAfterBorrow[col]
            val bottom = expected.bottomDigits[col]
            val result = (topAfterBorrow - bottom) % 10
            val showBorrow = borrowVisible.getOrNull(col) == true
            val top = if (showBorrow) topAfterBorrow else expected.topDigitsBeforeBorrow[col]
            "Calcola $top − $bottom nelle $colName e scrivi $result."
        }
    }
}

/* ---------------- UI helpers (solo per questo file, nomi “Sub*” per non conflitti) ---------------- */

@Composable
private fun SubStaticBox(
    text: String,
    size: Dp,
    active: Boolean,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColorOverride: Color? = null,
    borderWidthOverride: Dp? = null
) {
    val highlightColor = Color(0xFF22C55E)
    val borderColor = borderColorOverride
        ?: if (active) highlightColor else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = borderWidthOverride ?: if (active) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
private fun SubDigitInput(
    value: String,
    onChange: (String) -> Unit,
    size: Dp,
    enabled: Boolean,
    active: Boolean,
    bg: Color,
    status: Boolean?,
    maxDigits: Int = 1
) {
    val highlightColor = Color(0xFF22C55E)
    val border = when {
        status == false -> MaterialTheme.colorScheme.error
        status == true -> highlightColor
        active -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = when {
        status == false -> 2.dp
        status == true || active -> 3.dp
        else -> 2.dp
    }
    val bgColor = when {
        active -> MaterialTheme.colorScheme.tertiaryContainer
        value.isNotBlank() && status != false -> MaterialTheme.colorScheme.secondaryContainer
        else -> bg
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(if (active) 1.03f else 1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor.copy(alpha = 0.9f))
            .border(borderWidth, border, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(maxDigits)) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                disabledTextColor = Color(0xFF111827).copy(alpha = 0.6f),
                cursorColor = border
            )
        )
    }
}

/* ----------------------------- GAME ----------------------------- */

@Composable
fun LongSubtractionGame(
    digits: Int,
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
    val manualMinValue = 1
    val manualMaxValue = 999
    val manualMaxDigits = 3

    var manualA by remember { mutableStateOf("") }
    var manualB by remember { mutableStateOf("") }
    var manualNumbers by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val isHomeworkMode = exercise != null || onExerciseFinished != null

    var problem by remember(digits, startMode) {
        mutableStateOf(
            if (startMode == StartMode.RANDOM) generateSubtractionMixed(digits) else null
        )
    }

    fun manualDigitsFor(a: Int, b: Int): Int {
        return maxOf(a.toString().length, b.toString().length).coerceIn(1, manualMaxDigits)
    }

    val activeDigits = problem?.let { manualDigitsFor(it.a, it.b) } ?: digits

    val expected = remember(problem, activeDigits) { problem?.let { computeExpectedSub(it, activeDigits) } }
    val steps = remember(expected) { expected?.let { buildStepsSub(it) } ?: emptyList() }

    var stepIndex by remember(problem, activeDigits) { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    val currentStep = steps.getOrNull(stepIndex)
    val done = problem != null && currentStep == null
    var showSuccessDialog by remember { mutableStateOf(false) }
    var solutionRevealed by remember { mutableStateOf(false) }
    var attempts by remember(exercise?.a, exercise?.b) { mutableStateOf(0) }
    val wrongAnswers = remember(exercise?.a, exercise?.b) { mutableStateListOf<String>() }
    val stepErrors = remember(exercise?.a, exercise?.b) { mutableStateListOf<StepError>() }
    var solutionUsed by remember(exercise?.a, exercise?.b) { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    val inputGuard = remember { StepInputGuard() }

    var message by remember { mutableStateOf<String?>(null) }
    var waitTapToContinue by remember { mutableStateOf(false) }

    val resInputs = remember(problem, activeDigits) {
        mutableStateListOf<String>().apply { repeat(activeDigits) { add("") } }
    }

    // status: null/true/false
    val resOk = remember(problem, activeDigits) {
        mutableStateListOf<Boolean?>().apply { repeat(activeDigits) { add(null) } }
    }

    val borrowInputs = remember(problem, activeDigits) {
        mutableStateListOf<String>().apply { repeat(activeDigits) { add("") } }
    }

    val borrowOk = remember(problem, activeDigits) {
        mutableStateListOf<Boolean?>().apply { repeat(activeDigits) { add(null) } }
    }

    val borrowVisible = remember(problem, activeDigits) {
        mutableStateListOf<Boolean>().apply { repeat(activeDigits) { add(false) } }
    }

    fun resetSame() {
        stepIndex = 0
        message = null
        waitTapToContinue = false
        showSuccessDialog = false
        solutionRevealed = false
        attempts = 0
        wrongAnswers.clear()
        stepErrors.clear()
        solutionUsed = false
        for (i in resInputs.indices) resInputs[i] = ""
        for (i in resOk.indices) resOk[i] = null
        for (i in borrowInputs.indices) borrowInputs[i] = ""
        for (i in borrowOk.indices) borrowOk[i] = null
        for (i in borrowVisible.indices) borrowVisible[i] = false
        gameState = if (problem == null) GameState.INIT else GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    fun resetForNew() {
        if (startMode == StartMode.MANUAL) {
            manualA = ""
            manualB = ""
            manualNumbers = null
            problem = null
            resetSame()
        } else {
            problem = generateSubtractionMixed(digits)
            resetSame()
        }
    }

    fun revealSolution() {
        val expectedValues = expected ?: return
        solutionRevealed = true
        solutionUsed = true
        showSuccessDialog = false
        message = null
        waitTapToContinue = false
        stepIndex = steps.size
        for (i in 0 until activeDigits) {
            resInputs[i] = expectedValues.resultDigits[i].toString()
            resOk[i] = true
        }
    }

    fun playCorrect() { if (soundEnabled) fx.correct() }
    fun playWrong() { if (soundEnabled) fx.wrong() }

    fun resEnabled(col: Int): Boolean {
        if (waitTapToContinue) return false
        return currentStep?.type == SubStepType.RESULT_DIGIT && currentStep.colIndexFromLeft == col
    }

    fun tryValidateBorrow() {
        if (waitTapToContinue) return
        val s = currentStep ?: return
        val expectedValues = expected ?: return
        if (s.type != SubStepType.BORROW_VALUE) return
        message = null
        val col = s.colIndexFromLeft
        val stepId = "sub-borrow-$col"
        val expectedBorrow = expectedValues.topDigitsAfterBorrow[col]
        val validation = validateUserInput(
            stepId = stepId,
            value = borrowInputs[col],
            expectedRange = 10..19,
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
        gameState = GameState.VALIDATING
        val user = borrowInputs[col].toIntOrNull() ?: return
        val ok = user == expectedBorrow
        borrowOk[col] = ok
        if (!ok) {
            playWrong()
            attempts += 1
            stepErrors += StepError(
                stepLabel = BORROW_VALUE_ERROR,
                expected = expectedBorrow.toString(),
                actual = borrowInputs[col]
            )
            message = "❌ Quando prendi in prestito aggiungi 10."
            borrowInputs[col] = ""
            gameState = GameState.AWAITING_INPUT
            return
        }
        playCorrect()
        val revealColumns = expectedValues.borrowSteps[col].toMutableSet().apply { add(col) }
        revealColumns.forEach { idx ->
            if (idx in borrowVisible.indices) {
                borrowVisible[idx] = true
                borrowInputs[idx] = expectedValues.topDigitsAfterBorrow[idx].toString()
                borrowOk[idx] = true
            }
        }
        stepIndex++
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset(stepId)
    }

    fun tryValidateResult() {
        if (waitTapToContinue) return
        val s = currentStep ?: return
        val expectedValues = expected ?: return
        message = null

        when (s.type) {
            SubStepType.RESULT_DIGIT -> {
                val col = s.colIndexFromLeft
                val effectiveADigit = expectedValues.topDigitsAfterBorrow[col]
                val bDigit = expectedValues.bottomDigits[col]
                val exp = (effectiveADigit - bDigit) % 10
                val user = resInputs[col].toIntOrNull() ?: return
                val stepId = "sub-result-$col"
                val validation = validateUserInput(
                    stepId = stepId,
                    value = resInputs[col],
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
                gameState = GameState.VALIDATING
                val ok = user == exp
                resOk[col] = ok
                if (!ok) {
                    playWrong()
                    attempts += 1
                    if (resInputs[col].isNotBlank()) {
                        wrongAnswers += resInputs[col]
                        val posFromRight = (activeDigits - 1) - col
                        val colName = colNameFromRight(posFromRight)
                        stepErrors += StepError(
                            stepLabel = SUBTRACTION_CALCULATION_ERROR,
                            expected = exp.toString(),
                            actual = resInputs[col]
                        )
                    }
                    message = "❌ Riprova"
                    resInputs[col] = "" // cancella
                    val locked = inputGuard.registerAttempt(stepId)
                    gameState = GameState.AWAITING_INPUT
                    if (locked) {
                        resInputs[col] = exp.toString()
                        resOk[col] = true
                        message = "Continuiamo con il prossimo passo."
                        stepIndex++
                        inputGuard.reset()
                    }
                    return
                }
                playCorrect()
                stepIndex++
                gameState = GameState.AWAITING_INPUT
                inputGuard.reset()
            }
        }

        if (stepIndex >= steps.size) {
            message = "✅ Corretto!"
            gameState = GameState.GAME_COMPLETED
        }
    }

    val inputBg = Color(0xFFF3F4F6)

    val hint = when {
        expected == null -> "Inserisci i numeri e premi Avvia."
        helps?.hintsEnabled == false && !done -> "Completa l'operazione."
        !done -> instructionSub(currentStep!!, activeDigits, expected, borrowVisible)
        solutionRevealed -> "Soluzione: ${expected.resultDigits.joinToString("")}"
        else -> "Bravo! 🙂"
    }

    LaunchedEffect(done) {
        if (done && expected != null && !solutionRevealed) {
            showSuccessDialog = true
            if (!isHomeworkMode) {
                correctCount += 1
            }
        }
    }

    LaunchedEffect(exercise?.a, exercise?.b) {
        val a = exercise?.a
        val b = exercise?.b
        if (a != null && b != null) {
            manualNumbers = a to b
            problem = SubProblem(a, b)
            resetSame()
        }
    }

    LaunchedEffect(problem) {
        if (problem != null && gameState == GameState.INIT) {
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
        }
    }

    LaunchedEffect(currentStep) {
        if (currentStep?.type == SubStepType.BORROW_VALUE) {
            val col = currentStep.colIndexFromLeft
            if (col in borrowInputs.indices) {
                borrowInputs[col] = ""
                borrowOk[col] = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val boxSize = if (ui.isCompact) 46.dp else 56.dp
        val gap = if (ui.isCompact) 6.dp else 10.dp
        val minusWidth = if (ui.isCompact) 22.dp else 28.dp
        val minusSize = if (ui.isCompact) 22.sp else 26.sp
        val hintFont = if (ui.isCompact) 10.sp else 12.sp

        GameScreenFrame(
            title = "Sottrazioni in colonna",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            bonusTarget = BONUS_TARGET_LONG_ADD_SUB,
            hintText = hint,
            ui = ui,
            message = message,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                if (startMode == StartMode.MANUAL && !isHomeworkMode) {
                    val manualAValue = manualA.toIntOrNull()
                    val manualBValue = manualB.toIntOrNull()
                    val manualValid = manualAValue in manualMinValue..manualMaxValue &&
                        manualBValue in manualMinValue..manualMaxValue &&
                        (manualAValue ?: 0) > (manualBValue ?: 0)
                    val manualError = if (manualValid || (manualA.isBlank() && manualB.isBlank())) {
                        null
                    } else {
                        "Inserisci A e B tra $manualMinValue e $manualMaxValue (B < A)."
                    }

                    SeaGlassPanel(title = "Inserimento") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualA,
                                onValueChange = { manualA = it.filter { c -> c.isDigit() }.take(manualMaxDigits) },
                                label = { Text("Numero A") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = manualB,
                                onValueChange = { manualB = it.filter { c -> c.isDigit() }.take(manualMaxDigits) },
                                label = { Text("Numero B") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Limiti inserimento: $manualMinValue - $manualMaxValue (A > B)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                            androidx.compose.material3.Button(
                                onClick = {
                                    val aVal = manualAValue ?: return@Button
                                    val bVal = manualBValue ?: return@Button
                                    manualNumbers = aVal to bVal
                                    problem = SubProblem(aVal, bVal)
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

                SeaGlassPanel(title = "Operazione in colonna") {
                    if (expected == null) {
                        Text("Inserisci i numeri e premi Avvia.")
                    } else {
                        @Composable fun BlankBox() { Box(Modifier.size(boxSize)) }

                        // Riga “prestito” (visibile solo nel passo dedicato o dopo conferma)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BlankBox()
                            Spacer(Modifier.width(gap))
                            for (col in 0 until activeDigits) {
                                val isBorrowStep = currentStep?.type == SubStepType.BORROW_VALUE &&
                                    currentStep?.colIndexFromLeft == col
                                val showBorrow = borrowVisible[col] && !isBorrowStep
                                if (isBorrowStep) {
                                    SubDigitInput(
                                        value = borrowInputs[col],
                                        onChange = { v ->
                                            borrowInputs[col] = v
                                        },
                                        size = boxSize,
                                        enabled = true,
                                        active = true,
                                        bg = MaterialTheme.colorScheme.tertiaryContainer,
                                        status = borrowOk[col],
                                        maxDigits = 2
                                    )
                                } else if (showBorrow) {
                                    val active = currentStep?.colIndexFromLeft == col
                                    SubStaticBox(
                                        text = borrowInputs[col],
                                        size = boxSize,
                                        active = active,
                                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        borderColorOverride = if (active) {
                                            Color(0xFF22C55E)
                                        } else {
                                            MaterialTheme.colorScheme.tertiary
                                        },
                                        borderWidthOverride = if (active) 3.dp else 2.dp
                                    )
                                } else {
                                    Box(Modifier.size(boxSize))
                                }
                                Spacer(Modifier.width(gap))
                            }
                        }

                        // Due righe numeri + simbolo a destra centrato
                        Row(verticalAlignment = Alignment.Top) {
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                // Riga TOP originale
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BlankBox()
                                    Spacer(Modifier.width(gap))
                                    for (col in 0 until activeDigits) {
                                        val active = currentStep?.colIndexFromLeft == col
                                        val showBorrow = borrowVisible[col]
                                        val dimmedText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        val dimmedBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

                                        SubStaticBox(
                                            text = expected.topDigitsOriginal[col].toString(),
                                            size = boxSize,
                                            active = active,
                                            textColor = if (showBorrow) dimmedText else MaterialTheme.colorScheme.onSurface,
                                            backgroundColor = if (showBorrow) dimmedBackground else MaterialTheme.colorScheme.surfaceVariant,
                                            borderColorOverride = if (showBorrow) {
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                            } else {
                                                null
                                            }
                                        )
                                        Spacer(Modifier.width(gap))
                                    }
                                }

                                // Riga BOTTOM
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BlankBox()
                                    Spacer(Modifier.width(gap))
                                    for (col in 0 until activeDigits) {
                                        val active = currentStep?.colIndexFromLeft == col

                                        SubStaticBox(
                                            text = expected.bottomDigits[col].toString(),
                                            size = boxSize,
                                            active = active
                                        )
                                        Spacer(Modifier.width(gap))
                                    }
                                }
                            }

                            // simbolo a destra tra le due righe
                            Box(
                                modifier = Modifier
                                    .width(minusWidth)
                                    .height(boxSize + gap + boxSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "−",
                                    fontWeight = FontWeight.Black,
                                    fontSize = minusSize,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF111827).copy(alpha = 0.25f))

                        // Riga risultato
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BlankBox()
                            Spacer(Modifier.width(gap))
                            for (col in 0 until activeDigits) {
                                val enabled = resEnabled(col)
                                SubDigitInput(
                                    value = resInputs[col],
                                    onChange = { v ->
                                        resInputs[col] = v
                                        if (enabled) tryValidateResult()
                                    },
                                    size = boxSize,
                                    enabled = enabled,
                                    active = enabled,
                                    bg = inputBg,
                                    status = resOk[col]
                                )
                                Spacer(Modifier.width(gap))
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (currentStep?.type) {
                                SubStepType.COMPARE -> {
                                    Text(
                                        "Puoi fare ${expected.topDigitsBeforeBorrow[currentStep.colIndexFromLeft]} − " +
                                            "${expected.bottomDigits[currentStep.colIndexFromLeft]}?",
                                        color = Color(0xFF6B7280),
                                        fontSize = hintFont
                                    )
                                    Button(onClick = {
                                        message = null
                                        stepIndex++
                                    }) {
                                        Text("Continua")
                                    }
                                }
                                SubStepType.BORROW_DECISION -> {
                                    Text(
                                        "Devi chiedere in prestito?",
                                        color = Color(0xFF6B7280),
                                        fontSize = hintFont
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            message = null
                                            stepIndex++
                                        }) { Text("Sì") }
                                        Button(
                                            onClick = {
                                                playWrong()
                                                attempts += 1
                                                stepErrors += StepError(
                                                    stepLabel = BORROW_DECISION_ERROR,
                                                    expected = "Sì",
                                                    actual = "No"
                                                )
                                                message = "❌ A è più piccolo di B, serve un prestito."
                                            }
                                        ) {
                                            Text("No")
                                        }
                                    }
                                }
                                SubStepType.BORROW_VALUE -> {
                                    Text(
                                        "Scrivi il prestito nella casella sopra.",
                                        color = Color(0xFF6B7280),
                                        fontSize = hintFont
                                    )
                                    val col = currentStep.colIndexFromLeft
                                    val enabled = borrowInputs[col].length >= 2
                                    Button(
                                        onClick = { tryValidateBorrow() },
                                        enabled = enabled
                                    ) {
                                        Text("Conferma prestito")
                                    }
                                }
                                else -> {
                                    Text(
                                        "Le caselle grigie sono per il risultato.",
                                        color = Color(0xFF6B7280),
                                        fontSize = hintFont
                                    )
                                }
                            }
                        }
                    }
                }

                SeaGlassPanel(title = "Aiuto") {
                    val totalSteps = steps.size
                    val stepLabel = when {
                        totalSteps == 0 -> "Passo 0/0"
                        done -> "Passo $totalSteps/$totalSteps"
                        else -> "Passo ${stepIndex + 1}/$totalSteps"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)) {
                        Text(text = hint, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = stepLabel,
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
                            if (manual != null && problem != null) {
                                problem = SubProblem(manual.first, manual.second)
                                resetSame()
                            } else {
                                manualA = ""
                                manualB = ""
                                manualNumbers = null
                                problem = null
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
                            resetForNew()
                        }
                    },
                    center = {
                        Button(
                            onClick = { revealSolution() },
                            enabled = expected != null && (helps?.allowSolution != false),
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
                    resetForNew()
                }
            },
            onDismiss = { showSuccessDialog = false },
            resultText = expected?.resultDigits?.joinToString("").orEmpty(),
            confirmText = if (isHomeworkMode) "Avanti" else "Nuova operazione"
        )

        if (!isHomeworkMode) {
            BonusRewardHost(
                correctCount = correctCount,
                rewardsEarned = rewardsEarned,
                rewardEvery = BONUS_TARGET_LONG_ADD_SUB,
                soundEnabled = soundEnabled,
                fx = fx,
                onOpenLeaderboard = onOpenLeaderboardFromBonus,
                onBonusPromptAction = { showSuccessDialog = false },
                onRewardEarned = { rewardsEarned += 1 },
                onRewardSkipped = { rewardsEarned += 1 }
            )
        }

        // Overlay tap-to-continue
        if (waitTapToContinue) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable {
                        waitTapToContinue = false
                        message = null
                        resetForNew()
                    },
                contentAlignment = Alignment.Center
            ) {
                SeaGlassPanel(title = "✅ Corretto!") {
                    Text("Tappa per continuare", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
