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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.Spacer


// --------------------------------------------------
// ADDITION GAME
// --------------------------------------------------
@Composable
fun AdditionGame(
    digits: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    BasicColumnGame(
        title = "Addizioni",
        digits = digits,
        soundEnabled = soundEnabled,
        onToggleSound = onToggleSound,
        fx = fx,
        onBack = onBack,
        onOpenLeaderboard = onOpenLeaderboard,
        onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
        generator = { a, b -> a + b }
    )
}

// --------------------------------------------------
// SUBTRACTION GAME
// --------------------------------------------------
@Composable
fun SubtractionGame(
    digits: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    BasicColumnGame(
        title = "Sottrazioni",
        digits = digits,
        soundEnabled = soundEnabled,
        onToggleSound = onToggleSound,
        fx = fx,
        onBack = onBack,
        onOpenLeaderboard = onOpenLeaderboard,
        onOpenLeaderboardFromBonus = onOpenLeaderboardFromBonus,
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
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
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
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    val inputGuard = remember { StepInputGuard() }

    val correct = generator(a, b)
    val minValue = 10.0.pow(digits - 1).toInt()
    val maxValue = 10.0.pow(digits).toInt() - 1
    val expectedRange = if (title == "Addizioni") {
        (minValue + minValue)..(maxValue + maxValue)
    } else {
        (minValue - maxValue)..(maxValue - minValue)
    }

    fun next() {
        a = randomNumber()
        b = randomNumber()
        input = ""
        msg = null
        waitTap = false
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    LaunchedEffect(Unit) {
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val questionSize = if (ui.isCompact) 30.sp else 36.sp
        val inputFontSize = if (ui.isCompact) 18.sp else 22.sp
        val inputWidth = if (ui.isCompact) 150.dp else 180.dp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = title,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            ui = ui,
            message = msg,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                    SeaGlassPanel(title = "Quanto fa?") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(ui.spacing)
                        ) {
                            Text(
                                "$a ${if (title == "Addizioni") "+" else "-"} $b",
                                fontSize = questionSize,
                                fontWeight = FontWeight.ExtraBold
                            )

                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it.filter { c -> c.isDigit() || c == '-' }.take(5) },
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
                            val stepId = "basic-${a}-${b}"
                            val validation = validateUserInput(
                                stepId = stepId,
                                value = input,
                                expectedRange = expectedRange,
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
                                    return@Button
                                }
                                msg = "Inserisci un numero valido."
                                return@Button
                            }

                            val user = input.toIntOrNull()
                            gameState = GameState.VALIDATING
                            if (user == correct) {
                                val hitBonus = (correctCount + 1) % BONUS_TARGET == 0
                                correctCount += 1
                                msg = if (hitBonus) "🎉 Bonus sbloccato!" else "✅ Corretto! Tappa per continuare"
                                if (soundEnabled) fx.correct()
                                waitTap = !hitBonus
                                gameState = GameState.STEP_COMPLETED
                            } else {
                                val locked = inputGuard.registerAttempt(stepId)
                                msg = "❌ Riprova"
                                if (soundEnabled) fx.wrong()
                                gameState = GameState.AWAITING_INPUT
                                if (locked) {
                                    msg = "Facciamo un'altra operazione insieme."
                                    waitTap = true
                                    gameState = GameState.STEP_COMPLETED
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(actionHeight),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Controlla") }
                }
            }
        )

        BonusRewardHost(
            correctCount = correctCount,
            rewardsEarned = rewardsEarned,
            soundEnabled = soundEnabled,
            fx = fx,
            onOpenLeaderboard = onOpenLeaderboardFromBonus,
            onRewardEarned = {
                rewardsEarned += 1
                waitTap = true
            },
            onRewardSkipped = {
                rewardsEarned += 1
                waitTap = true
            }
        )
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
    startMode: StartMode = StartMode.RANDOM,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    exercise: ExerciseInstance? = null,
    onExerciseFinished: ((ExerciseResultPartial) -> Unit)? = null,
    helps: HelpSettings? = null,
    bonusLabelOverride: String? = null,
    bonusProgressOverride: Float? = null,
    exerciseKey: Int? = null
) {
    val isHomeworkMode = exercise != null || onExerciseFinished != null
    val resolvedTable = exercise?.table ?: table
    var step by remember { mutableStateOf(1) }
    val inputs = remember { mutableStateListOf<String>().apply { repeat(10) { add("") } } }
    val ok = remember { mutableStateListOf<Boolean?>().apply { repeat(10) { add(null) } } }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    val wrongAnswers = remember { mutableStateListOf<String>() }
    var completed by remember { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    var showWarningDialog by remember { mutableStateOf(false) }
    var warningMessage by remember { mutableStateOf("") }
    val inputGuard = remember { StepInputGuard() }
    val focusRequesters = remember { List(10) { FocusRequester() } }
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }

    fun reset() {
        step = 1
        for (i in 0 until 10) {
            inputs[i] = ""
            ok[i] = null
        }
        attempts = 0
        wrongAnswers.clear()
        completed = false
        msg = null
        showWarningDialog = false
        warningMessage = ""
        pendingFocusIndex = null
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    LaunchedEffect(resolvedTable, exerciseKey) {
        reset()
    }

    LaunchedEffect(step, completed) {
        if (!completed && step in 1..10) {
            focusRequesters[step - 1].requestFocus()
        }
    }

    LaunchedEffect(pendingFocusIndex, completed) {
        val target = pendingFocusIndex
        if (!completed && target != null) {
            withFrameNanos { }
            focusRequesters[target].requestFocus()
            pendingFocusIndex = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val boxW = if (ui.isCompact) ui.cellSmall else ui.cell
        val boxH = if (ui.isCompact) ui.cellSmall else ui.cell
        val fontSize = if (ui.isCompact) 16.sp else 18.sp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Tabellina del $resolvedTable",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = if (isHomeworkMode) 0 else correctCount,
            ui = ui,
            bonusLabelOverride = bonusLabelOverride,
            bonusProgressOverride = bonusProgressOverride,
            message = msg,
            content = {
                SeaGlassPanel(title = "Completa la tabellina") {
                    Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                        for (i in 1..10) {
                            val index = i - 1
                            val active = (i == step)
                            val expected = resolvedTable * i
                            val expectedLength = expected.toString().length
                            val focusRequester = focusRequesters[index]

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ui.spacing)
                            ) {
                                Text(
                                    "$resolvedTable × $i",
                                    modifier = Modifier.width(if (ui.isCompact) 70.dp else 80.dp),
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = inputs[index],
                                    onValueChange = {
                                        if (completed) return@OutlinedTextField
                                        if (!active) return@OutlinedTextField
                                        inputs[index] = it.filter { c -> c.isDigit() }.take(3)
                                        ok[index] = null
                                        if (inputs[index].length < expectedLength) return@OutlinedTextField
                                        val stepId = "table-$resolvedTable-$i"
                                        val validation = validateUserInput(
                                            stepId = stepId,
                                            value = inputs[index],
                                            expectedRange = 0..(resolvedTable * 10),
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
                                                return@OutlinedTextField
                                            }
                                            msg = "Inserisci un numero valido."
                                            return@OutlinedTextField
                                        }
                                        val v = inputs[index].toIntOrNull()
                                        attempts += 1
                                        if (v == expected) {
                                            ok[index] = true
                                            if (soundEnabled) fx.correct()
                                            correctCount += 1
                                            step++
                                            gameState = if (step > 10) GameState.GAME_COMPLETED else GameState.AWAITING_INPUT
                                            inputGuard.reset()
                                            if (step > 10 && isHomeworkMode) {
                                                completed = true
                                                msg = "✅ Tabellina completata!"
                                            }
                                        } else {
                                            ok[index] = false
                                            inputs[index] = ""
                                            if (soundEnabled) fx.wrong()
                                            if (it.isNotBlank()) {
                                                wrongAnswers += it
                                            }
                                            val locked = inputGuard.registerAttempt(stepId)
                                            if (locked) {
                                                warningMessage = if (helps?.hintsEnabled == false) {
                                                    "Attenzione: prova ancora con calma su questa casella."
                                                } else {
                                                    "Attenzione: al terzo errore fermiamoci un attimo e riproviamo insieme questa casella."
                                                }
                                                showWarningDialog = true
                                                inputGuard.reset(stepId)
                                                gameState = GameState.AWAITING_INPUT
                                                pendingFocusIndex = index
                                            }
                                        }
                                    },
                                    enabled = active,
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(boxW)
                                        .height(boxH)
                                        .focusRequester(focusRequester),
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
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (isHomeworkMode) {
                    Button(
                        onClick = {
                            if (completed) {
                                onExerciseFinished?.invoke(
                                    ExerciseResultPartial(
                                        correct = true,
                                        attempts = attempts,
                                        wrongAnswers = wrongAnswers.toList(),
                                        solutionUsed = false
                                    )
                                )
                            }
                        },
                        enabled = completed,
                        modifier = Modifier.fillMaxWidth().height(actionHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Avanti", fontWeight = FontWeight.Black)
                    }
                } else {
                    Button(
                        onClick = { reset() },
                        modifier = Modifier.fillMaxWidth().height(actionHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("RESET", fontWeight = FontWeight.Black)
                    }
                }
            }
        )

        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = {
                    showWarningDialog = false
                    pendingFocusIndex = (step - 1).coerceIn(0, 9)
                },
                title = { Text("Attenzione") },
                text = { Text(warningMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWarningDialog = false
                            pendingFocusIndex = (step - 1).coerceIn(0, 9)
                        }
                    ) {
                        Text("Ho capito")
                    }
                }
            )
        }

        if (!isHomeworkMode) {
            BonusRewardHost(
                correctCount = correctCount,
                rewardsEarned = rewardsEarned,
                rewardEvery = 10,
                soundEnabled = soundEnabled,
                fx = fx,
                onOpenLeaderboard = onOpenLeaderboardFromBonus,
                onRewardEarned = { rewardsEarned += 1 },
                onRewardSkipped = { rewardsEarned += 1 }
            )
        }
    }
}

// --------------------------------------------------
// DIVISION GAME
// --------------------------------------------------
@Composable
fun DivisionGame(
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
    var gameState by remember { mutableStateOf(GameState.AWAITING_INPUT) }
    val inputGuard = remember { StepInputGuard() }

    LaunchedEffect(Unit) {
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp
        val titleSize = if (ui.isCompact) 28.sp else 32.sp

        Column(
            Modifier.fillMaxSize().padding(ui.pad),
            verticalArrangement = Arrangement.spacedBy(ui.spacing)
        ) {
            GameHeader("Divisioni", soundEnabled, onToggleSound, onBack, onOpenLeaderboard, ui = ui)

            SeaGlassPanel(title = "Dividi") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${q.first} ÷ ${q.second}", fontSize = titleSize, fontWeight = FontWeight.ExtraBold)

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(ui.spacing))

            Button(
                onClick = {
                    val stepId = "division-${q.first}-${q.second}"
                    val validation = validateUserInput(
                        stepId = stepId,
                        value = input,
                        expectedRange = 0..100,
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
                            return@Button
                        }
                        msg = "Inserisci un numero valido."
                        return@Button
                    }
                    val u = input.toIntOrNull()
                    gameState = GameState.VALIDATING
                    if (u == q.first / q.second) {
                        msg = "✅ Corretto"
                        if (soundEnabled) fx.correct()
                        q = newQ()
                        input = ""
                        inputGuard.reset()
                        gameState = GameState.AWAITING_INPUT
                    } else {
                        val locked = inputGuard.registerAttempt(stepId)
                        msg = "❌ Riprova"
                        if (soundEnabled) fx.wrong()
                        gameState = GameState.AWAITING_INPUT
                        if (locked) {
                            msg = "Proviamo con un'altra divisione."
                            q = newQ()
                            input = ""
                            inputGuard.reset()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(actionHeight)
            ) { Text("Controlla") }

            if (!msg.isNullOrBlank()) Text(msg!!, fontWeight = FontWeight.Bold)

            Spacer(Modifier.weight(1f))
        }
    }
}

// --------------------------------------------------
// MONEY GAME (semplice)
// --------------------------------------------------
@Composable
fun MoneyGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var a by remember { mutableStateOf(rng.nextInt(1, 10)) }
    var b by remember { mutableStateOf(rng.nextInt(1, 10)) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf(GameState.AWAITING_INPUT) }
    val inputGuard = remember { StepInputGuard() }

    LaunchedEffect(Unit) {
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    Box(Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val titleSize = if (ui.isCompact) 24.sp else 28.sp
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Soldi",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            ui = ui,
            message = msg,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                    SeaGlassPanel(title = "Quanto fa?") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("€ $a + € $b", fontSize = titleSize, fontWeight = FontWeight.ExtraBold)

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
                            val stepId = "money-${a}-${b}"
                            val validation = validateUserInput(
                                stepId = stepId,
                                value = input,
                                expectedRange = 0..200,
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
                                    return@Button
                                }
                                msg = "Inserisci un numero valido."
                                return@Button
                            }
                            val u = input.toIntOrNull()
                            gameState = GameState.VALIDATING
                            if (u == a + b) {
                                msg = "✅ Corretto"
                                correctCount += 1
                                if (soundEnabled) fx.correct()
                                a = rng.nextInt(1, 10)
                                b = rng.nextInt(1, 10)
                                input = ""
                                inputGuard.reset()
                                gameState = GameState.AWAITING_INPUT
                            } else {
                                val locked = inputGuard.registerAttempt(stepId)
                                msg = "❌ Riprova"
                                if (soundEnabled) fx.wrong()
                                gameState = GameState.AWAITING_INPUT
                                if (locked) {
                                    msg = "Passiamo alla prossima somma."
                                    a = rng.nextInt(1, 10)
                                    b = rng.nextInt(1, 10)
                                    input = ""
                                    inputGuard.reset()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(actionHeight)
                    ) { Text("Controlla") }
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
