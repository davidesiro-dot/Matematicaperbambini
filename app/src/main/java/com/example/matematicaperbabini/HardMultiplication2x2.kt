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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/* -------------------------------------------------------------------------
   MOLTIPLICAZIONE DIFFICILE 2x2 GUIDATA (come il tuo codice originale)
   - UNA sola casella attiva
   - riporto gestito
   - hint passo-passo
   - integra SoundFx (correct/wrong)
   ------------------------------------------------------------------------- */

private enum class HMRowKey { CARRY_P1, P1, CARRY_P2, P2, CARRY_SUM, SUM }
private enum class HMCellKind { CARRY, DIGIT }

private data class HMTarget(
    val row: HMRowKey,
    val col: Int, // 0..3 -> migliaia..unità
    val kind: HMCellKind,
    val expected: Char,
    val hint: String
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

    /* ✅ L’ULTIMO RIPORTO A SINISTRA NON VA NELLA CASELLA PICCOLA.
       - carryP1: mostro SOLO c11 (entra nelle decine). c12 va in P1 col1.
       - carryP2: mostro SOLO c21 (entra nelle centinaia). c22 va in P2 col0.
       - carrySUM: mostro carry unità->decine e decine->centinaia. carry centinaia->migliaia NO.
     */
    val carryP1 = CharArray(4) { ' ' }
    if (c11 > 0) carryP1[2] = c11.toString()[0]

    val carryP2 = CharArray(4) { ' ' }
    if (c21 > 0) carryP2[1] = c21.toString()[0]

    // carry somma
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
    // carry verso migliaia NON in carrySUM[0]

    // targets in ordine “elementari”
    val targets = mutableListOf<HMTarget>()

    fun addDigit(row: HMRowKey, col: Int, ch: Char, hint: String) {
        targets += HMTarget(row, col, HMCellKind.DIGIT, ch, hint)
    }
    fun addCarry(row: HMRowKey, col: Int, ch: Char, hint: String) {
        targets += HMTarget(row, col, HMCellKind.CARRY, ch, hint)
    }

    // --- P1
    addDigit(HMRowKey.P1, 3, w11.toString()[0], "${aU}×${bU} = $m1 → scrivi $w11 nelle unità")
    if (c11 > 0) addCarry(HMRowKey.CARRY_P1, 2, c11.toString()[0], "${aU}×${bU} = $m1 → riporta $c11 nelle decine")
    addDigit(HMRowKey.P1, 2, w12.toString()[0], "${aT}×${bU} = $m2raw${if (c11 > 0) " + $c11 = $m2" else ""} → scrivi $w12 nelle decine")
    if (c12 > 0) addDigit(HMRowKey.P1, 1, c12.toString()[0], "Ultimo riporto riga 1: scrivi $c12 nella casella delle centinaia")

    // --- P2 (unità = trattino fisso)
    addDigit(HMRowKey.P2, 2, w21.toString()[0], "Riga decine: ${aU}×${bT} = $n1 → scrivi $w21 (unità è un trattino)")
    if (c21 > 0) addCarry(HMRowKey.CARRY_P2, 1, c21.toString()[0], "Riporta $c21 nelle centinaia (riga 2)")
    addDigit(HMRowKey.P2, 1, w22.toString()[0], "${aT}×${bT} = $n2raw${if (c21 > 0) " + $c21 = $n2" else ""} → scrivi $w22 nelle centinaia")
    if (c22 > 0) addDigit(HMRowKey.P2, 0, c22.toString()[0], "Ultimo riporto riga 2: scrivi $c22 nella casella delle migliaia")

    // --- SOMMA
    fun addSum(col: Int, carryIn: Int, carryOut: Int, digit: Int) {
        addDigit(
            HMRowKey.SUM, col, digit.toString()[0],
            "Somma ${hmColLabel(col)}: ${p1d[col]} + ${p2d[col]}${if (carryIn > 0) " + riporto $carryIn" else ""} → scrivi $digit"
        )
        if (carryOut > 0 && col - 1 >= 0 && (col - 1) != 0) {
            addCarry(HMRowKey.CARRY_SUM, col - 1, carryOut.toString()[0], "Riporta $carryOut nella ${hmColLabel(col - 1)}")
        }
    }

    var cin = 0
    run { val s = p1d[3] + p2d[3] + cin; addSum(3, cin, s / 10, s % 10); cin = s / 10 }
    run { val s = p1d[2] + p2d[2] + cin; addSum(2, cin, s / 10, s % 10); cin = s / 10 }
    run { val s = p1d[1] + p2d[1] + cin; addSum(1, cin, s / 10, s % 10); cin = s / 10 }
    run {
        val s = p1d[0] + p2d[0] + cin
        addDigit(
            HMRowKey.SUM, 0, (s % 10).toString()[0],
            "Ultima colonna a sinistra: ${p1d[0]} + ${p2d[0]}${if (cin > 0) " + riporto $cin" else ""} → scrivi ${s % 10}"
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

/* ----------------------------- GAME COMPOSABLE (integrata in app) ----------------------------- */

@Composable
fun HardMultiplication2x2Game(
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    var plan by remember { mutableStateOf(hmComputePlan(47, 36)) }
    var step by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }

    // input arrays
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

    // dimensioni (scalano in base alla larghezza disponibile)
    val baseDigitW = 40.dp
    val baseDigitH = 56.dp
    val baseCarryW = 24.dp
    val baseCarryH = 30.dp
    val baseSignW = 26.dp
    val baseGap = 6.dp

    fun reset(newA: Int, newB: Int) {
        plan = hmComputePlan(newA, newB)
        step = 0
        inCarryP1 = CharArray(4) { '\u0000' }
        inP1 = CharArray(4) { '\u0000' }
        inCarryP2 = CharArray(4) { '\u0000' }
        inP2 = CharArray(4) { '\u0000' }.also { it[3] = '-' } // trattino fisso
        inCarrySUM = CharArray(4) { '\u0000' }
        inSUM = CharArray(4) { '\u0000' }

        errCarryP1 = BooleanArray(4) { false }
        errP1 = BooleanArray(4) { false }
        errCarryP2 = BooleanArray(4) { false }
        errP2 = BooleanArray(4) { false }
        errCarrySUM = BooleanArray(4) { false }
        errSUM = BooleanArray(4) { false }
    }

    LaunchedEffect(Unit) {
        val p2 = inP2.copyOf()
        p2[3] = '-'
        inP2 = p2
    }

    val current = plan.targets.getOrNull(step)
    val done = step >= plan.targets.size

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

    // il riporto sbiadisce SOLO DOPO che hai scritto la cifra che lo usa
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

        if (t.row == relatedDigitRow) {
            return t.col < carryCol
        }

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
            step = (step + 1).coerceAtMost(plan.targets.size)
            correctCount += 1
        } else {
            if (soundEnabled) fx.wrong()
        }
    }

    val hint = if (done) {
        "Bravo! ✅ Risultato: ${plan.result}"
    } else {
        current!!.hint
    }

    Box(Modifier.fillMaxSize()) {
        GameScreenFrame(
            title = "Moltiplicazioni difficili",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = hint,
            content = {
            SeaGlassPanel(title = "Esercizio") {
                Text(
                    "Esercizio: ${plan.a} × ${plan.b}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val totalCols = 4
                    val totalItems = totalCols + 1
                    val baseTotalWidth = baseSignW + (baseDigitW * totalCols) + (baseGap * (totalItems - 1))
                    val scale = (maxWidth.value / baseTotalWidth.value).coerceAtMost(1f)

                    val digitW = baseDigitW * scale
                    val digitH = baseDigitH * scale
                    val carryW = baseCarryW * scale
                    val carryH = baseCarryH * scale
                    val signW = baseSignW * scale
                    val gap = baseGap * scale

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HMGridRowRight(signW, gap) {
                            HMFixedDigit(plan.a4[0], digitW, digitH)
                            HMFixedDigit(plan.a4[1], digitW, digitH)
                            HMFixedDigit(plan.a4[2], digitW, digitH)
                            HMFixedDigit(plan.a4[3], digitW, digitH)
                            HMSignCell("", signW)
                        }

                        HMGridRowRight(signW, gap) {
                            HMFixedDigit(plan.b4[0], digitW, digitH)
                            HMFixedDigit(plan.b4[1], digitW, digitH)
                            HMFixedDigit(plan.b4[2], digitW, digitH)
                            HMFixedDigit(plan.b4[3], digitW, digitH)
                            HMSignCell("×", signW)
                        }

                        Divider(thickness = 2.dp)

                        HMCarryRowRight(
                            signW, gap, digitW, carryW, carryH,
                            expected = plan.carryP1,
                            input = inCarryP1,
                            err = errCarryP1,
                            enabled = { c -> enabled(HMRowKey.CARRY_P1, c, HMCellKind.CARRY) },
                            isActive = { c -> isActive(HMRowKey.CARRY_P1, c, HMCellKind.CARRY) },
                            shouldFade = { c -> carryShouldFade(HMRowKey.CARRY_P1, c) },
                            onChange = { c, v -> onTyped(HMRowKey.CARRY_P1, c, HMCellKind.CARRY, v) }
                        )

                        HMDigitRowRight(
                            signW, gap, digitW, digitH,
                            expected = plan.p1_4,
                            input = inP1,
                            err = errP1,
                            enabled = { c -> enabled(HMRowKey.P1, c, HMCellKind.DIGIT) },
                            isActive = { c -> isActive(HMRowKey.P1, c, HMCellKind.DIGIT) },
                            onChange = { c, v -> onTyped(HMRowKey.P1, c, HMCellKind.DIGIT, v) }
                        )

                        HMCarryRowRight(
                            signW, gap, digitW, carryW, carryH,
                            expected = plan.carryP2,
                            input = inCarryP2,
                            err = errCarryP2,
                            enabled = { c -> enabled(HMRowKey.CARRY_P2, c, HMCellKind.CARRY) },
                            isActive = { c -> isActive(HMRowKey.CARRY_P2, c, HMCellKind.CARRY) },
                            shouldFade = { c -> carryShouldFade(HMRowKey.CARRY_P2, c) },
                            onChange = { c, v -> onTyped(HMRowKey.CARRY_P2, c, HMCellKind.CARRY, v) }
                        )

                        HMDigitRowRight(
                            signW, gap, digitW, digitH,
                            expected = plan.p2_4,
                            input = inP2,
                            err = errP2,
                            enabled = { c -> enabled(HMRowKey.P2, c, HMCellKind.DIGIT) },
                            isActive = { c -> isActive(HMRowKey.P2, c, HMCellKind.DIGIT) },
                            onChange = { c, v -> onTyped(HMRowKey.P2, c, HMCellKind.DIGIT, v) },
                            fixedUnitDash = true
                        )

                        Divider(thickness = 2.dp)

                        HMCarryRowRight(
                            signW, gap, digitW, carryW, carryH,
                            expected = plan.carrySUM,
                            input = inCarrySUM,
                            err = errCarrySUM,
                            enabled = { c -> enabled(HMRowKey.CARRY_SUM, c, HMCellKind.CARRY) },
                            isActive = { c -> isActive(HMRowKey.CARRY_SUM, c, HMCellKind.CARRY) },
                            shouldFade = { c -> carryShouldFade(HMRowKey.CARRY_SUM, c) },
                            onChange = { c, v -> onTyped(HMRowKey.CARRY_SUM, c, HMCellKind.CARRY, v) }
                        )

                        HMDigitRowRight(
                            signW, gap, digitW, digitH,
                            expected = plan.res_4,
                            input = inSUM,
                            err = errSUM,
                            enabled = { c -> enabled(HMRowKey.SUM, c, HMCellKind.DIGIT) },
                            isActive = { c -> isActive(HMRowKey.SUM, c, HMCellKind.DIGIT) },
                            onChange = { c, v -> onTyped(HMRowKey.SUM, c, HMCellKind.DIGIT, v) }
                        )
                    }
                }
            }

            SeaGlassPanel(title = "Stato") {
                if (done) {
                    Text("Operazione completata.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Passo ${step + 1}/${plan.targets.size}", style = MaterialTheme.typography.bodyLarge)
                    Text("Casella attiva: ${hmRowLabel(current!!.row)} – ${hmColLabel(current.col)}")
                }
            }
        },
        bottomBar = {
            GameBottomActions(
                leftText = "Ricomincia",
                onLeft = { reset(plan.a, plan.b) },
                rightText = "Nuovo",
                onRight = { reset(Random.nextInt(10, 100), Random.nextInt(10, 100)) },
                center = {
                    OutlinedButton(onClick = {
                        for (c in 0..3) {
                            val cp1 = plan.carryP1[c]; if (cp1 != ' ') setCell(HMRowKey.CARRY_P1, c, cp1, false)
                            val cp2 = plan.carryP2[c]; if (cp2 != ' ') setCell(HMRowKey.CARRY_P2, c, cp2, false)
                            val cs = plan.carrySUM[c]; if (cs != ' ') setCell(HMRowKey.CARRY_SUM, c, cs, false)

                            val d1 = plan.p1_4[c]; if (d1 != ' ') setCell(HMRowKey.P1, c, d1, false)
                            val d2 = plan.p2_4[c]; if (d2 != ' ') setCell(HMRowKey.P2, c, d2, false)
                            val dr = plan.res_4[c]; if (dr != ' ') setCell(HMRowKey.SUM, c, dr, false)
                        }
                        setCell(HMRowKey.P2, 3, '-', false)
                        step = plan.targets.size
                    }) { Text("Soluzione") }
                }
            )
            }
        )

        BonusRewardHost(
            correctCount = correctCount,
            rewardsEarned = rewardsEarned,
            boardId = boardId,
            soundEnabled = soundEnabled,
            fx = fx,
            onRewardEarned = { rewardsEarned += 1 }
        )
    }
}

/* ----------------------------- GRID UI (RIGHT-ALIGNED) ----------------------------- */

@Composable
private fun HMGridRowRight(
    signW: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
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
private fun HMSignCell(text: String, w: androidx.compose.ui.unit.Dp) {
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
private fun HMFixedDigit(ch: Char, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        val color = if (ch == '-') MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        Text(
            if (ch == ' ') "" else ch.toString(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HMCarryRowRight(
    signW: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    digitW: androidx.compose.ui.unit.Dp,
    carryW: androidx.compose.ui.unit.Dp,
    carryH: androidx.compose.ui.unit.Dp,
    expected: CharArray,
    input: CharArray,
    err: BooleanArray,
    enabled: (Int) -> Boolean,
    isActive: (Int) -> Boolean,
    shouldFade: (Int) -> Boolean,
    onChange: (Int, String) -> Unit
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
                        w = carryW,
                        h = carryH,
                        fontSize = 16.sp,
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
    signW: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    digitW: androidx.compose.ui.unit.Dp,
    digitH: androidx.compose.ui.unit.Dp,
    expected: String,
    input: CharArray,
    err: BooleanArray,
    enabled: (Int) -> Boolean,
    isActive: (Int) -> Boolean,
    onChange: (Int, String) -> Unit,
    fixedUnitDash: Boolean = false
) {
    HMGridRowRight(signW, gap) {
        for (col in 0..3) {
            val exp = expected[col]

            if (fixedUnitDash && col == 3) {
                HMFixedDigit('-', digitW, digitH)
                continue
            }

            if (exp == ' ') {
                HMFixedDigit(' ', digitW, digitH)
            } else {
                val txt = if (input[col] == '\u0000') "" else input[col].toString()
                HMInputBox(
                    value = txt,
                    enabled = enabled(col),
                    isActive = isActive(col),
                    isError = err[col],
                    w = digitW,
                    h = digitH,
                    fontSize = 22.sp,
                    onValueChange = { onChange(col, it) }
                )
            }
        }
        HMSignCell("", signW)
    }
}

/* ----------------------------- INPUT BOX ----------------------------- */

@Composable
private fun HMInputBox(
    value: String,
    enabled: Boolean,
    isActive: Boolean,
    isError: Boolean,
    w: androidx.compose.ui.unit.Dp,
    h: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    val bg = when {
        isActive -> MaterialTheme.colorScheme.tertiaryContainer
        value.isNotBlank() && !isError -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isActive -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val borderW = if (isActive) 3.dp else 2.dp

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
