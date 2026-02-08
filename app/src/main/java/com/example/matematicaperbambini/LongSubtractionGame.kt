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
    val resultDigits: List<Int>
)

private const val BORROW_CHAIN_ERROR = "BORROW_CHAIN_ERROR"
private const val BORROW_VALUE_ERROR = "BORROW_VALUE_ERROR"
private const val BORROW_TARGET_ERROR = "BORROW_TARGET_ERROR"
private const val SUBTRACTION_CALCULATION_ERROR = "SUBTRACTION_CALCULATION_ERROR"

private enum class BorrowPhase {
    NONE,
    SOURCE_INPUT,
    TARGET_INPUT
}

private enum class DigitVisualState {
    ACTIVE,
    CONSUMED_BY_BORROW
}

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
    val top = problem.a.toString().padStart(digits, '0').map { it.digitToInt() }
    val bottom = problem.b.toString().padStart(digits, '0').map { it.digitToInt() }
    val res = (problem.a - problem.b).toString().padStart(digits, '0').map { it.digitToInt() }
    return ExpectedSub(
        topDigitsOriginal = top,
        bottomDigits = bottom,
        resultDigits = res
    )
}

private fun colNameFromRight(posFromRight: Int): String {
    if (posFromRight == 0) return "unità"
    if (posFromRight == 1) return "decine"
    if (posFromRight == 2) return "centinaia"
    if (posFromRight == 3) return "migliaia"
    return "colonna ${posFromRight + 1}"
}

private fun instructionSub(
    currentColumn: Int,
    digits: Int,
    expected: ExpectedSub,
    workingTopDigits: List<Int>,
    borrowPending: Boolean,
    borrowSource: Int?,
    borrowPhase: BorrowPhase
): String {
    val posFromRight = (digits - 1) - currentColumn
    val colName = colNameFromRight(posFromRight)
    if (borrowPending) {
        if (borrowPhase == BorrowPhase.TARGET_INPUT) {
            val top = workingTopDigits[currentColumn]
            return "La colonna $colName è ora $top + 10. Scrivi il nuovo numero."
        }
        val source = borrowSource ?: currentColumn
        val sourcePos = (digits - 1) - source
        val sourceName = colNameFromRight(sourcePos)
        return "Non puoi sottrarre nelle $colName. Guarda $sourceName e scrivi il numero diminuito."
    }
    val top = workingTopDigits[currentColumn]
    val bottom = expected.bottomDigits[currentColumn]
    return "Calcola $top − $bottom nelle $colName."
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
    val highlightsEnabled = helps?.highlightsEnabled != false
    val isChallengeMode = isChallengeMode(isHomeworkMode, helps)
    val guideHighlightsAllowed = shouldHighlightGuideCell(
        isInputCell = false,
        isCurrentStepInput = false,
        isChallengeMode = isChallengeMode,
        isHomeworkMode = isHomeworkMode,
        highlightsEnabled = highlightsEnabled
    )

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

    var currentColumn by remember(problem, activeDigits) { mutableStateOf(activeDigits - 1) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    val done = problem != null && currentColumn < 0
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

    val borrowTargetInputs = remember(problem, activeDigits) {
        mutableStateListOf<String>().apply { repeat(activeDigits) { add("") } }
    }

    val borrowTargetOk = remember(problem, activeDigits) {
        mutableStateListOf<Boolean?>().apply { repeat(activeDigits) { add(null) } }
    }

    val borrowVisible = remember(problem, activeDigits) {
        mutableStateListOf<Boolean>().apply { repeat(activeDigits) { add(false) } }
    }

    val workingTopDigits = remember(problem, activeDigits) {
        mutableStateListOf<Int>().apply { repeat(activeDigits) { add(0) } }
    }

    fun resetSame() {
        currentColumn = activeDigits - 1
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
        for (i in borrowTargetInputs.indices) borrowTargetInputs[i] = ""
        for (i in borrowTargetOk.indices) borrowTargetOk[i] = null
        for (i in borrowVisible.indices) borrowVisible[i] = false
        for (i in workingTopDigits.indices) workingTopDigits[i] = expected?.topDigitsOriginal?.get(i) ?: 0
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
        currentColumn = -1
        for (i in 0 until activeDigits) {
            resInputs[i] = expectedValues.resultDigits[i].toString()
            resOk[i] = true
        }
    }

    fun playCorrect() { if (soundEnabled) fx.correct() }
    fun playWrong() { if (soundEnabled) fx.wrong() }

    fun resEnabled(col: Int, borrowPending: Boolean): Boolean {
        if (waitTapToContinue) return false
        return col == currentColumn && !borrowPending
    }

    fun tryValidateBorrow(currentCol: Int, sourceCol: Int) {
        if (waitTapToContinue) return
        if (currentCol != currentColumn) return
        message = null
        val stepId = "sub-borrow-$currentCol-$sourceCol"
        val expectedBorrow = (workingTopDigits.getOrNull(sourceCol) ?: return) - 1
        val validation = validateUserInput(
            stepId = stepId,
            value = borrowInputs[sourceCol],
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
        val user = borrowInputs[sourceCol].toIntOrNull() ?: return
        val ok = user == expectedBorrow
        borrowOk[sourceCol] = ok
        if (!ok) {
            playWrong()
            attempts += 1
            val errorLabel = if (sourceCol < currentCol - 1) BORROW_CHAIN_ERROR else BORROW_VALUE_ERROR
            val expectedLabel = if (errorLabel == BORROW_CHAIN_ERROR) {
                val sourceName = colNameFromRight((activeDigits - 1) - sourceCol)
                val targetName = colNameFromRight((activeDigits - 1) - (sourceCol + 1))
                "$sourceName->$targetName"
            } else {
                expectedBorrow.toString()
            }
            stepErrors += StepError(
                stepLabel = errorLabel,
                expected = expectedLabel,
                actual = borrowInputs[sourceCol]
            )
            message = "Guarda il numero a sinistra."
            borrowInputs[sourceCol] = ""
            borrowOk[sourceCol] = false
            gameState = GameState.AWAITING_INPUT
            return
        }
        playCorrect()
        workingTopDigits[sourceCol] = expectedBorrow
        val updatedColumns = mutableSetOf<Int>()
        updatedColumns += sourceCol
        for (k in sourceCol + 1 until currentCol) {
            if (workingTopDigits[k] == 0) {
                workingTopDigits[k] = 9
                updatedColumns += k
            }
        }
        updatedColumns.forEach { idx ->
            if (idx in borrowVisible.indices) {
                borrowVisible[idx] = true
                borrowInputs[idx] = workingTopDigits[idx].toString()
                borrowOk[idx] = true
            }
        }
        borrowTargetInputs[currentCol] = ""
        borrowTargetOk[currentCol] = null
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset(stepId)
    }

    fun tryValidateBorrowTarget() {
        if (waitTapToContinue) return
        if (currentColumn < 0) return
        message = null
        val col = currentColumn
        val expectedBorrowTarget = (workingTopDigits.getOrNull(col) ?: return) + 10
        val stepId = "sub-borrow-target-$col"
        val validation = validateUserInput(
            stepId = stepId,
            value = borrowTargetInputs[col],
            expectedRange = 0..99,
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
        val user = borrowTargetInputs[col].toIntOrNull() ?: return
        val ok = user == expectedBorrowTarget
        borrowTargetOk[col] = ok
        if (!ok) {
            playWrong()
            attempts += 1
            stepErrors += StepError(
                stepLabel = BORROW_TARGET_ERROR,
                expected = expectedBorrowTarget.toString(),
                actual = borrowTargetInputs[col]
            )
            message = "Calcola il numero dopo il prestito."
            borrowTargetInputs[col] = ""
            borrowTargetOk[col] = false
            gameState = GameState.AWAITING_INPUT
            return
        }
        playCorrect()
        workingTopDigits[col] = expectedBorrowTarget
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset(stepId)
    }

    fun tryValidateResult() {
        if (waitTapToContinue) return
        val expectedValues = expected ?: return
        message = null
        val col = currentColumn
        if (col < 0) return
        val exp = expectedValues.resultDigits[col]
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
                stepErrors += StepError(
                    stepLabel = SUBTRACTION_CALCULATION_ERROR,
                    expected = exp.toString(),
                    actual = resInputs[col]
                )
            }
            message = "❌ Riprova"
            resInputs[col] = ""
            val locked = inputGuard.registerAttempt(stepId)
            gameState = GameState.AWAITING_INPUT
            if (locked) {
                resInputs[col] = exp.toString()
                resOk[col] = true
                message = "Continuiamo con il prossimo passo."
                currentColumn -= 1
                inputGuard.reset()
            }
            return
        }
        playCorrect()
        currentColumn -= 1
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()

        if (currentColumn < 0) {
            message = "✅ Corretto!"
            gameState = GameState.GAME_COMPLETED
        }
    }

    val inputBg = Color(0xFFF3F4F6)

    val borrowNeeded = expected != null &&
        currentColumn >= 0 &&
        workingTopDigits[currentColumn] < expected.bottomDigits[currentColumn]
    val borrowSource = if (borrowNeeded) {
        (currentColumn - 1 downTo 0).firstOrNull { workingTopDigits[it] > 0 }
    } else {
        null
    }
    val borrowSourceDone = borrowSource != null && borrowOk.getOrNull(borrowSource) == true
    val borrowTargetDone = currentColumn >= 0 && borrowTargetOk.getOrNull(currentColumn) == true
    val borrowPhase = when {
        !borrowNeeded -> BorrowPhase.NONE
        borrowSourceDone && !borrowTargetDone -> BorrowPhase.TARGET_INPUT
        !borrowSourceDone -> BorrowPhase.SOURCE_INPUT
        else -> BorrowPhase.NONE
    }
    val borrowPending = borrowNeeded && borrowPhase != BorrowPhase.NONE

    val hint = if (expected == null) {
        "Inserisci i numeri e premi Avvia."
    } else if (helps?.hintsEnabled == false && !done) {
        "Completa l'operazione."
    } else if (!done && !solutionRevealed) {
        instructionSub(
            currentColumn = currentColumn,
            digits = activeDigits,
            expected = expected,
            workingTopDigits = workingTopDigits,
            borrowPending = borrowPending,
            borrowSource = borrowSource,
            borrowPhase = borrowPhase
        )
    } else if (solutionRevealed) {
        "Soluzione: ${expected.resultDigits.joinToString("")}"
    } else {
        "Bravo! 🙂"
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

    LaunchedEffect(borrowSource, borrowPending, borrowPhase, currentColumn) {
        val source = borrowSource
        if (borrowPending && source != null && source in borrowInputs.indices) {
            if (!borrowVisible[source]) {
                borrowInputs[source] = ""
                borrowOk[source] = null
            }
        }
        if (borrowPending && borrowPhase == BorrowPhase.TARGET_INPUT) {
            if (currentColumn in borrowTargetInputs.indices && borrowTargetOk[currentColumn] == null) {
                borrowTargetInputs[currentColumn] = borrowTargetInputs[currentColumn].take(2)
            }
        }
    }

    LaunchedEffect(expected, activeDigits) {
        val digitsList = expected?.topDigitsOriginal ?: return@LaunchedEffect
        for (i in 0 until activeDigits) {
            workingTopDigits[i] = digitsList.getOrElse(i) { 0 }
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
                                text = "Limiti inserimento: $manualMinValue - $manualMaxValue (A > B)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        // Riga “prestito” (compare automaticamente quando serve)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BlankBox()
                            Spacer(Modifier.width(gap))
                            for (col in 0 until activeDigits) {
                                val isActiveBorrowSource = borrowPending &&
                                    borrowPhase == BorrowPhase.SOURCE_INPUT &&
                                    borrowSource == col
                                val isActiveBorrowTarget = borrowPending &&
                                    borrowPhase == BorrowPhase.TARGET_INPUT &&
                                    col == currentColumn
                                val showBorrowSource = borrowVisible[col] && !isActiveBorrowSource
                                val showBorrowTarget = borrowTargetOk[col] == true && !isActiveBorrowTarget
                                val showBorrowSpace = borrowPending &&
                                    borrowPhase == BorrowPhase.SOURCE_INPUT &&
                                    col == currentColumn
                                if (isActiveBorrowSource) {
                                    SubDigitInput(
                                        value = borrowInputs[col],
                                        onChange = { v ->
                                            borrowInputs[col] = v
                                            if (v.length == 1) {
                                                tryValidateBorrow(currentColumn, col)
                                            }
                                        },
                                        size = boxSize,
                                        enabled = true,
                                        active = true,
                                        bg = MaterialTheme.colorScheme.tertiaryContainer,
                                        status = borrowOk[col],
                                        maxDigits = 1
                                    )
                                } else if (isActiveBorrowTarget) {
                                    SubDigitInput(
                                        value = borrowTargetInputs[col],
                                        onChange = { v ->
                                            borrowTargetInputs[col] = v
                                            if (v.length == 2) {
                                                tryValidateBorrowTarget()
                                            }
                                        },
                                        size = boxSize,
                                        enabled = true,
                                        active = true,
                                        bg = MaterialTheme.colorScheme.tertiaryContainer,
                                        status = borrowTargetOk[col],
                                        maxDigits = 2
                                    )
                                } else if (showBorrowTarget) {
                                    val active = col == currentColumn
                                    val guideActive = active && guideHighlightsAllowed
                                    SubStaticBox(
                                        text = borrowTargetInputs[col],
                                        size = boxSize,
                                        active = guideActive,
                                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        borderColorOverride = if (guideActive) {
                                            Color(0xFF22C55E)
                                        } else {
                                            MaterialTheme.colorScheme.tertiary
                                        },
                                        borderWidthOverride = if (guideActive) 3.dp else 2.dp
                                    )
                                } else if (showBorrowSource) {
                                    val active = col == currentColumn
                                    val guideActive = active && guideHighlightsAllowed
                                    SubStaticBox(
                                        text = borrowInputs[col],
                                        size = boxSize,
                                        active = guideActive,
                                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        borderColorOverride = if (guideActive) {
                                            Color(0xFF22C55E)
                                        } else {
                                            MaterialTheme.colorScheme.tertiary
                                        },
                                        borderWidthOverride = if (guideActive) 3.dp else 2.dp
                                    )
                                } else if (showBorrowSpace) {
                                    SubStaticBox(
                                        text = "",
                                        size = boxSize,
                                        active = guideHighlightsAllowed,
                                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        borderColorOverride = if (guideHighlightsAllowed) Color(0xFF22C55E) else MaterialTheme.colorScheme.tertiary,
                                        borderWidthOverride = 2.dp
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
                                        val active = col == currentColumn
                                        val guideActive = active && guideHighlightsAllowed
                                        val isActiveBorrowSource = borrowPending &&
                                            borrowPhase == BorrowPhase.SOURCE_INPUT &&
                                            borrowSource == col
                                        val isActiveBorrowTarget = borrowPending &&
                                            borrowPhase == BorrowPhase.TARGET_INPUT &&
                                            col == currentColumn
                                        val isConsumedByBorrow = borrowVisible[col] ||
                                            borrowTargetOk[col] == true ||
                                            isActiveBorrowSource ||
                                            isActiveBorrowTarget
                                        val visualState = if (isConsumedByBorrow) {
                                            DigitVisualState.CONSUMED_BY_BORROW
                                        } else {
                                            DigitVisualState.ACTIVE
                                        }
                                        val dimmedText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        val dimmedBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

                                        SubStaticBox(
                                            text = expected.topDigitsOriginal[col].toString(),
                                            size = boxSize,
                                            active = guideActive,
                                            textColor = if (visualState == DigitVisualState.CONSUMED_BY_BORROW) {
                                                dimmedText
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            backgroundColor = if (visualState == DigitVisualState.CONSUMED_BY_BORROW) {
                                                dimmedBackground
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            borderColorOverride = if (visualState == DigitVisualState.CONSUMED_BY_BORROW) {
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
                                        val active = col == currentColumn
                                        val guideActive = active && guideHighlightsAllowed

                                        SubStaticBox(
                                            text = expected.bottomDigits[col].toString(),
                                            size = boxSize,
                                            active = guideActive
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
                                val enabled = resEnabled(col, borrowPending)
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
                            val inlineText = if (borrowPending) {
                                if (borrowPhase == BorrowPhase.TARGET_INPUT) {
                                    val top = workingTopDigits[currentColumn]
                                    "La colonna corrente vale $top + 10. Scrivi il numero completo nella casella verde."
                                } else {
                                    "Scrivi il numero diminuito nella colonna a sinistra."
                                }
                            } else {
                                "Scrivi il risultato nella casella grigia."
                            }
                            Text(
                                inlineText,
                                color = Color(0xFF6B7280),
                                fontSize = hintFont
                            )
                        }
                    }
                }

                SeaGlassPanel(title = "Aiuto") {
                    val totalColumns = activeDigits
                    val currentStepNumber = if (done) {
                        totalColumns
                    } else {
                        (totalColumns - currentColumn).coerceIn(1, totalColumns)
                    }
                    val stepLabel = "Colonna $currentStepNumber/$totalColumns"
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
