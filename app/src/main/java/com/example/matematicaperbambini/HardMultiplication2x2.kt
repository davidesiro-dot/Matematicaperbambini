package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/* -------------------------------------------------------------------------
   MOLTIPLICAZIONE DIFFICILE 2x2 GUIDATA
   ------------------------------------------------------------------------- */

private enum class HMRowKey { CARRY_P1, P1, CARRY_P2, P2, CARRY_SUM, SUM }
private enum class HMCellKind { CARRY, DIGIT }
private enum class HMHighlightRow { TOP_A, TOP_B, CARRY_P1, P1, CARRY_P2, P2, CARRY_SUM, SUM }

private data class HMHighlight(
    val row: HMHighlightRow,
    val col: Int
)

private data class HMTarget(
    val row: HMRowKey,
    val col: Int, // 0..3 -> migliaia..unità
    val kind: HMCellKind,
    val expected: Char,
    val hint: String,
    val highlights: List<HMHighlight>
)

private data class HMPlan(
    val a: Int,
    val b: Int,
    val a4: String,
    val b4: String,
    val p1_4: String,
    val p2_4: String,
    val res_4: String,
    val carryP1: CharArray,
    val carryP2: CharArray,
    val carrySUM: CharArray,
    val targets: List<HMTarget>,
    val result: Int
)

private fun hmPad4(n: Int) = n.toString().padStart(4, ' ')
private fun hmPad4From2(n2: Int) = n2.toString().padStart(2, ' ').padStart(4, ' ')

private fun hmColLabel(col: Int): String = when (col) {
    3 -> "Unità"
    2 -> "Decine"
    1 -> "Centinaia"
    0 -> "Migliaia"
    else -> "?"
}

private fun hmRowLabel(row: HMRowKey): String = when (row) {
    HMRowKey.CARRY_P1 -> "Riporto riga 1"
    HMRowKey.P1 -> "Riga 1 (× unità)"
    HMRowKey.CARRY_P2 -> "Riporto riga 2"
    HMRowKey.P2 -> "Riga 2 (× decine)"
    HMRowKey.CARRY_SUM -> "Riporto somma"
    HMRowKey.SUM -> "Risultato"
}

private fun hmComputePlan(a: Int, b: Int): HMPlan {
    require(a in 10..99 && b in 10..99)

    val aT = a / 10
    val aU = a % 10
    val bT = b / 10
    val bU = b % 10

    // Riga 1: a × unità
    val m1 = aU * bU
    val w11 = m1 % 10
    val c11 = m1 / 10

    val m2raw = aT * bU
    val m2 = m2raw + c11
    val w12 = m2 % 10
    val c12 = m2 / 10

    val p1 = (c12 * 100) + (w12 * 10) + w11

    // Riga 2: a × decine poi shift ×10
    val n1 = aU * bT
    val w21 = n1 % 10
    val c21 = n1 / 10

    val n2raw = aT * bT
    val n2 = n2raw + c21
    val w22 = n2 % 10
    val c22 = n2 / 10

    val p2NoShift = (c22 * 100) + (w22 * 10) + w21
    val p2 = p2NoShift * 10

    val res = p1 + p2

    val a4 = hmPad4From2(a)
    val b4 = hmPad4From2(b)
    val p1_4 = hmPad4(p1)
    val p2_4 = hmPad4(p2)
    val res_4 = hmPad4(res)

    // riporti (vedi tue note)
    val carryP1 = CharArray(4) { ' ' }
    if (c11 > 0) carryP1[2] = c11.toString()[0]

    val carryP2 = CharArray(4) { ' ' }
    if (c21 > 0) carryP2[1] = c21.toString()[0]

    val p1d = p1_4.map { if (it == ' ') 0 else it - '0' }
    val p2d = p2_4.map { if (it == ' ') 0 else it - '0' }
    val carrySUM = CharArray(4) { ' ' }

    var carry = 0
    run {
        val s = p1d[3] + p2d[3] + carry
        carry = s / 10
        if (carry > 0) carrySUM[2] = carry.toString()[0]
    }
    run {
        val s = p1d[2] + p2d[2] + carry
        carry = s / 10
        if (carry > 0) carrySUM[1] = carry.toString()[0]
    }

    val targets = mutableListOf<HMTarget>()

    fun addHighlight(
        list: MutableList<HMHighlight>,
        row: HMHighlightRow,
        col: Int,
        ch: Char? = null
    ) {
        if (col !in 0..3) return
        if (ch != null && ch == ' ') return
        list += HMHighlight(row, col)
    }

    fun addDigit(row: HMRowKey, col: Int, ch: Char, hint: String, highlights: List<HMHighlight>) {
        targets += HMTarget(row, col, HMCellKind.DIGIT, ch, hint, highlights)
    }

    fun addCarry(row: HMRowKey, col: Int, ch: Char, hint: String, highlights: List<HMHighlight>) {
        targets += HMTarget(row, col, HMCellKind.CARRY, ch, hint, highlights)
    }

    fun highlightsForMul(
        aCol: Int,
        bCol: Int,
        targetRow: HMHighlightRow,
        targetCol: Int,
        extra: List<HMHighlight> = emptyList()
    ): List<HMHighlight> {
        val list = mutableListOf<HMHighlight>()
        addHighlight(list, HMHighlightRow.TOP_A, aCol, a4[aCol])
        addHighlight(list, HMHighlightRow.TOP_B, bCol, b4[bCol])
        addHighlight(list, targetRow, targetCol)
        list.addAll(extra)
        return list.distinct()
    }

    fun highlightsForSum(
        col: Int,
        targetRow: HMHighlightRow,
        targetCol: Int,
        carryCol: Int? = null
    ): List<HMHighlight> {
        val list = mutableListOf<HMHighlight>()
        addHighlight(list, HMHighlightRow.P1, col, p1_4[col])
        addHighlight(list, HMHighlightRow.P2, col, p2_4[col])
        addHighlight(list, targetRow, targetCol)
        if (carryCol != null) {
            addHighlight(list, HMHighlightRow.CARRY_SUM, carryCol, carrySUM[carryCol])
        }
        return list.distinct()
    }

    // P1
    addDigit(
        HMRowKey.P1,
        3,
        w11.toString()[0],
        "${aU}×${bU} = $m1 → scrivi $w11 nelle unità",
        highlightsForMul(3, 3, HMHighlightRow.P1, 3)
    )
    if (c11 > 0) {
        addCarry(
            HMRowKey.CARRY_P1,
            2,
            c11.toString()[0],
            "${aU}×${bU} = $m1 → riporta $c11 nelle decine",
            highlightsForMul(3, 3, HMHighlightRow.CARRY_P1, 2)
        )
    }
    val p1TensHighlights = highlightsForMul(
        2,
        3,
        HMHighlightRow.P1,
        2,
        extra = buildList {
            if (carryP1[2] != ' ') add(HMHighlight(HMHighlightRow.CARRY_P1, 2))
        }
    )
    addDigit(
        HMRowKey.P1,
        2,
        w12.toString()[0],
        "${aT}×${bU} = $m2raw${if (c11 > 0) " + $c11 = $m2" else ""} → scrivi $w12 nelle decine",
        p1TensHighlights
    )
    if (c12 > 0) {
        addDigit(
            HMRowKey.P1,
            1,
            c12.toString()[0],
            "Ultimo riporto riga 1: scrivi $c12 nella casella delle centinaia",
            highlightsForMul(2, 3, HMHighlightRow.P1, 1)
        )
    }

    // P2
    addDigit(
        HMRowKey.P2,
        2,
        w21.toString()[0],
        "Riga decine: ${aU}×${bT} = $n1 → scrivi $w21 (unità è un trattino)",
        highlightsForMul(3, 2, HMHighlightRow.P2, 2)
    )
    if (c21 > 0) {
        addCarry(
            HMRowKey.CARRY_P2,
            1,
            c21.toString()[0],
            "Riporta $c21 nelle centinaia (riga 2)",
            highlightsForMul(3, 2, HMHighlightRow.CARRY_P2, 1)
        )
    }
    val p2HundredsHighlights = highlightsForMul(
        2,
        2,
        HMHighlightRow.P2,
        1,
        extra = buildList {
            if (carryP2[1] != ' ') add(HMHighlight(HMHighlightRow.CARRY_P2, 1))
        }
    )
    addDigit(
        HMRowKey.P2,
        1,
        w22.toString()[0],
        "${aT}×${bT} = $n2raw${if (c21 > 0) " + $c21 = $n2" else ""} → scrivi $w22 nelle centinaia",
        p2HundredsHighlights
    )
    if (c22 > 0) {
        addDigit(
            HMRowKey.P2,
            0,
            c22.toString()[0],
            "Ultimo riporto riga 2: scrivi $c22 nella casella delle migliaia",
            highlightsForMul(2, 2, HMHighlightRow.P2, 0)
        )
    }

    // SOMMA
    fun addSum(col: Int, carryIn: Int, carryOut: Int, digit: Int) {
        val carryHighlightCol = if (carryIn > 0 && carrySUM[col] != ' ') col else null
        addDigit(
            HMRowKey.SUM,
            col,
            digit.toString()[0],
            "Somma ${hmColLabel(col)}: ${p1d[col]} + ${p2d[col]}${if (carryIn > 0) " + riporto $carryIn" else ""} → scrivi $digit",
            highlightsForSum(col, HMHighlightRow.SUM, col, carryHighlightCol)
        )
        if (carryOut > 0 && col - 1 >= 0 && (col - 1) != 0) {
            addCarry(
                HMRowKey.CARRY_SUM,
                col - 1,
                carryOut.toString()[0],
                "Riporta $carryOut nella ${hmColLabel(col - 1)}",
                highlightsForSum(col, HMHighlightRow.CARRY_SUM, col - 1)
            )
        }
    }

    var cin = 0
    run { val s = p1d[3] + p2d[3] + cin; addSum(3, cin, s / 10, s % 10); cin = s / 10 }
    run { val s = p1d[2] + p2d[2] + cin; addSum(2, cin, s / 10, s % 10); cin = s / 10 }
    run { val s = p1d[1] + p2d[1] + cin; addSum(1, cin, s / 10, s % 10); cin = s / 10 }
    run {
        val s = p1d[0] + p2d[0] + cin
        addDigit(
            HMRowKey.SUM,
            0,
            (s % 10).toString()[0],
            "Ultima colonna a sinistra: ${p1d[0]} + ${p2d[0]}${if (cin > 0) " + riporto $cin" else ""} → scrivi ${s % 10}",
            highlightsForSum(0, HMHighlightRow.SUM, 0)
        )
    }

    return HMPlan(
        a = a,
        b = b,
        a4 = a4,
        b4 = b4,
        p1_4 = p1_4,
        p2_4 = p2_4,
        res_4 = res_4,
        carryP1 = carryP1,
        carryP2 = carryP2,
        carrySUM = carrySUM,
        targets = targets,
        result = res
    )
}

/* ----------------------------- GAME ----------------------------- */

@Composable
fun HardMultiplication2x2Game(
    startMode: StartMode = StartMode.RANDOM,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    var plan by remember(startMode) {
        mutableStateOf(
            if (startMode == StartMode.RANDOM) hmComputePlan(47, 36) else null
        )
    }
    var manualA by remember { mutableStateOf("") }
    var manualB by remember { mutableStateOf("") }
    var manualNumbers by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var step by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var noHintsMode by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var solutionUsed by remember { mutableStateOf(false) }

    var inCarryP1 by remember { mutableStateOf(CharArray(4) { '\u0000' }) }
    var inP1 by remember { mutableStateOf(CharArray(4) { '\u0000' }) }
    var inCarryP2 by remember { mutableStateOf(CharArray(4) { '\u0000' }) }
    var inP2 by remember { mutableStateOf(CharArray(4) { '\u0000' }) }
    var inCarrySUM by remember { mutableStateOf(CharArray(4) { '\u0000' }) }
    var inSUM by remember { mutableStateOf(CharArray(4) { '\u0000' }) }

    var errCarryP1 by remember { mutableStateOf(BooleanArray(4) { false }) }
    var errP1 by remember { mutableStateOf(BooleanArray(4) { false }) }
    var errCarryP2 by remember { mutableStateOf(BooleanArray(4) { false }) }
    var errP2 by remember { mutableStateOf(BooleanArray(4) { false }) }
    var errCarrySUM by remember { mutableStateOf(BooleanArray(4) { false }) }
    var errSUM by remember { mutableStateOf(BooleanArray(4) { false }) }

    fun reset(newA: Int, newB: Int) {
        plan = hmComputePlan(newA, newB)
        step = 0
        showSuccessDialog = false
        solutionUsed = false
        inCarryP1 = CharArray(4) { '\u0000' }
        inP1 = CharArray(4) { '\u0000' }
        inCarryP2 = CharArray(4) { '\u0000' }
        inP2 = CharArray(4) { '\u0000' }.also { it[3] = '-' }
        inCarrySUM = CharArray(4) { '\u0000' }
        inSUM = CharArray(4) { '\u0000' }

        errCarryP1 = BooleanArray(4) { false }
        errP1 = BooleanArray(4) { false }
        errCarryP2 = BooleanArray(4) { false }
        errP2 = BooleanArray(4) { false }
        errCarrySUM = BooleanArray(4) { false }
        errSUM = BooleanArray(4) { false }
    }

    fun clearInputsOnly() {
        step = 0
        showSuccessDialog = false
        solutionUsed = false
        inCarryP1 = CharArray(4) { '\u0000' }
        inP1 = CharArray(4) { '\u0000' }
        inCarryP2 = CharArray(4) { '\u0000' }
        inP2 = CharArray(4) { '\u0000' }.also { it[3] = '-' }
        inCarrySUM = CharArray(4) { '\u0000' }
        inSUM = CharArray(4) { '\u0000' }

        errCarryP1 = BooleanArray(4) { false }
        errP1 = BooleanArray(4) { false }
        errCarryP2 = BooleanArray(4) { false }
        errP2 = BooleanArray(4) { false }
        errCarrySUM = BooleanArray(4) { false }
        errSUM = BooleanArray(4) { false }
    }

    fun resetManualInputs() {
        manualA = ""
        manualB = ""
        manualNumbers = null
        solutionUsed = false
    }

    fun startManual(a: Int, b: Int) {
        manualNumbers = a to b
        reset(a, b)
    }

    LaunchedEffect(Unit) {
        val p2 = inP2.copyOf()
        p2[3] = '-'
        inP2 = p2
    }

    val p = plan
    val current = p?.targets?.getOrNull(step)
    val done = p != null && step >= p.targets.size

    LaunchedEffect(done) {
        if (done && p != null) {
            if (!solutionUsed) {
                showSuccessDialog = true
                correctCount += 1
            }
        }
    }

    fun enabled(row: HMRowKey, col: Int, kind: HMCellKind): Boolean {
        val t = current ?: return false
        if (row == HMRowKey.P2 && col == 3) return false
        return t.row == row && t.col == col && t.kind == kind
    }

    fun isActive(row: HMRowKey, col: Int, kind: HMCellKind): Boolean {
        val t = current ?: return false
        return t.row == row && t.col == col && t.kind == kind
    }

    fun stageIndex(r: HMRowKey): Int = when (r) {
        HMRowKey.CARRY_P1 -> 0
        HMRowKey.P1 -> 1
        HMRowKey.CARRY_P2 -> 2
        HMRowKey.P2 -> 3
        HMRowKey.CARRY_SUM -> 4
        HMRowKey.SUM -> 5
    }

    fun carryShouldFade(carryRow: HMRowKey, carryCol: Int): Boolean {
        val t = current ?: return false
        val relatedDigitRow = when (carryRow) {
            HMRowKey.CARRY_P1 -> HMRowKey.P1
            HMRowKey.CARRY_P2 -> HMRowKey.P2
            HMRowKey.CARRY_SUM -> HMRowKey.SUM
            else -> return false
        }
        val curStage = stageIndex(t.row)
        val relatedStage = stageIndex(relatedDigitRow)

        if (curStage > relatedStage) return true
        if (t.row == relatedDigitRow) return t.col < carryCol
        if (t.row == carryRow) return false
        return false
    }

    fun setCell(row: HMRowKey, col: Int, ch: Char, isErr: Boolean) {
        fun setArr(arr: CharArray) = arr.copyOf().also { it[col] = ch }
        fun setErr(arr: BooleanArray) = arr.copyOf().also { it[col] = isErr }

        when (row) {
            HMRowKey.CARRY_P1 -> { inCarryP1 = setArr(inCarryP1); errCarryP1 = setErr(errCarryP1) }
            HMRowKey.P1 -> { inP1 = setArr(inP1); errP1 = setErr(errP1) }
            HMRowKey.CARRY_P2 -> { inCarryP2 = setArr(inCarryP2); errCarryP2 = setErr(errCarryP2) }
            HMRowKey.P2 -> { inP2 = setArr(inP2); errP2 = setErr(errP2) }
            HMRowKey.CARRY_SUM -> { inCarrySUM = setArr(inCarrySUM); errCarrySUM = setErr(errCarrySUM) }
            HMRowKey.SUM -> { inSUM = setArr(inSUM); errSUM = setErr(errSUM) }
        }
    }

    fun onTyped(row: HMRowKey, col: Int, kind: HMCellKind, text: String) {
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
            if (soundEnabled) fx.correct()
            val activePlan = plan ?: return
            step = (step + 1).coerceAtMost(activePlan.targets.size)
        } else {
            if (soundEnabled) fx.wrong()
        }
    }

    val hint = when {
        plan == null -> "Inserisci i numeri e premi Avvia."
        done && p != null -> {
            if (solutionUsed) {
                "Risultato: ${p.result}"
            } else {
                "Bravo! ✅ Risultato: ${p.result}"
            }
        }
        else -> current?.hint.orEmpty()
    }

    fun isHL(row: HMHighlightRow, col: Int): Boolean =
        !noHintsMode && current?.highlights?.contains(HMHighlight(row, col)) == true

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val digitW = if (ui.isCompact) 34.dp else 44.dp
        val digitH = if (ui.isCompact) 48.dp else 56.dp
        val carryW = if (ui.isCompact) 20.dp else 24.dp
        val carryH = if (ui.isCompact) 26.dp else 30.dp
        val signW = if (ui.isCompact) 20.dp else 26.dp
        val gap = if (ui.isCompact) 4.dp else 6.dp
        val carryFont = if (ui.isCompact) 14.sp else 16.sp
        val digitFont = if (ui.isCompact) 18.sp else 22.sp

        GameScreenFrame(
            title = "Moltiplicazioni difficili",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            bonusTarget = BONUS_TARGET_LONG_MULT_DIV,
            hintText = hint,
            noHintsMode = noHintsMode,
            onToggleHints = { noHintsMode = !noHintsMode },
            ui = ui,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                if (startMode == StartMode.MANUAL) {
                    val manualAValue = manualA.toIntOrNull()
                    val manualBValue = manualB.toIntOrNull()
                    val manualValid = manualAValue in 10..99 && manualBValue in 10..99
                    val manualError = if (manualValid || (manualA.isBlank() && manualB.isBlank())) {
                        null
                    } else {
                        "Inserisci due numeri da 10 a 99."
                    }

                    SeaGlassPanel(title = "Inserimento") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualA,
                                onValueChange = { manualA = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Numero A") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = manualB,
                                onValueChange = { manualB = it.filter { c -> c.isDigit() }.take(2) },
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
                            "Esercizio: ${activePlan.a} × ${activePlan.b}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(if (ui.isCompact) 6.dp else 8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HMGridRowRight(signW, gap) {
                                HMFixedDigit(
                                    activePlan.a4[0],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_A, 0)
                                )
                                HMFixedDigit(
                                    activePlan.a4[1],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_A, 1)
                                )
                                HMFixedDigit(
                                    activePlan.a4[2],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_A, 2)
                                )
                                HMFixedDigit(
                                    activePlan.a4[3],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_A, 3)
                                )
                                HMSignCell("", signW)
                            }

                            HMGridRowRight(signW, gap) {
                                HMFixedDigit(
                                    activePlan.b4[0],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_B, 0)
                                )
                                HMFixedDigit(
                                    activePlan.b4[1],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_B, 1)
                                )
                                HMFixedDigit(
                                    activePlan.b4[2],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_B, 2)
                                )
                                HMFixedDigit(
                                    activePlan.b4[3],
                                    digitW,
                                    digitH,
                                    digitFont,
                                    highlight = isHL(HMHighlightRow.TOP_B, 3)
                                )
                                HMSignCell("×", signW)
                            }

                            Divider(thickness = if (ui.isCompact) 1.dp else 2.dp)

                            HMCarryRowRight(
                                signW, gap, digitW, carryW, carryH,
                                expected = activePlan.carryP1,
                                input = inCarryP1,
                                err = errCarryP1,
                                enabled = { c -> enabled(HMRowKey.CARRY_P1, c, HMCellKind.CARRY) },
                                isActive = { c -> isActive(HMRowKey.CARRY_P1, c, HMCellKind.CARRY) },
                                highlight = { c -> isHL(HMHighlightRow.CARRY_P1, c) },
                                shouldFade = { c -> carryShouldFade(HMRowKey.CARRY_P1, c) },
                                onChange = { c, v -> onTyped(HMRowKey.CARRY_P1, c, HMCellKind.CARRY, v) },
                                fontSize = carryFont
                            )

                            HMDigitRowRight(
                                signW, gap, digitW, digitH,
                                expected = activePlan.p1_4,
                                input = inP1,
                                err = errP1,
                                enabled = { c -> enabled(HMRowKey.P1, c, HMCellKind.DIGIT) },
                                isActive = { c -> isActive(HMRowKey.P1, c, HMCellKind.DIGIT) },
                                highlight = { c -> isHL(HMHighlightRow.P1, c) },
                                onChange = { c, v -> onTyped(HMRowKey.P1, c, HMCellKind.DIGIT, v) },
                                fontSize = digitFont
                            )

                            HMCarryRowRight(
                                signW, gap, digitW, carryW, carryH,
                                expected = activePlan.carryP2,
                                input = inCarryP2,
                                err = errCarryP2,
                                enabled = { c -> enabled(HMRowKey.CARRY_P2, c, HMCellKind.CARRY) },
                                isActive = { c -> isActive(HMRowKey.CARRY_P2, c, HMCellKind.CARRY) },
                                highlight = { c -> isHL(HMHighlightRow.CARRY_P2, c) },
                                shouldFade = { c -> carryShouldFade(HMRowKey.CARRY_P2, c) },
                                onChange = { c, v -> onTyped(HMRowKey.CARRY_P2, c, HMCellKind.CARRY, v) },
                                fontSize = carryFont
                            )

                            HMDigitRowRight(
                                signW, gap, digitW, digitH,
                                expected = activePlan.p2_4,
                                input = inP2,
                                err = errP2,
                                enabled = { c -> enabled(HMRowKey.P2, c, HMCellKind.DIGIT) },
                                isActive = { c -> isActive(HMRowKey.P2, c, HMCellKind.DIGIT) },
                                highlight = { c -> isHL(HMHighlightRow.P2, c) },
                                onChange = { c, v -> onTyped(HMRowKey.P2, c, HMCellKind.DIGIT, v) },
                                fixedUnitDash = true,
                                fontSize = digitFont
                            )

                            Divider(thickness = if (ui.isCompact) 1.dp else 2.dp)

                            HMCarryRowRight(
                                signW, gap, digitW, carryW, carryH,
                                expected = activePlan.carrySUM,
                                input = inCarrySUM,
                                err = errCarrySUM,
                                enabled = { c -> enabled(HMRowKey.CARRY_SUM, c, HMCellKind.CARRY) },
                                isActive = { c -> isActive(HMRowKey.CARRY_SUM, c, HMCellKind.CARRY) },
                                highlight = { c -> isHL(HMHighlightRow.CARRY_SUM, c) },
                                shouldFade = { c -> carryShouldFade(HMRowKey.CARRY_SUM, c) },
                                onChange = { c, v -> onTyped(HMRowKey.CARRY_SUM, c, HMCellKind.CARRY, v) },
                                fontSize = carryFont
                            )

                            HMDigitRowRight(
                                signW, gap, digitW, digitH,
                                expected = activePlan.res_4,
                                input = inSUM,
                                err = errSUM,
                                enabled = { c -> enabled(HMRowKey.SUM, c, HMCellKind.DIGIT) },
                                isActive = { c -> isActive(HMRowKey.SUM, c, HMCellKind.DIGIT) },
                                highlight = { c -> isHL(HMHighlightRow.SUM, c) },
                                onChange = { c, v -> onTyped(HMRowKey.SUM, c, HMCellKind.DIGIT, v) },
                                fontSize = digitFont
                            )
                        }
                    }
                }

                SeaGlassPanel(title = "Aiuto") {
                    val totalSteps = p?.targets?.size ?: 0
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
                                reset(manual.first, manual.second)
                            } else {
                                resetManualInputs()
                                plan = null
                                clearInputsOnly()
                            }
                        } else if (p != null) {
                            reset(p.a, p.b)
                        }
                    },
                    rightText = "Nuovo",
                    onRight = {
                        if (startMode == StartMode.MANUAL) {
                            resetManualInputs()
                            plan = null
                            clearInputsOnly()
                        } else {
                            reset(Random.nextInt(10, 100), Random.nextInt(10, 100))
                        }
                    },
                    center = {
                        OutlinedButton(
                            onClick = {
                                val activePlan = plan ?: return@OutlinedButton
                                solutionUsed = true
                                for (c in 0..3) {
                                    val cp1 = activePlan.carryP1[c]; if (cp1 != ' ') setCell(HMRowKey.CARRY_P1, c, cp1, false)
                                    val cp2 = activePlan.carryP2[c]; if (cp2 != ' ') setCell(HMRowKey.CARRY_P2, c, cp2, false)
                                    val cs = activePlan.carrySUM[c]; if (cs != ' ') setCell(HMRowKey.CARRY_SUM, c, cs, false)

                                    val d1 = activePlan.p1_4[c]; if (d1 != ' ') setCell(HMRowKey.P1, c, d1, false)
                                    val d2 = activePlan.p2_4[c]; if (d2 != ' ') setCell(HMRowKey.P2, c, d2, false)
                                    val dr = activePlan.res_4[c]; if (dr != ' ') setCell(HMRowKey.SUM, c, dr, false)
                                }
                                setCell(HMRowKey.P2, 3, '-', false)
                                step = activePlan.targets.size
                            }
                        ) {
                            Text(
                                "Soluzione",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                fontSize = 12.sp
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
                if (startMode == StartMode.MANUAL) {
                    resetManualInputs()
                    plan = null
                } else {
                    reset(Random.nextInt(10, 100), Random.nextInt(10, 100))
                }
            },
            onDismiss = { showSuccessDialog = false },
            resultText = plan?.result?.toString().orEmpty()
        )

        BonusRewardHost(
            correctCount = correctCount,
            rewardsEarned = rewardsEarned,
            rewardEvery = BONUS_TARGET_LONG_MULT_DIV,
            soundEnabled = soundEnabled,
            fx = fx,
            onOpenLeaderboard = onOpenLeaderboardFromBonus,
            onRewardEarned = { rewardsEarned += 1 },
            onRewardSkipped = { rewardsEarned += 1 }
        )
    }
}

/* ----------------------------- GRID UI ----------------------------- */

@Composable
private fun HMGridRowRight(
    signW: Dp,
    gap: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun HMSignCell(text: String, w: Dp) {
    Box(modifier = Modifier.width(w), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HMFixedDigit(
    ch: Char,
    w: Dp,
    h: Dp,
    fontSize: TextUnit,
    highlight: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    val highlightColor = Color(0xFF22C55E)
    val borderColor = if (highlight) highlightColor else MaterialTheme.colorScheme.outlineVariant
    val borderW = if (highlight) 3.dp else 1.dp
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderW, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        val color = if (ch == '-') MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        Text(
            if (ch == ' ') "" else ch.toString(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HMCarryRowRight(
    signW: Dp,
    gap: Dp,
    digitW: Dp,
    carryW: Dp,
    carryH: Dp,
    expected: CharArray,
    input: CharArray,
    err: BooleanArray,
    enabled: (Int) -> Boolean,
    isActive: (Int) -> Boolean,
    highlight: (Int) -> Boolean,
    shouldFade: (Int) -> Boolean,
    onChange: (Int, String) -> Unit,
    fontSize: TextUnit
) {
    HMGridRowRight(signW, gap) {
        for (col in 0..3) {
            val exp = expected[col]
            val fade = (exp != ' ') && shouldFade(col) && !enabled(col) && !isActive(col)
            val a = if (fade) 0.35f else 1f

            Box(
                modifier = Modifier.width(digitW).alpha(a),
                contentAlignment = Alignment.Center
            ) {
                if (exp == ' ') {
                    Box(Modifier.width(carryW).height(carryH))
                } else {
                    val txt = if (input[col] == '\u0000') "" else input[col].toString()
                    HMInputBox(
                        value = txt,
                        enabled = enabled(col),
                        isActive = isActive(col),
                        isError = err[col],
                        highlight = highlight(col),
                        w = carryW,
                        h = carryH,
                        fontSize = fontSize,
                        onValueChange = { onChange(col, it) }
                    )
                }
            }
        }
        HMSignCell("", signW)
    }
}

@Composable
private fun HMDigitRowRight(
    signW: Dp,
    gap: Dp,
    digitW: Dp,
    digitH: Dp,
    expected: String,
    input: CharArray,
    err: BooleanArray,
    enabled: (Int) -> Boolean,
    isActive: (Int) -> Boolean,
    highlight: (Int) -> Boolean,
    onChange: (Int, String) -> Unit,
    fixedUnitDash: Boolean = false,
    fontSize: TextUnit
) {
    HMGridRowRight(signW, gap) {
        for (col in 0..3) {
            val exp = expected[col]

            if (fixedUnitDash && col == 3) {
                HMFixedDigit('-', digitW, digitH, fontSize)
                continue
            }

            if (exp == ' ') {
                HMFixedDigit(' ', digitW, digitH, fontSize)
            } else {
                val txt = if (input[col] == '\u0000') "" else input[col].toString()
                HMInputBox(
                    value = txt,
                    enabled = enabled(col),
                    isActive = isActive(col),
                    isError = err[col],
                    highlight = highlight(col),
                    w = digitW,
                    h = digitH,
                    fontSize = fontSize,
                    onValueChange = { onChange(col, it) }
                )
            }
        }
        HMSignCell("", signW)
    }
}

@Composable
private fun HMInputBox(
    value: String,
    enabled: Boolean,
    isActive: Boolean,
    isError: Boolean,
    highlight: Boolean,
    w: Dp,
    h: Dp,
    fontSize: TextUnit,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val highlightColor = Color(0xFF22C55E)

    val bg = when {
        isActive -> MaterialTheme.colorScheme.tertiaryContainer
        value.isNotBlank() && !isError -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        highlight -> highlightColor
        isActive -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val borderW = when {
        isError -> 2.dp
        highlight || isActive -> 3.dp
        else -> 2.dp
    }

    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(borderW, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val cleaned = it.filter { ch -> ch.isDigit() }.takeLast(1)
                onValueChange(cleaned)
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            }
        )
    }
}
