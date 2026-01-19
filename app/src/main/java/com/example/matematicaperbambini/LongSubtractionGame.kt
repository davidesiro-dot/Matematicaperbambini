package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

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
    val resultDigits: List<Int>,
    val borrowChanged: List<Boolean>     // true se quella cifra “sopra” cambia
)

private enum class SubStepType { BORROW_NEW_TOP_DIGIT, RESULT_DIGIT }
private data class SubStep(val type: SubStepType, val colIndexFromLeft: Int)

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
    val changed = MutableList(digits) { false }
    val res = MutableList(digits) { 0 }

    for (i in digits - 1 downTo 0) {
        val b = bottom[i]
        if (topAfter[i] < b) {
            var j = i - 1
            while (j >= 0 && topAfter[j] == 0) j--
            if (j >= 0) {
                topAfter[j] -= 1
                changed[j] = true
                for (k in j + 1 until i) {
                    if (topAfter[k] == 0) {
                        topAfter[k] = 9
                        changed[k] = true
                    }
                }
                topAfter[i] += 10
                changed[i] = true
            }
        }
        res[i] = topAfter[i] - b
    }

    return ExpectedSub(
        topDigitsOriginal = top,
        bottomDigits = bottom,
        topDigitsAfterBorrow = topAfter,
        resultDigits = res,
        borrowChanged = changed
    )
}

private fun buildStepsSub(expected: ExpectedSub): List<SubStep> {
    val digits = expected.topDigitsOriginal.size
    val steps = mutableListOf<SubStep>()
    for (i in digits - 1 downTo 0) {
        val left = i - 1
        // se la cifra a sinistra cambia per il prestito, prima la scrivo “sopra”
        if (left >= 0 && expected.borrowChanged[left]) {
            steps += SubStep(SubStepType.BORROW_NEW_TOP_DIGIT, left)
        }
        // poi scrivo la cifra del risultato in colonna i
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

private fun instructionSub(step: SubStep, digits: Int): String = when (step.type) {
    SubStepType.BORROW_NEW_TOP_DIGIT -> {
        val posFromRight = (digits - 1) - step.colIndexFromLeft
        "Scrivi il nuovo numero sopra nelle ${colNameFromRight(posFromRight)} (dopo il prestito)"
    }
    SubStepType.RESULT_DIGIT -> {
        val posFromRight = (digits - 1) - step.colIndexFromLeft
        "Scrivi la cifra del risultato nelle ${colNameFromRight(posFromRight)}"
    }
}

/* ---------------- UI helpers (solo per questo file, nomi “Sub*” per non conflitti) ---------------- */

@Composable
private fun SubStaticBox(text: String, size: Dp, active: Boolean) {
    val borderColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    else Color.White.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.80f))
            .border(2.dp, borderColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
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
    status: Boolean?
) {
    val border = when {
        status == false -> Color(0xFFEF4444)
        status == true -> Color(0xFF10B981)
        active -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(if (active) 1.03f else 1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(1)) },
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
                focusedBorderColor = border,
                unfocusedBorderColor = border,
                disabledBorderColor = border.copy(alpha = 0.35f),
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
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    var problem by remember(digits) { mutableStateOf(generateSubtractionMixed(digits)) }
    val expected = remember(problem, digits) { computeExpectedSub(problem, digits) }
    val steps = remember(expected) { buildStepsSub(expected) }

    var stepIndex by remember(problem, digits) { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    val currentStep = steps.getOrNull(stepIndex)
    val done = currentStep == null
    var showSuccessDialog by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf<String?>(null) }
    var waitTapToContinue by remember { mutableStateOf(false) }

    // input: riga “nuovi numeri sopra” + riga risultato
    val topNewInputs = remember(problem, digits) { mutableStateListOf<String>().apply { repeat(digits) { add("") } } }
    val resInputs = remember(problem, digits) { mutableStateListOf<String>().apply { repeat(digits) { add("") } } }

    // status: null/true/false
    val topOk = remember(problem, digits) { mutableStateListOf<Boolean?>().apply { repeat(digits) { add(null) } } }
    val resOk = remember(problem, digits) { mutableStateListOf<Boolean?>().apply { repeat(digits) { add(null) } } }

    fun resetSame() {
        stepIndex = 0
        message = null
        waitTapToContinue = false
        showSuccessDialog = false
        for (i in topNewInputs.indices) topNewInputs[i] = ""
        for (i in resInputs.indices) resInputs[i] = ""
        for (i in topOk.indices) topOk[i] = null
        for (i in resOk.indices) resOk[i] = null
    }

    fun resetForNew() {
        problem = generateSubtractionMixed(digits)
        resetSame()
    }

    fun playCorrect() { if (soundEnabled) fx.correct() }
    fun playWrong() { if (soundEnabled) fx.wrong() }

    fun topEnabled(col: Int): Boolean {
        if (waitTapToContinue) return false
        return currentStep?.type == SubStepType.BORROW_NEW_TOP_DIGIT && currentStep.colIndexFromLeft == col
    }

    fun resEnabled(col: Int): Boolean {
        if (waitTapToContinue) return false
        return currentStep?.type == SubStepType.RESULT_DIGIT && currentStep.colIndexFromLeft == col
    }

    fun tryValidate() {
        if (waitTapToContinue) return
        val s = currentStep ?: return
        message = null

        when (s.type) {
            SubStepType.BORROW_NEW_TOP_DIGIT -> {
                val col = s.colIndexFromLeft
                val exp = expected.topDigitsAfterBorrow[col]
                val user = topNewInputs[col].toIntOrNull() ?: return
                val ok = user == exp
                topOk[col] = ok
                if (!ok) {
                    playWrong()
                    topNewInputs[col] = "" // cancella
                    message = "❌ Riprova"
                    return
                }
                playCorrect()
                stepIndex++
                correctCount += 1
            }

            SubStepType.RESULT_DIGIT -> {
                val col = s.colIndexFromLeft
                val exp = expected.resultDigits[col]
                val user = resInputs[col].toIntOrNull() ?: return
                val ok = user == exp
                resOk[col] = ok
                if (!ok) {
                    playWrong()
                    resInputs[col] = "" // cancella
                    message = "❌ Riprova"
                    return
                }
                playCorrect()
                stepIndex++
                correctCount += 1
            }
        }

        if (stepIndex >= steps.size) {
            message = "✅ Corretto!"
        }
    }

    val borrowBg = Color(0xFFE0F2FE)
    val inputBg = Color(0xFFF3F4F6)

    val hint = if (!done) instructionSub(currentStep!!, digits) else "Bravo! 🙂"

    LaunchedEffect(done) {
        if (done) {
            showSuccessDialog = true
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
            hintText = hint,
            ui = ui,
            message = message,
            content = {
                SeaGlassPanel(title = "Operazione in colonna") {
                    @Composable fun BlankBox() { Box(Modifier.size(boxSize)) }

                    // Riga “nuovi numeri sopra” (solo dove serve)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BlankBox()
                        Spacer(Modifier.width(gap))
                        for (col in 0 until digits) {
                            val enabled = topEnabled(col)
                            val show = expected.borrowChanged[col] || enabled
                            if (show) {
                                SubDigitInput(
                                    value = topNewInputs[col],
                                    onChange = { v ->
                                        topNewInputs[col] = v
                                        if (enabled) tryValidate()
                                    },
                                    size = boxSize,
                                    enabled = enabled,
                                    active = enabled,
                                    bg = borrowBg,
                                    status = topOk[col]
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
                                for (col in 0 until digits) {
                                    val active =
                                        (currentStep?.colIndexFromLeft == col) &&
                                                (currentStep?.type == SubStepType.RESULT_DIGIT || currentStep?.type == SubStepType.BORROW_NEW_TOP_DIGIT)

                                    SubStaticBox(
                                        text = expected.topDigitsOriginal[col].toString(),
                                        size = boxSize,
                                        active = active
                                    )
                                    Spacer(Modifier.width(gap))
                                }
                            }

                            // Riga BOTTOM
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BlankBox()
                                Spacer(Modifier.width(gap))
                                for (col in 0 until digits) {
                                    val active =
                                        (currentStep?.colIndexFromLeft == col) &&
                                                (currentStep?.type == SubStepType.RESULT_DIGIT)

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
                        for (col in 0 until digits) {
                            val enabled = resEnabled(col)
                            SubDigitInput(
                                value = resInputs[col],
                                onChange = { v ->
                                    resInputs[col] = v
                                    if (enabled) tryValidate()
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

                    Text(
                        "Le caselle azzurre sono per il cambio (prestito).",
                        color = Color(0xFF6B7280),
                        fontSize = hintFont
                    )
                }

                SeaGlassPanel(title = "Stato") {
                    Text(
                        if (done) "Operazione completata." else "Passo ${stepIndex + 1}/${steps.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            bottomBar = {
                GameBottomActions(
                    leftText = "Ricomincia",
                    onLeft = { resetSame() },
                    rightText = "Nuovo",
                    onRight = { resetForNew() }
                )
            }
        )

        SuccessDialog(
            show = showSuccessDialog,
            onNew = {
                showSuccessDialog = false
                resetForNew()
            },
            onDismiss = { showSuccessDialog = false },
            resultText = expected.resultDigits.joinToString("")
        )

        BonusRewardHost(
            correctCount = correctCount,
            rewardsEarned = rewardsEarned,
            soundEnabled = soundEnabled,
            fx = fx,
            onOpenLeaderboard = onOpenLeaderboardFromBonus,
            onRewardEarned = { rewardsEarned += 1 },
            onRewardSkipped = { rewardsEarned += 1 }
        )

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
