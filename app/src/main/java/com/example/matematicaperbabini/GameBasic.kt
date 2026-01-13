package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.pow


// --------------------------------------------------
// ADDITION GAME
// --------------------------------------------------
@Composable
fun AdditionGame(
    digits: Int,
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    BasicColumnGame(
        title = "Addizioni",
        digits = digits,
        soundEnabled = soundEnabled,
        onToggleSound = onToggleSound,
        fx = fx,
        onBack = onBack,
        onOpenLeaderboard = onOpenLeaderboard,
        generator = { a, b -> a + b }
    )
}

// --------------------------------------------------
// SUBTRACTION GAME
// --------------------------------------------------
@Composable
fun SubtractionGame(
    digits: Int,
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    BasicColumnGame(
        title = "Sottrazioni",
        digits = digits,
        soundEnabled = soundEnabled,
        onToggleSound = onToggleSound,
        fx = fx,
        onBack = onBack,
        onOpenLeaderboard = onOpenLeaderboard,
        generator = { a, b -> a - b }
    )
}

// --------------------------------------------------
// BASIC ADD/SUB ENGINE (semplificato)
// --------------------------------------------------
@Composable
private fun BasicColumnGame(
    title: String,
    digits: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    generator: (Int, Int) -> Int
) {
    val rng = remember { Random(System.currentTimeMillis()) }

    fun randomNumber() =
        (10.0.pow(digits - 1).toInt() until 10.0.pow(digits).toInt()).random(rng)

    var a by remember { mutableStateOf(randomNumber()) }
    var b by remember { mutableStateOf(randomNumber()) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var waitTap by remember { mutableStateOf(false) }

    val correct = generator(a, b)

    fun next() {
        a = randomNumber()
        b = randomNumber()
        input = ""
        msg = null
        waitTap = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GameHeader(title, soundEnabled, onToggleSound, onBack, onOpenLeaderboard)

        SeaGlassPanel(title = "Quanto fa?") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$a ${if (title == "Addizioni") "+" else "-"} $b", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() || c == '-' }.take(5) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(180.dp)
                )
            }
        }

        Button(
            onClick = {
                val user = input.toIntOrNull()
                if (user == correct) {
                    msg = "✅ Corretto! Tappa per continuare"
                    if (soundEnabled) fx.correct()
                    waitTap = true
                } else {
                    msg = "❌ Riprova"
                    if (soundEnabled) fx.wrong()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Controlla") }

        if (!msg.isNullOrBlank()) {
            Text(msg!!, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.weight(1f))
    }

    if (waitTap) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).clickable { next() },
            contentAlignment = Alignment.Center
        ) {
            SeaGlassPanel(title = "Bravo!") {
                Text("Tappa per continuare", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// --------------------------------------------------
// MULTIPLICATION TABLE GAME (10 CASELLE)
// --------------------------------------------------
@Composable
fun MultiplicationTableGame(
    table: Int,
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val screenH = LocalConfiguration.current.screenHeightDp
    val scale = (screenH / 820f).coerceIn(0.7f, 1f)

    val boxW = (64 * scale).dp   // 🔽 50% più piccoli
    val boxH = (52 * scale).dp

    var step by remember { mutableStateOf(1) }
    val inputs = remember { mutableStateListOf<String>().apply { repeat(10) { add("") } } }
    val ok = remember { mutableStateListOf<Boolean?>().apply { repeat(10) { add(null) } } }

    fun reset() {
        step = 1
        for (i in 0 until 10) {
            inputs[i] = ""
            ok[i] = null
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GameHeader("Tabellina del $table", soundEnabled, onToggleSound, onBack, onOpenLeaderboard)

        SeaGlassPanel(title = "Completa la tabellina") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..10) {
                    val index = i - 1
                    val active = (i == step)

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "$table × $i",
                            modifier = Modifier.width(80.dp),
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = inputs[index],
                            onValueChange = {
                                if (!active) return@OutlinedTextField
                                inputs[index] = it.filter { c -> c.isDigit() }.take(3)
                                val v = inputs[index].toIntOrNull()
                                if (v == table * i) {
                                    ok[index] = true
                                    if (soundEnabled) fx.correct()
                                    step++
                                } else {
                                    ok[index] = false
                                    inputs[index] = ""
                                    if (soundEnabled) fx.wrong()
                                }
                            },
                            enabled = active,
                            singleLine = true,
                            modifier = Modifier
                                .width(boxW)
                                .height(boxH),
                            textStyle = TextStyle(
                                fontSize = (18 * scale).sp,
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
                    }
                }
            }
        }

        Button(
            onClick = { reset() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("RESET", fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.weight(1f))
    }
}

// --------------------------------------------------
// DIVISION GAME
// --------------------------------------------------
@Composable
fun DivisionGame(
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }

    fun newQ(): Pair<Int, Int> {
        val d = rng.nextInt(2, 10)
        val q = rng.nextInt(1, 11)
        return Pair(d * q, d)
    }

    var q by remember { mutableStateOf(newQ()) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GameHeader("Divisioni", soundEnabled, onToggleSound, onBack, onOpenLeaderboard)

        SeaGlassPanel(title = "Dividi") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${q.first} ÷ ${q.second}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = {
                val u = input.toIntOrNull()
                if (u == q.first / q.second) {
                    msg = "✅ Corretto"
                    if (soundEnabled) fx.correct()
                    q = newQ()
                    input = ""
                } else {
                    msg = "❌ Riprova"
                    if (soundEnabled) fx.wrong()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Controlla") }

        if (!msg.isNullOrBlank()) Text(msg!!, fontWeight = FontWeight.Bold)

        Spacer(Modifier.weight(1f))
    }
}

// --------------------------------------------------
// MONEY GAME (semplice)
// --------------------------------------------------
@Composable
fun MoneyGame(
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var a by remember { mutableStateOf(rng.nextInt(1, 10)) }
    var b by remember { mutableStateOf(rng.nextInt(1, 10)) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GameHeader("Soldi", soundEnabled, onToggleSound, onBack, onOpenLeaderboard)

        SeaGlassPanel(title = "Quanto fa?") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("€ $a + € $b", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = {
                val u = input.toIntOrNull()
                if (u == a + b) {
                    msg = "✅ Corretto"
                    if (soundEnabled) fx.correct()
                    a = rng.nextInt(1, 10)
                    b = rng.nextInt(1, 10)
                    input = ""
                } else {
                    msg = "❌ Riprova"
                    if (soundEnabled) fx.wrong()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Controlla") }

        if (!msg.isNullOrBlank()) Text(msg!!, fontWeight = FontWeight.Bold)

        Spacer(Modifier.weight(1f))
    }
}
