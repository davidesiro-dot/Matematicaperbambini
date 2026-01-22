package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.random.Random

private enum class DivMode { DIV_1DIG, DIV_2DIG }

private enum class DivTargetType { QUOTIENT, PRODUCT, REMAINDER, BRING_DOWN }

private data class DivTarget(
    val type: DivTargetType,
    val stepIndex: Int,
    val col: Int,          // ✅ colonna ASSOLUTA nella griglia del dividendo (0..n-1)
    val expected: Char?,   // per BRING_DOWN può essere null
    val hint: String
)

private data class DivStep(
    val endPos: Int,
    val partial: Int,
    val qDigit: Int,
    val product: Int,
    val remainder: Int,
    val bringDownDigit: Int?
)

private data class DivPlan(
    val dividend: Int,
    val divisor: Int,
    val dividendDigits: List<Int>,
    val quotientDigits: List<Int>,
    val steps: List<DivStep>,
    val targets: List<DivTarget>,
    val finalQuotient: Int,
    val finalRemainder: Int
)

private data class DivConfig(
    val dividendDigitsRange: IntRange,
    val divisorRange: IntRange
)

private fun configFor(mode: DivMode): DivConfig = when (mode) {
    DivMode.DIV_1DIG -> DivConfig(dividendDigitsRange = 2..4, divisorRange = 2..9)
    DivMode.DIV_2DIG -> DivConfig(dividendDigitsRange = 3..5, divisorRange = 10..79)
}

private fun generateDivision(rng: Random, mode: DivMode): Pair<Int, Int> {
    val config = configFor(mode)
    val digits = rng.nextInt(config.dividendDigitsRange.first, config.dividendDigitsRange.last + 1)
    val minDividend = 10.0.pow(digits - 1).toInt()
    val maxDividend = (10.0.pow(digits.toDouble()).toInt()) - 1
    val dividend = rng.nextInt(minDividend, maxDividend + 1)
    val divisor = rng.nextInt(config.divisorRange.first, config.divisorRange.last + 1)
    return dividend to divisor
}

private fun estimateQuotientDigit(partial: Int, divisor: Int): Int {
    if (partial < divisor) return 0
    if (divisor < 10) return partial / divisor

    val leadingDivisor = divisor / 10
    val partialStr = partial.toString()
    val leadingPartial = if (partialStr.length >= 2) partialStr.substring(0, 2).toInt() else partialStr.toInt()

    var qDigit = minOf(9, leadingPartial / leadingDivisor)
    while (qDigit > 0 && divisor * qDigit > partial) qDigit--
    return qDigit
}

/** ✅ colonna di inizio (assoluta) per allineare un numero di len cifre che termina in endPos */
private fun startColForEnd(endPos: Int, len: Int): Int = (endPos - len + 1).coerceAtLeast(0)

private fun generateDivisionPlan(dividend: Int, divisor: Int): DivPlan {
    val ds = dividend.toString().map { it.digitToInt() }
    val n = ds.size

    val steps = mutableListOf<DivStep>()
    var index = 0
    var partial = ds.first()
    while (partial < divisor && index < n - 1) {
        index++
        partial = partial * 10 + ds[index]
    }

    while (true) {
        val qDigit = if (partial < divisor) 0 else estimateQuotientDigit(partial, divisor)
        val product = qDigit * divisor
        val rem = partial - product
        val bringDownDigit = if (index < n - 1) ds[index + 1] else null

        steps += DivStep(
            endPos = index,
            partial = partial,
            qDigit = qDigit,
            product = product,
            remainder = rem,
            bringDownDigit = bringDownDigit
        )

        if (index >= n - 1) break
        index++
        partial = rem * 10 + ds[index]
    }

    val quotientDigits = steps.map { it.qDigit }
    val quotientString = quotientDigits.joinToString("")
    val finalQuotient = quotientString.trimStart('0').ifEmpty { "0" }.toInt()
    val finalRemainder = steps.lastOrNull()?.remainder ?: 0

    val targets = mutableListOf<DivTarget>()
    fun add(type: DivTargetType, stepIndex: Int, col: Int, expected: Char?, hint: String) {
        targets += DivTarget(type, stepIndex, col, expected, hint)
    }

    steps.forEachIndexed { si, st ->
        // QUOZIENTE: una cifra per step, col=si (non è una colonna del dividendo, ma non ci serve allinearla)
        val qCh = st.qDigit.toString()[0]
        add(
            type = DivTargetType.QUOTIENT,
            stepIndex = si,
            col = si, // ✅ così ogni casella del quoziente è distinta
            expected = qCh,
            hint = "Trova la cifra del quoziente: il numero più grande che, moltiplicato per $divisor, dà un risultato ≤ ${st.partial}."
        )

        // PRODOTTO: target sulle colonne ASSOLUTE della griglia
        val prodStr = st.product.toString()
        val prodStart = startColForEnd(st.endPos, prodStr.length)
        for (k in prodStr.indices) {
            val absCol = prodStart + k
            add(
                type = DivTargetType.PRODUCT,
                stepIndex = si,
                col = absCol,
                expected = prodStr[k],
                hint = "Moltiplica: $divisor × ${st.qDigit} = ${st.product}. Scrivi il prodotto sotto le cifre giuste."
            )
        }

        // RESTO: target sulle colonne ASSOLUTE della griglia
        val remStr = st.remainder.toString()
        val remStart = startColForEnd(st.endPos, remStr.length)
        val remainderHint = "Sottrai: ${st.partial} − ${st.product} = ${st.remainder}. Scrivi il resto."
        for (k in remStr.indices) {
            val absCol = remStart + k
            add(
                type = DivTargetType.REMAINDER,
                stepIndex = si,
                col = absCol,
                expected = remStr[k],
                hint = remainderHint
            )
        }

        if (st.bringDownDigit != null) {
            add(
                type = DivTargetType.BRING_DOWN,
                stepIndex = si,
                col = st.endPos + 1, // ✅ colonna ASSOLUTA dove “scende” la cifra
                expected = null,
                hint = "Abbassa la cifra successiva (${st.bringDownDigit}) accanto al resto per formare il nuovo parziale."
            )
        }
    }

    return DivPlan(
        dividend = dividend,
        divisor = divisor,
        dividendDigits = ds,
        quotientDigits = quotientDigits,
        steps = steps,
        targets = targets,
        finalQuotient = finalQuotient,
        finalRemainder = finalRemainder
    )
}

@Composable
private fun DigitBox(
    value: String,
    enabled: Boolean,
    active: Boolean,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    w: Dp = 44.dp,
    h: Dp = 56.dp,
    fontSize: Int = 22
) {
    val shape = RoundedCornerShape(10.dp)

    val bg = when {
        active -> MaterialTheme.colorScheme.tertiaryContainer
        value.isNotBlank() && !isError -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val border = when {
        isError -> MaterialTheme.colorScheme.error
        active -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderW = if (active) 3.dp else 2.dp

    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(borderW, border, shape),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                // ✅ se la casella NON è attiva, ignora completamente l’input
                if (!enabled) return@BasicTextField

                val d = raw.filter { ch -> ch.isDigit() }.takeLast(1)
                onValueChange(d)
            },
            enabled = true,          // ✅ sempre true: così prende focus e mostra tastiera
            readOnly = !enabled,     // ✅ ma se non è attiva non permette di modificare
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
            }
        )
    }
}

@Composable
private fun FixedBox(
    text: String,
    w: Dp = 44.dp,
    h: Dp = 56.dp,
    fontSize: Int = 22
) {
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
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionBox(
    text: String,
    active: Boolean,
    w: Dp = 44.dp,
    h: Dp = 56.dp,
    fontSize: Int = 20
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val border = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(if (active) 3.dp else 2.dp, border, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DivisionDigitRow(
    columns: Int,
    cellW: Dp,
    cellH: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
    cell: @Composable (Int) -> Unit
) {
    val rowWidth = cellW * columns + gap * (columns - 1)
    Row(
        modifier = modifier.width(rowWidth),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        for (col in 0 until columns) {
            Box(
                modifier = Modifier.width(cellW).height(cellH),
                contentAlignment = Alignment.Center
            ) { cell(col) }
        }
    }
}

@Composable
private fun DivisionCompactWorksheet(
    plan: DivPlan,
    qInputs: List<String>,
    qErr: List<Boolean>,
    prodInputs: List<List<String>>,
    prodErr: List<List<Boolean>>,
    remInputs: List<List<String>>,
    remErr: List<List<Boolean>>,
    bringDownDone: List<Boolean>,
    isActive: (DivTargetType, Int, Int) -> Boolean,
    onTyped: (DivTargetType, Int, Int, String) -> Unit,
    onBringDown: (Int) -> Unit,
    ui: UiSizing
) {
    val digitW = if (ui.isCompact) 36.dp else 44.dp
    val digitH = if (ui.isCompact) 48.dp else 56.dp
    val digitSmallW = if (ui.isCompact) 34.dp else 40.dp
    val digitSmallH = if (ui.isCompact) 46.dp else 52.dp
    val fontLarge = if (ui.isCompact) 18 else 22
    val fontSmall = if (ui.isCompact) 16 else 20
    val gap = if (ui.isCompact) 4.dp else 6.dp
    val columns = plan.dividendDigits.size

    val divisorDigits = plan.divisor.toString()
    val divisorWidth = digitW * divisorDigits.length + gap * (divisorDigits.length - 1)
    val dividerHeight = digitH + digitH + gap
    val stepGap = if (ui.isCompact) 6.dp else 8.dp

    Column(verticalArrangement = Arrangement.spacedBy(stepGap)) {
        // Riga dividendo + divisore + quoziente
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
                    FixedBox(plan.dividendDigits[col].toString(), w = digitW, h = digitH, fontSize = fontLarge)
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
                        FixedBox(ch.toString(), w = digitW, h = digitH, fontSize = fontLarge)
                    }
                }
                Box(
                    modifier = Modifier
                        .width(divisorWidth)
                        .height(if (ui.isCompact) 2.dp else 3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )

                // Quoziente: una casella per step (col=si)
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    plan.steps.forEachIndexed { si, _ ->
                        DigitBox(
                            value = qInputs[si],
                            enabled = isActive(DivTargetType.QUOTIENT, si, si),
                            active = isActive(DivTargetType.QUOTIENT, si, si),
                            isError = qErr[si],
                            onValueChange = { onTyped(DivTargetType.QUOTIENT, si, si, it) },
                            w = digitW,
                            h = digitH,
                            fontSize = fontLarge
                        )
                    }
                }
            }
        }

        // Righe prodotto/resto/abbassa per step
        Column(verticalArrangement = Arrangement.spacedBy(stepGap)) {
            plan.steps.forEachIndexed { si, st ->
                val prodStr = st.product.toString()
                val remStr = st.remainder.toString()

                val prodStart = startColForEnd(st.endPos, prodStr.length)
                val remStart = startColForEnd(st.endPos, remStr.length)

                // PRODOTTO (allineato)
                DivisionDigitRow(
                    columns = columns,
                    cellW = digitSmallW,
                    cellH = digitSmallH,
                    gap = gap
                ) { col ->
                    if (col in prodStart until (prodStart + prodStr.length)) {
                        val idx = col - prodStart
                        DigitBox(
                            value = prodInputs[si][idx],
                            enabled = isActive(DivTargetType.PRODUCT, si, col),
                            active = isActive(DivTargetType.PRODUCT, si, col),
                            isError = prodErr[si][idx],
                            onValueChange = { onTyped(DivTargetType.PRODUCT, si, col, it) },
                            w = digitSmallW,
                            h = digitSmallH,
                            fontSize = fontSmall
                        )
                    }
                }

                // RESTO (allineato)
                DivisionDigitRow(
                    columns = columns,
                    cellW = digitSmallW,
                    cellH = digitSmallH,
                    gap = gap
                ) { col ->
                    if (col in remStart until (remStart + remStr.length)) {
                        val idx = col - remStart
                        DigitBox(
                            value = remInputs[si][idx],
                            enabled = isActive(DivTargetType.REMAINDER, si, col),
                            active = isActive(DivTargetType.REMAINDER, si, col),
                            isError = remErr[si][idx],
                            onValueChange = { onTyped(DivTargetType.REMAINDER, si, col, it) },
                            w = digitSmallW,
                            h = digitSmallH,
                            fontSize = fontSmall
                        )
                    }
                }

                // ABBASSA (nella colonna endPos+1)
                if (st.bringDownDigit != null) {
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
                            if (col == st.endPos + 1) {
                                ActionBox(
                                    text = st.bringDownDigit.toString(),
                                    active = isActive(DivTargetType.BRING_DOWN, si, col),
                                    w = digitSmallW,
                                    h = digitSmallH,
                                    fontSize = fontSmall
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onBringDown(si) },
                            enabled = isActive(DivTargetType.BRING_DOWN, si, st.endPos + 1)
                        ) {
                            Text(if (bringDownDone[si]) "Abbassato" else "Abbassa")
                        }
                    }
                }
            }
        }
    }
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
    var mode by remember { mutableStateOf(DivMode.DIV_1DIG) }

    fun newPlan(): DivPlan {
        val (dividend, divisor) = generateDivision(rng, mode)
        return generateDivisionPlan(dividend, divisor)
    }

    var plan by remember { mutableStateOf(newPlan()) }

    var stepIndex by remember(plan) { mutableStateOf(0) }
    val current = plan.targets.getOrNull(stepIndex)
    val done = current == null

    var correctCount by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val qInputs = remember(plan) { mutableStateListOf<String>().apply { repeat(plan.steps.size) { add("") } } }

    val prodInputs = remember(plan) {
        mutableStateListOf<MutableList<String>>().apply {
            plan.steps.forEach { st ->
                add(MutableList(st.product.toString().length) { "" })
            }
        }
    }
    val remInputs = remember(plan) {
        mutableStateListOf<MutableList<String>>().apply {
            plan.steps.forEach { st ->
                add(MutableList(st.remainder.toString().length) { "" })
            }
        }
    }

    val prodErr = remember(plan) {
        mutableStateListOf<MutableList<Boolean>>().apply {
            prodInputs.forEach { add(MutableList(it.size) { false }) }
        }
    }
    val remErr = remember(plan) {
        mutableStateListOf<MutableList<Boolean>>().apply {
            remInputs.forEach { add(MutableList(it.size) { false }) }
        }
    }
    val qErr = remember(plan) { mutableStateListOf<Boolean>().apply { repeat(plan.steps.size) { add(false) } } }
    val bringDownDone = remember(plan) { mutableStateListOf<Boolean>().apply { repeat(plan.steps.size) { add(false) } } }

    fun resetSame() {
        stepIndex = 0
        message = null
        showSuccessDialog = false
        for (i in qInputs.indices) { qInputs[i] = ""; qErr[i] = false }
        for (si in prodInputs.indices) {
            for (i in prodInputs[si].indices) { prodInputs[si][i] = ""; prodErr[si][i] = false }
        }
        for (si in remInputs.indices) {
            for (i in remInputs[si].indices) { remInputs[si][i] = ""; remErr[si][i] = false }
        }
        for (i in bringDownDone.indices) bringDownDone[i] = false
    }

    fun resetNew() {
        plan = newPlan()
        // resetSame verrà rieseguito dai remember(plan) ma lo teniamo per chiarezza/consistenza
        resetSame()
    }

    fun playCorrect() { if (soundEnabled) fx.correct() }
    fun playWrong() { if (soundEnabled) fx.wrong() }

    fun isActive(type: DivTargetType, si: Int, col: Int): Boolean {
        val t = current ?: return false
        return t.type == type && t.stepIndex == si && t.col == col
    }

    fun advanceStep() {
        stepIndex++
        if (stepIndex >= plan.targets.size) {
            correctCount++
            message = "✅ Finito! Quoziente ${plan.finalQuotient} resto ${plan.finalRemainder}"
        }
    }

    // ✅ traduzione colonna assoluta -> indice locale dentro prodInputs/remInputs
    fun localIndexFor(type: DivTargetType, si: Int, absCol: Int): Int {
        val st = plan.steps[si]
        return when (type) {
            DivTargetType.PRODUCT -> {
                val start = startColForEnd(st.endPos, st.product.toString().length)
                absCol - start
            }
            DivTargetType.REMAINDER -> {
                val start = startColForEnd(st.endPos, st.remainder.toString().length)
                absCol - start
            }
            else -> 0
        }
    }

    fun onTyped(type: DivTargetType, si: Int, col: Int, v: String) {
        val t = current ?: return
        if (t.type != type || t.stepIndex != si || t.col != col) return

        val d = v.firstOrNull() ?: return
        val ok = d == t.expected

        if (!ok) {
            when (type) {
                DivTargetType.QUOTIENT -> { qErr[si] = true; qInputs[si] = "" }
                DivTargetType.PRODUCT -> {
                    val idx = localIndexFor(type, si, col)
                    if (idx in prodErr[si].indices) prodErr[si][idx] = true
                    if (idx in prodInputs[si].indices) prodInputs[si][idx] = ""
                }
                DivTargetType.REMAINDER -> {
                    val idx = localIndexFor(type, si, col)
                    if (idx in remErr[si].indices) remErr[si][idx] = true
                    if (idx in remInputs[si].indices) remInputs[si][idx] = ""
                }
                DivTargetType.BRING_DOWN -> Unit
            }
            message = "❌ Riprova"
            playWrong()
            return
        }

        when (type) {
            DivTargetType.QUOTIENT -> { qErr[si] = false; qInputs[si] = d.toString() }
            DivTargetType.PRODUCT -> {
                val idx = localIndexFor(type, si, col)
                if (idx in prodErr[si].indices) prodErr[si][idx] = false
                if (idx in prodInputs[si].indices) prodInputs[si][idx] = d.toString()
            }
            DivTargetType.REMAINDER -> {
                val idx = localIndexFor(type, si, col)
                if (idx in remErr[si].indices) remErr[si][idx] = false
                if (idx in remInputs[si].indices) remInputs[si][idx] = d.toString()
            }
            DivTargetType.BRING_DOWN -> Unit
        }

        message = null
        playCorrect()
        advanceStep()
    }

    fun onBringDown(si: Int) {
        val t = current ?: return
        val col = plan.steps[si].endPos + 1
        if (t.type != DivTargetType.BRING_DOWN || t.stepIndex != si || t.col != col) return
        bringDownDone[si] = true
        message = null
        playCorrect()
        advanceStep()
    }

    fun fillSolution() {
        plan.steps.forEachIndexed { si, st ->
            qInputs[si] = st.qDigit.toString()
            qErr[si] = false

            val prodStr = st.product.toString()
            prodStr.forEachIndexed { idx, ch ->
                prodInputs[si][idx] = ch.toString()
                prodErr[si][idx] = false
            }

            val remStr = st.remainder.toString()
            remStr.forEachIndexed { idx, ch ->
                remInputs[si][idx] = ch.toString()
                remErr[si][idx] = false
            }

            if (st.bringDownDigit != null) bringDownDone[si] = true
        }
        stepIndex = plan.targets.size
        message = "✅ Soluzione completata! Quoziente ${plan.finalQuotient} resto ${plan.finalRemainder}"
        showSuccessDialog = true
    }

    val hint = if (done) {
        "Bravo! Quoziente ${plan.finalQuotient} con resto ${plan.finalRemainder}."
    } else current!!.hint

    LaunchedEffect(done) {
        if (done) showSuccessDialog = true
    }

    val activeStepNumber = current?.stepIndex?.plus(1) ?: plan.steps.size

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()

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
                            val oneDigitSelected = mode == DivMode.DIV_1DIG
                            val twoDigitSelected = mode == DivMode.DIV_2DIG

                            if (oneDigitSelected) {
                                androidx.compose.material3.Button(
                                    onClick = { mode = DivMode.DIV_1DIG; resetNew() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Divisore 1 cifra") }
                            } else {
                                OutlinedButton(
                                    onClick = { mode = DivMode.DIV_1DIG; resetNew() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Divisore 1 cifra") }
                            }

                            if (twoDigitSelected) {
                                androidx.compose.material3.Button(
                                    onClick = { mode = DivMode.DIV_2DIG; resetNew() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Divisore 2 cifre") }
                            } else {
                                OutlinedButton(
                                    onClick = { mode = DivMode.DIV_2DIG; resetNew() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Divisore 2 cifre") }
                            }
                        }
                    }

                    SeaGlassPanel(title = "Calcolo") {
                        DivisionCompactWorksheet(
                            plan = plan,
                            qInputs = qInputs,
                            qErr = qErr,
                            prodInputs = prodInputs,
                            prodErr = prodErr,
                            remInputs = remInputs,
                            remErr = remErr,
                            bringDownDone = bringDownDone,
                            isActive = ::isActive,
                            onTyped = ::onTyped,
                            onBringDown = ::onBringDown,
                            ui = ui
                        )
                    }

                    SeaGlassPanel(title = "Aiuto") {
                        Column(verticalArrangement = Arrangement.spacedBy(if (ui.isCompact) 4.dp else 6.dp)) {
                            Text(text = hint, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (done) "Passo ${plan.steps.size}/${plan.steps.size}" else "Passo $activeStepNumber/${plan.steps.size}",
                                fontWeight = FontWeight.Bold,
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
                        OutlinedButton(onClick = { fillSolution() }) { Text("Soluzione") }
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
