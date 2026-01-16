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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


private enum class DivRow { QUOTIENT, PRODUCT, REMAINDER }
private data class DivTarget(
    val row: DivRow,
    val stepIndex: Int,      // quale step (0..)
    val col: Int,            // posizione cifra dentro la riga
    val expected: Char,
    val hint: String
)

private data class DivStep(
    val endPos: Int,         // indice cifra dividend usata (0..n-1)
    val chunk: Int,          // numero su cui stai dividendo in questo passo
    val qDigit: Int,         // cifra quoziente
    val product: Int,        // qDigit * divisor
    val remainder: Int       // chunk - product
)

private data class DivPlan(
    val dividend: Int,
    val divisor: Int,
    val dividendDigits: List<Int>,
    val quotientDigits: List<Int?>, // null prima di iniziare
    val steps: List<DivStep>,
    val targets: List<DivTarget>,
    val finalQuotient: Int,
    val finalRemainder: Int
)

private fun generateDivision(rng: Random): Pair<Int, Int> {
    val divisor = rng.nextInt(2, 10) // 2..9
    val digits = if (rng.nextBoolean()) 2 else 3
    val min = if (digits == 2) 10 else 100
    val max = if (digits == 2) 99 else 999
    val dividend = rng.nextInt(min, max + 1)
    return dividend to divisor
}

private fun computeDivisionPlan(dividend: Int, divisor: Int): DivPlan {
    val ds = dividend.toString().map { it.digitToInt() }
    val n = ds.size

    val steps = mutableListOf<DivStep>()
    val quotient = MutableList<Int?>(n) { null }

    var acc = 0
    var started = false
    var qValue = 0

    for (i in 0 until n) {
        acc = acc * 10 + ds[i]

        if (!started && acc < divisor) {
            // finché non “parte” la divisione, niente cifra quoziente
            continue
        }

        started = true
        val qDigit = acc / divisor
        val product = qDigit * divisor
        val rem = acc - product

        quotient[i] = qDigit
        steps += DivStep(
            endPos = i,
            chunk = acc,
            qDigit = qDigit,
            product = product,
            remainder = rem
        )

        acc = rem
        qValue = qValue * 10 + qDigit
    }

    val finalQuotient = if (qValue == 0) 0 else qValue
    val finalRemainder = acc

    // Targets: per ogni step -> cifra quoziente -> cifre prodotto -> cifre resto
    val targets = mutableListOf<DivTarget>()
    fun add(row: DivRow, stepIndex: Int, col: Int, expected: Char, hint: String) {
        targets += DivTarget(row, stepIndex, col, expected, hint)
    }

    steps.forEachIndexed { si, st ->
        val qCh = st.qDigit.toString()[0]
        add(
            row = DivRow.QUOTIENT,
            stepIndex = si,
            col = 0,
            expected = qCh,
            hint = "Quante volte $divisor sta in ${st.chunk}? Scrivi la cifra del quoziente."
        )

        val prodStr = st.product.toString()
        // scriviamo il prodotto da destra verso sinistra (più naturale in colonna)
        for (k in prodStr.indices.reversed()) {
            add(
                row = DivRow.PRODUCT,
                stepIndex = si,
                col = k,
                expected = prodStr[k],
                hint = "Moltiplica: $divisor × ${st.qDigit} = ${st.product}. Scrivi il prodotto."
            )
        }

        val remStr = st.remainder.toString()
        for (k in remStr.indices.reversed()) {
            add(
                row = DivRow.REMAINDER,
                stepIndex = si,
                col = k,
                expected = remStr[k],
                hint = "Sottrai: ${st.chunk} − ${st.product} = ${st.remainder}. Scrivi il resto."
            )
        }
    }

    return DivPlan(
        dividend = dividend,
        divisor = divisor,
        dividendDigits = ds,
        quotientDigits = quotient,
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
    val shape = RoundedCornerShape(12.dp)

    val bg = when {
        active -> Color(0xFFFFF3CC)
        value.isNotBlank() && !isError -> Color(0xFFDCFCE7)
        else -> Color.White.copy(alpha = 0.85f)
    }

    val border = when {
        isError -> Color(0xFFEF4444)
        active -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(bg)
            .border(if (active) 3.dp else 2.dp, border, shape),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val d = it.filter { ch -> ch.isDigit() }.takeLast(1)
                onValueChange(d)
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF111827)
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
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.70f))
            .border(2.dp, Color.White.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF111827)
        )
    }
}

@Composable
fun DivisionStepGame(
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }

    fun newPlan(): DivPlan {
        val (dividend, divisor) = generateDivision(rng)
        return computeDivisionPlan(dividend, divisor)
    }

    var plan by remember { mutableStateOf(newPlan()) }

    var stepIndex by remember(plan) { mutableStateOf(0) }
    val current = plan.targets.getOrNull(stepIndex)
    val done = current == null

    var correctCount by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    // input per step:
    // - quotient: una cifra per step (ma visivamente sopra il dividend)
    // - product: stringa variabile
    // - remainder: stringa variabile
    val qInputs = remember(plan) { mutableStateListOf<String>().apply { repeat(plan.steps.size) { add("") } } }
    val prodInputs = remember(plan) {
        mutableStateListOf<MutableList<String>>().apply {
            plan.steps.forEach { st ->
                val len = st.product.toString().length
                add(MutableList(len) { "" })
            }
        }
    }
    val remInputs = remember(plan) {
        mutableStateListOf<MutableList<String>>().apply {
            plan.steps.forEach { st ->
                val len = st.remainder.toString().length
                add(MutableList(len) { "" })
            }
        }
    }

    val prodErr = remember(plan) { mutableStateListOf<MutableList<Boolean>>().apply { prodInputs.forEach { add(MutableList(it.size) { false }) } } }
    val remErr = remember(plan) { mutableStateListOf<MutableList<Boolean>>().apply { remInputs.forEach { add(MutableList(it.size) { false }) } } }
    val qErr = remember(plan) { mutableStateListOf<Boolean>().apply { repeat(plan.steps.size) { add(false) } } }

    fun resetSame() {
        stepIndex = 0
        message = null
        for (i in qInputs.indices) { qInputs[i] = ""; qErr[i] = false }
        for (si in prodInputs.indices) {
            for (c in prodInputs[si].indices) { prodInputs[si][c] = ""; prodErr[si][c] = false }
        }
        for (si in remInputs.indices) {
            for (c in remInputs[si].indices) { remInputs[si][c] = ""; remErr[si][c] = false }
        }
    }

    fun resetNew() {
        plan = newPlan()
        resetSame()
    }

    fun playCorrect() { if (soundEnabled) fx.correct() }
    fun playWrong() { if (soundEnabled) fx.wrong() }

    fun isActive(row: DivRow, si: Int, col: Int): Boolean {
        val t = current ?: return false
        return t.row == row && t.stepIndex == si && t.col == col
    }

    fun onTyped(row: DivRow, si: Int, col: Int, v: String) {
        val t = current ?: return
        if (t.row != row || t.stepIndex != si || t.col != col) return

        val d = v.firstOrNull()
        if (d == null) return

        val ok = d == t.expected
        if (!ok) {
            // rosso + cancella cifra
            when (row) {
                DivRow.QUOTIENT -> { qErr[si] = true; qInputs[si] = "" }
                DivRow.PRODUCT -> { prodErr[si][col] = true; prodInputs[si][col] = "" }
                DivRow.REMAINDER -> { remErr[si][col] = true; remInputs[si][col] = "" }
            }
            message = "❌ Riprova"
            playWrong()
            return
        }

        // ok: scrivi e avanza
        when (row) {
            DivRow.QUOTIENT -> { qErr[si] = false; qInputs[si] = d.toString() }
            DivRow.PRODUCT -> { prodErr[si][col] = false; prodInputs[si][col] = d.toString() }
            DivRow.REMAINDER -> { remErr[si][col] = false; remInputs[si][col] = d.toString() }
        }

        message = null
        playCorrect()
        stepIndex++

        if (stepIndex >= plan.targets.size) {
            correctCount++
            message = "✅ Finito! Quoziente ${plan.finalQuotient} resto ${plan.finalRemainder}"
        }
    }

    val hint = if (done) {
        "Bravo! Quoziente ${plan.finalQuotient} con resto ${plan.finalRemainder}."
    } else {
        current!!.hint
    }
    val activeStepNumber = current?.stepIndex?.plus(1) ?: plan.steps.size
    val activeAction = when (current?.row) {
        DivRow.QUOTIENT -> "Scrivi la cifra del quoziente."
        DivRow.PRODUCT -> "Scrivi il prodotto."
        DivRow.REMAINDER -> "Scrivi il resto."
        null -> "Hai completato tutti i passi!"
    }
    val activeChunk = current?.stepIndex?.let { plan.steps[it].chunk }

    GameScreenFrame(
        title = "Divisioni passo passo",
        soundEnabled = soundEnabled,
        onToggleSound = onToggleSound,
        onBack = onBack,
        onOpenLeaderboard = onOpenLeaderboard,
        correctCount = correctCount,
        hintText = hint,
        message = message,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SeaGlassPanel(title = "Come si fa") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1) Prendi le prime cifre finché puoi dividere per il divisore.", color = Color(0xFF374151))
                        Text("2) Scrivi la cifra del quoziente sopra al numero.", color = Color(0xFF374151))
                        Text("3) Moltiplica il divisore × quella cifra e scrivi il prodotto.", color = Color(0xFF374151))
                        Text("4) Sottrai per trovare il resto.", color = Color(0xFF374151))
                        Text("5) Abbassa la cifra successiva e ripeti i passi 2-4.", color = Color(0xFF374151))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (done) {
                                "Hai completato tutti i passi!"
                            } else {
                                "Adesso: passo $activeStepNumber/${plan.steps.size} • $activeChunk ÷ ${plan.divisor} • $activeAction"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SeaGlassPanel(title = "Esercizio") {
                    // Riga principale: divisor | dividend
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FixedBox(plan.divisor.toString(), w = 52.dp, h = 56.dp, fontSize = 22)
                        Text("⟌", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            plan.dividendDigits.forEach { d ->
                                FixedBox(d.toString())
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quoziente: una casella per step (in ordine)
                    Text("Quoziente", fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        plan.steps.forEachIndexed { si, _ ->
                            DigitBox(
                                value = qInputs[si],
                                enabled = isActive(DivRow.QUOTIENT, si, 0),
                                active = isActive(DivRow.QUOTIENT, si, 0),
                                isError = qErr[si],
                                onValueChange = { onTyped(DivRow.QUOTIENT, si, 0, it) },
                                w = 44.dp, h = 56.dp, fontSize = 22
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Mostro i passi (chunk / prodotto / resto) con caselle controllate
                    plan.steps.forEachIndexed { si, st ->
                        val prodStr = st.product.toString()
                        val remStr = st.remainder.toString()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Passo ${si + 1}: ${st.chunk} ÷ ${plan.divisor}", fontWeight = FontWeight.Black)
                            Text("Prima il quoziente, poi prodotto e resto.", color = Color(0xFF6B7280))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Prodotto", fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (c in prodStr.indices) {
                                        DigitBox(
                                            value = prodInputs[si][c],
                                            enabled = isActive(DivRow.PRODUCT, si, c),
                                            active = isActive(DivRow.PRODUCT, si, c),
                                            isError = prodErr[si][c],
                                            onValueChange = { onTyped(DivRow.PRODUCT, si, c, it) },
                                            w = 40.dp, h = 52.dp, fontSize = 20
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Resto", fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (c in remStr.indices) {
                                        DigitBox(
                                            value = remInputs[si][c],
                                            enabled = isActive(DivRow.REMAINDER, si, c),
                                            active = isActive(DivRow.REMAINDER, si, c),
                                            isError = remErr[si][c],
                                            onValueChange = { onTyped(DivRow.REMAINDER, si, c, it) },
                                            w = 40.dp, h = 52.dp, fontSize = 20
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { resetSame() }, modifier = Modifier.weight(1f)) {
                    Text("Ricomincia")
                }
                OutlinedButton(onClick = { resetNew() }, modifier = Modifier.weight(1f)) {
                    Text("Nuovo")
                }
            }
        }
    )
}
