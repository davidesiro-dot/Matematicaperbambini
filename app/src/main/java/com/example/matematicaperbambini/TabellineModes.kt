package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun TabellineMixedGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var a by remember { mutableStateOf(rng.nextInt(1, 11)) }
    var b by remember { mutableStateOf(rng.nextInt(1, 11)) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }

    fun newQuestion() {
        a = rng.nextInt(1, 11)
        b = rng.nextInt(1, 11)
        input = ""
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val questionSize = if (ui.isCompact) 30.sp else 36.sp
        val inputFontSize = if (ui.isCompact) 18.sp else 22.sp
        val inputWidth = if (ui.isCompact) 150.dp else 180.dp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Tabelline miste",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Scrivi il risultato della moltiplicazione.",
            ui = ui,
            message = msg,
            content = {
                SeaGlassPanel(title = "Quanto fa?") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ui.spacing)
                    ) {
                        Text(
                            "$a × $b",
                            fontSize = questionSize,
                            fontWeight = FontWeight.ExtraBold
                        )

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = inputFontSize,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(inputWidth)
                        )
                    }
                }

                Button(
                    onClick = {
                        val user = input.toIntOrNull()
                        if (user == a * b) {
                            msg = "✅ Corretto!"
                            correctCount += 1
                            if (soundEnabled) fx.correct()
                            newQuestion()
                        } else {
                            msg = "❌ Riprova"
                            if (soundEnabled) fx.wrong()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(actionHeight),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) { Text("Controlla") }
            }
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
    }
}

@Composable
fun TabellineGapsGame(
    table: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    val inputs = remember { mutableStateListOf<String>().apply { repeat(10) { add("") } } }
    val ok = remember { mutableStateListOf<Boolean?>().apply { repeat(10) { add(null) } } }
    var blanks by remember { mutableStateOf(setOf<Int>()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }

    fun resetRound() {
        blanks = (1..10).shuffled(rng).take(4).toSet()
        for (i in 0 until 10) {
            inputs[i] = ""
            ok[i] = null
        }
    }

    LaunchedEffect(Unit) { resetRound() }

    LaunchedEffect(msg) {
        if (!msg.isNullOrBlank()) {
            delay(1200)
            msg = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val boxW = if (ui.isCompact) ui.cellSmall else ui.cell
        val boxH = if (ui.isCompact) ui.cellSmall else ui.cell
        val fontSize = if (ui.isCompact) 16.sp else 18.sp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Buchi nella tabellina",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Completa solo i risultati mancanti.",
            ui = ui,
            message = msg,
            content = {
                SeaGlassPanel(title = "Tabellina del $table") {
                    Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                        for (i in 1..10) {
                            val index = i - 1
                            val expected = table * i
                            val expectedLength = expected.toString().length
                            val isBlank = blanks.contains(i)

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ui.spacing)
                            ) {
                                Text(
                                    "$table × $i",
                                    modifier = Modifier.width(if (ui.isCompact) 70.dp else 80.dp),
                                    fontWeight = FontWeight.Bold
                                )

                                if (isBlank) {
                                    OutlinedTextField(
                                        value = inputs[index],
                                        onValueChange = {
                                            inputs[index] = it.filter { c -> c.isDigit() }.take(3)
                                            ok[index] = null
                                            if (inputs[index].length < expectedLength) return@OutlinedTextField
                                            val v = inputs[index].toIntOrNull()
                                            if (v == expected) {
                                                ok[index] = true
                                                if (soundEnabled) fx.correct()
                                                val allDone = blanks.all { blankIndex ->
                                                    ok[blankIndex - 1] == true
                                                }
                                                if (allDone) {
                                                    correctCount += 1
                                                    msg = "✅ Tabellina completata!"
                                                    resetRound()
                                                }
                                            } else {
                                                ok[index] = false
                                                inputs[index] = ""
                                                if (soundEnabled) fx.wrong()
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .width(boxW)
                                            .height(boxH),
                                        textStyle = TextStyle(
                                            fontSize = fontSize,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = when (ok[index]) {
                                                true -> Color(0xFF22C55E)
                                                false -> Color(0xFFEF4444)
                                                null -> MaterialTheme.colorScheme.primary
                                            },
                                            unfocusedBorderColor = Color.Gray
                                        )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .width(boxW)
                                            .height(boxH)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE5E7EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            expected.toString(),
                                            fontSize = fontSize,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Button(
                    onClick = {
                        resetRound()
                        msg = null
                    },
                    modifier = Modifier.fillMaxWidth().height(actionHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) {
                    Text("RESET", fontWeight = FontWeight.Black)
                }
            }
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
    }
}

@Composable
fun TabellinaReverseGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    data class ReverseQuestion(val a: Int, val b: Int)

    val rng = remember { Random(System.currentTimeMillis()) }
    var question by remember {
        mutableStateOf(
            ReverseQuestion(
                a = rng.nextInt(1, 11),
                b = rng.nextInt(1, 11)
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }

    fun newQuestion() {
        question = ReverseQuestion(
            a = rng.nextInt(1, 11),
            b = rng.nextInt(1, 11)
        )
        input = ""
    }

    val product = question.a * question.b
    val prompt = "__ × ${question.b} = $product"

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val questionSize = if (ui.isCompact) 26.sp else 32.sp
        val inputFontSize = if (ui.isCompact) 18.sp else 22.sp
        val inputWidth = if (ui.isCompact) 140.dp else 170.dp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Tabellina al contrario",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Inserisci il numero mancante per completare l’operazione.",
            ui = ui,
            message = msg,
            content = {
                SeaGlassPanel(title = "Completa l’operazione") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ui.spacing)
                    ) {
                        Text(
                            prompt,
                            fontSize = questionSize,
                            fontWeight = FontWeight.ExtraBold
                        )

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.filter { c -> c.isDigit() }.take(2) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = inputFontSize,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(inputWidth)
                        )
                    }
                }

                Button(
                    onClick = {
                        val user = input.toIntOrNull()
                        if (user == question.a) {
                            msg = "✅ Corretto!"
                            correctCount += 1
                            if (soundEnabled) fx.correct()
                            newQuestion()
                        } else {
                            msg = "❌ Riprova"
                            if (soundEnabled) fx.wrong()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(actionHeight),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) { Text("Controlla") }
            }
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
    }
}

@Composable
fun TabellineMultipleChoiceGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var a by remember { mutableStateOf(rng.nextInt(1, 11)) }
    var b by remember { mutableStateOf(rng.nextInt(1, 11)) }
    var options by remember { mutableStateOf(listOf<Int>()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }

    fun newQuestion() {
        a = rng.nextInt(1, 11)
        b = rng.nextInt(1, 11)
        val correct = a * b
        val optionSet = mutableSetOf(correct)
        while (optionSet.size < 3) {
            optionSet.add(rng.nextInt(2, 101))
        }
        options = optionSet.shuffled(rng)
    }

    LaunchedEffect(Unit) { newQuestion() }

    val correctResult = a * b

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val questionSize = if (ui.isCompact) 30.sp else 36.sp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Scelta multipla",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Scegli il risultato corretto.",
            ui = ui,
            message = msg,
            content = {
                SeaGlassPanel(title = "Quanto fa?") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ui.spacing)
                    ) {
                        Text(
                            "$a × $b",
                            fontSize = questionSize,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            options.forEach { option ->
                                Button(
                                    onClick = {
                                        if (option == correctResult) {
                                            msg = "✅ Corretto!"
                                            correctCount += 1
                                            if (soundEnabled) fx.correct()
                                            newQuestion()
                                        } else {
                                            msg = "❌ Riprova"
                                            if (soundEnabled) fx.wrong()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(actionHeight),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0EA5E9),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(option.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
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
    }
}
