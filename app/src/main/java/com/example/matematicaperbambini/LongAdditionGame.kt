package com.example.matematicaperbambini

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.random.Random

private enum class AddRowKey { CARRY, A, B, SUM }
private enum class AddCellKind { CARRY, DIGIT }

private data class AddTarget(
    val row: AddRowKey,
    val col: Int,           // 0..N (da sinistra)
    val kind: AddCellKind,
    val expected: Char,
    val hint: String
)

private data class AddPlan(
    val digits: Int,
    val aStr: String,       // es " 47" o "347" (sempre N)
    val bStr: String,       // sempre N
    val carry: CharArray,   // N (carry che entra nella colonna, quindi carry[0] è carry verso la colonna più a sinistra)
    val res: String,        // N+1 (può avere spazio in testa)
    val targets: List<AddTarget>,
    val a: Int,
    val b: Int,
    val result: Int
)

private fun pad(n: Int, len: Int) = n.toString().padStart(len, '0')
private fun padSpace(n: Int, len: Int) = n.toString().padStart(len, ' ')

private fun computeAdditionPlan(digits: Int, a: Int, b: Int): AddPlan {
    val result = a + b

    val aS = pad(a, digits)
    val bS = pad(b, digits)
    val resS = padSpace(result, digits + 1) // può essere 3 o 4 cifre ecc.

    // carry che entra in ogni colonna (da sinistra), ma in addizione utile è "carry verso colonna successiva a sinistra"
    // qui lo memorizzo per colonna (0..digits-1), dove carry[i] è il riporto scritto SOPRA la colonna i
    val carry = CharArray(digits) { ' ' }
    var c = 0
    for (i in digits - 1 downTo 0) {
        val da = aS[i] - '0'
        val db = bS[i] - '0'
        val s = da + db + c
        val newC = s / 10
        // il carry va nella colonna a sinistra (i-1), quindi lo scriviamo sopra i-1
        if (i - 1 >= 0 && newC > 0) carry[i - 1] = newC.toString()[0]
        c = newC
    }

    // targets step-by-step: unità->sinistra
    val targets = mutableListOf<AddTarget>()

    fun colLabel(i: Int, digits: Int): String {
        val posFromRight = (digits - 1) - i
        return when (posFromRight) {
            0 -> "unità"
            1 -> "decine"
            2 -> "centinaia"
            3 -> "migliaia"
            else -> "colonna ${posFromRight + 1}"
        }
    }

    var carryIn = 0
    for (i in digits - 1 downTo 0) {
        val da = aS[i] - '0'
        val db = bS[i] - '0'
        val s = da + db + carryIn
        val digitOut = (s % 10)
        val carryOut = (s / 10)

        // 1) scrivi cifra risultato in questa colonna (ma su resS è spostato di +1)
        val resIndex = i + 1
        targets += AddTarget(
            row = AddRowKey.SUM,
            col = resIndex, // colonna nel risultato (0..digits)
            kind = AddCellKind.DIGIT,
            expected = digitOut.toString()[0],
            hint = "${da} + ${db}${if (carryIn > 0) " + riporto $carryIn" else ""} = $s → scrivi $digitOut nelle ${colLabel(i, digits)}"
        )

        // 2) se c’è carryOut e non siamo alla colonna più a sinistra -> scrivi il riporto nella casellina sopra la colonna a sinistra
        if (carryOut > 0 && i - 1 >= 0) {
            targets += AddTarget(
                row = AddRowKey.CARRY,
                col = i - 1,
                kind = AddCellKind.CARRY,
                expected = carryOut.toString()[0],
                hint = "Riporta $carryOut nella ${colLabel(i - 1, digits)}"
            )
        }

        carryIn = carryOut
    }

    // se resta carry finale, va nella prima cifra del risultato (col 0 del risultato)
    val first = resS[0]
    if (first != ' ' && first != '0') {
        targets += AddTarget(
            row = AddRowKey.SUM,
            col = 0,
            kind = AddCellKind.DIGIT,
            expected = first,
            hint = "Ultimo riporto a sinistra: scrivi $first"
        )
    }

    return AddPlan(
        digits = digits,
        aStr = padSpace(a, digits),
        bStr = padSpace(b, digits),
        carry = carry,
        res = resS,
        targets = targets,
        a = a,
        b = b,
        result = result
    )
}

private fun computeAdditionPlan(digits: Int, rng: Random): AddPlan {
    val min = 10.0.pow((digits - 1).toDouble()).toInt()
    val max = 10.0.pow(digits.toDouble()).toInt() - 1
    val a = rng.nextInt(min, max + 1)
    val b = rng.nextInt(min, max + 1)
    return computeAdditionPlan(digits, a, b)
}

@Composable
fun LongAdditionGame(
    digits: Int,                   // 2 o 3
    startMode: StartMode = StartMode.RANDOM,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    val minValue = 10.0.pow((digits - 1).toDouble()).toInt()
    val maxValue = 10.0.pow(digits.toDouble()).toInt() - 1

    var manualA by remember { mutableStateOf("") }
    var manualB by remember { mutableStateOf("") }
    var manualNumbers by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var plan by remember(digits, startMode) {
        mutableStateOf(
            if (startMode == StartMode.RANDOM) computeAdditionPlan(digits, rng) else null
        )
    }

    var step by remember(plan) { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // input
    val planDigits = plan?.digits ?: digits
    val carryIn = remember(plan) { mutableStateOf(CharArray(planDigits) { '\u0000' }) }
    val sumIn = remember(plan) { mutableStateOf(CharArray(planDigits + 1) { '\u0000' }) }
    val errCarry = remember(plan) { mutableStateOf(BooleanArray(planDigits) { false }) }
    val errSum = remember(plan) { mutableStateOf(BooleanArray(planDigits + 1) { false }) }

    fun clearInputs() {
        val digitsCount = plan?.digits ?: digits
        carryIn.value = CharArray(digitsCount) { '\u0000' }
        sumIn.value = CharArray(digitsCount + 1) { '\u0000' }
        errCarry.value = BooleanArray(digitsCount) { false }
        errSum.value = BooleanArray(digitsCount + 1) { false }
    }

    fun resetSame() {
        step = 0
        showSuccessDialog = false
        clearInputs()
    }

    fun resetManualInputs() {
        manualA = ""
        manualB = ""
        manualNumbers = null
    }

    fun startManual(a: Int, b: Int) {
        manualNumbers = a to b
        plan = computeAdditionPlan(digits, a, b)
        resetSame()
    }

    fun resetNew() {
        if (startMode == StartMode.MANUAL) {
            resetManualInputs()
            plan = null
            resetSame()
        } else {
            plan = computeAdditionPlan(digits, rng)
            resetSame()
        }
    }

    val p = plan
    val current = p?.targets?.getOrNull(step)
    val done = p != null && step >= p.targets.size

    LaunchedEffect(done) {
        if (done && p != null) {
            showSuccessDialog = true
            correctCount += 1
        }
    }

    fun enabled(row: AddRowKey, col: Int, kind: AddCellKind): Boolean {
        val t = current ?: return false
        return t.row == row && t.col == col && t.kind == kind
    }

    fun isActive(row: AddRowKey, col: Int, kind: AddCellKind): Boolean = enabled(row, col, kind)

    fun setCell(row: AddRowKey, col: Int, ch: Char, isErr: Boolean) {
        when (row) {
            AddRowKey.CARRY -> {
                val a = carryIn.value.copyOf().also { it[col] = ch }
                val e = errCarry.value.copyOf().also { it[col] = isErr }
                carryIn.value = a; errCarry.value = e
            }
            AddRowKey.SUM -> {
                val a = sumIn.value.copyOf().also { it[col] = ch }
                val e = errSum.value.copyOf().also { it[col] = isErr }
                sumIn.value = a; errSum.value = e
            }
            else -> Unit
        }
    }

    fun onTyped(row: AddRowKey, col: Int, kind: AddCellKind, text: String) {
        val t = current ?: return
        if (t.row != row || t.col != col || t.kind != kind) return

        val digit = text.filter { it.isDigit() }.takeLast(1).firstOrNull()
        if (digit == null) {
            setCell(row, col, '\u0000', false)
            return
        }
        val ok = digit == t.expected
        setCell(row, col, digit, !ok)
        if (ok) {
            val activePlan = plan ?: return
            step = (step + 1).coerceAtMost(activePlan.targets.size)
        }
    }

    val hint = when {
        p == null -> "Inserisci i numeri e premi Avvia."
        done -> "Bravo! ✅ Risultato: ${p.result}"
        else -> current?.hint.orEmpty()
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val digitW = if (ui.isCompact) 34.dp else 44.dp
        val digitH = if (ui.isCompact) 48.dp else 56.dp
        val carryW = if (ui.isCompact) 20.dp else 24.dp
        val carryH = if (ui.isCompact) 26.dp else 30.dp
        val signW = if (ui.isCompact) 20.dp else 26.dp
        val gap = if (ui.isCompact) 4.dp else 6.dp
        val carryFont = if (ui.isCompact) 14.sp else 16.sp
        val sumFont = if (ui.isCompact) 18.sp else 22.sp

        GameScreenFrame(
            title = "Addizioni in colonna",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            bonusTarget = BONUS_TARGET_LONG_ADD_SUB,
            hintText = hint,
            ui = ui,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                if (startMode == StartMode.MANUAL) {
                    val manualAValue = manualA.toIntOrNull()
                    val manualBValue = manualB.toIntOrNull()
                    val manualValid = manualAValue in minValue..maxValue && manualBValue in minValue..maxValue
                    val manualError = if (manualValid || (manualA.isBlank() && manualB.isBlank())) {
                        null
                    } else {
                        "Inserisci due numeri da $minValue a $maxValue."
                    }

                    SeaGlassPanel(title = "Inserimento") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualA,
                                onValueChange = { manualA = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Numero A") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = manualB,
                                onValueChange = { manualB = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Numero B") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val aVal = manualAValue ?: return@Button
                                    val bVal = manualBValue ?: return@Button
                                    startManual(aVal, bVal)
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

                SeaGlassPanel(title = "Esercizio") {
                    if (p == null) {
                        Text("Inserisci i numeri e premi Avvia.")
                    } else {
                        val activePlan = p
                        Text(
                            "Esercizio: ${activePlan.a} + ${activePlan.b}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(if (ui.isCompact) 6.dp else 8.dp))

                        val totalCols = activePlan.digits + 1

                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Riga carry (caselline piccole) allineate alle cifre A/B
                            GridRowRight(signW, gap) {
                                for (displayCol in 0 until totalCols) {
                                    if (displayCol == 0) {
                                        Box(Modifier.width(digitW).height(carryH))
                                    } else {
                                        val col = displayCol - 1
                                        val expected = activePlan.carry[col]
                                        Box(modifier = Modifier.width(digitW), contentAlignment = Alignment.Center) {
                                            if (expected == ' ') {
                                                Box(Modifier.width(carryW).height(carryH))
                                            } else {
                                                val txt = carryIn.value[col].let { if (it == '\u0000') "" else it.toString() }
                                                InputBox(
                                                    value = txt,
                                                    enabled = enabled(AddRowKey.CARRY, col, AddCellKind.CARRY),
                                                    isActive = isActive(AddRowKey.CARRY, col, AddCellKind.CARRY),
                                                    isError = errCarry.value[col],
                                                    w = carryW,
                                                    h = carryH,
                                                    fontSize = carryFont,
                                                    onValueChange = { onTyped(AddRowKey.CARRY, col, AddCellKind.CARRY, it) }
                                                )
                                            }
                                        }
                                    }
                                }
                                SignCell("", signW)
                            }

                            // Riga A
                            GridRowRight(signW, gap) {
                                for (displayCol in 0 until totalCols) {
                                    val ch = if (displayCol == 0) ' ' else activePlan.aStr[displayCol - 1]
                                    FixedDigit(ch, digitW, digitH)
                                }
                                SignCell("", signW)
                            }

                            // Riga B con segno +
                            GridRowRight(signW, gap) {
                                for (displayCol in 0 until totalCols) {
                                    val ch = if (displayCol == 0) ' ' else activePlan.bStr[displayCol - 1]
                                    FixedDigit(ch, digitW, digitH)
                                }
                                SignCell("+", signW)
                            }

                            Divider(thickness = if (ui.isCompact) 1.dp else 2.dp)

                            // Risultato (digits+1)
                            GridRowRight(signW, gap) {
                                for (col in 0 until totalCols) {
                                    val exp = activePlan.res[col]
                                    if (exp == ' ') {
                                        FixedDigit(' ', digitW, digitH)
                                    } else {
                                        val txt = sumIn.value[col].let { if (it == '\u0000') "" else it.toString() }
                                        InputBox(
                                            value = txt,
                                            enabled = enabled(AddRowKey.SUM, col, AddCellKind.DIGIT),
                                            isActive = isActive(AddRowKey.SUM, col, AddCellKind.DIGIT),
                                            isError = errSum.value[col],
                                            w = digitW,
                                            h = digitH,
                                            fontSize = sumFont,
                                            onValueChange = { onTyped(AddRowKey.SUM, col, AddCellKind.DIGIT, it) }
                                        )
                                    }
                                }
                                SignCell("", signW)
                            }
                        }
                    }
                }

                SeaGlassPanel(title = "Aiuto") {
                    val totalSteps = plan?.targets?.size ?: 0
                    val stepLabel = when {
                        totalSteps == 0 -> "Passo 0/0"
                        done -> "Passo $totalSteps/$totalSteps"
                        else -> "Passo ${step + 1}/$totalSteps"
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
                            if (manual != null && p != null) {
                                startManual(manual.first, manual.second)
                            } else {
                                resetManualInputs()
                                plan = null
                                resetSame()
                            }
                        } else {
                            resetSame()
                        }
                    },
                    rightText = "Nuovo",
                    onRight = { resetNew() }
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
            resultText = plan?.result?.toString().orEmpty()
        )

        BonusRewardHost(
            correctCount = correctCount,
            rewardsEarned = rewardsEarned,
            rewardEvery = BONUS_TARGET_LONG_ADD_SUB,
            soundEnabled = soundEnabled,
            fx = fx,
            onOpenLeaderboard = onOpenLeaderboardFromBonus,
            onRewardEarned = { rewardsEarned += 1 },
            onRewardSkipped = { rewardsEarned += 1 }
        )
    }
}
