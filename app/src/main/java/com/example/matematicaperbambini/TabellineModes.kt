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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    var gameState by remember { mutableStateOf(GameState.AWAITING_INPUT) }
    val inputGuard = remember { StepInputGuard() }

    fun newQuestion() {
        a = rng.nextInt(1, 11)
        b = rng.nextInt(1, 11)
        input = ""
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
            title = "Tabelline miste",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
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
                Spacer(Modifier.height(ui.spacing))
                Button(
                    onClick = {
                        val stepId = "mixed-basic-${a}-${b}"
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
                        val user = input.toIntOrNull()
                        gameState = GameState.VALIDATING
                        if (user == a * b) {
                            msg = "✅ Corretto!"
                            correctCount += 1
                            if (soundEnabled) fx.correct()
                            newQuestion()
                            gameState = GameState.AWAITING_INPUT
                        } else {
                            val locked = inputGuard.registerAttempt(stepId)
                            msg = "❌ Riprova"
                            if (soundEnabled) fx.wrong()
                            gameState = GameState.AWAITING_INPUT
                            if (locked) {
                                msg = "Passiamo alla prossima."
                                newQuestion()
                            }
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
            rewardEvery = 10,
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
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    exercise: ExerciseInstance? = null,
    onExerciseFinished: ((ExerciseResultPartial) -> Unit)? = null,
    helps: HelpSettings? = null,
    bonusLabelOverride: String? = null,
    bonusProgressOverride: Float? = null,
    exerciseKey: Int? = null
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    val isHomeworkMode = exercise != null || onExerciseFinished != null
    val resolvedTable = exercise?.table ?: table
    val inputs = remember { mutableStateListOf<String>().apply { repeat(10) { add("") } } }
    val ok = remember { mutableStateListOf<Boolean?>().apply { repeat(10) { add(null) } } }
    var blanks by remember { mutableStateOf(setOf<Int>()) }
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
    var activeBlankIndex by remember { mutableStateOf<Int?>(null) }

    fun resetRound() {
        blanks = (1..10).shuffled(rng).take(4).toSet()
        for (i in 0 until 10) {
            inputs[i] = ""
            ok[i] = null
        }
        attempts = 0
        wrongAnswers.clear()
        completed = false
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
        activeBlankIndex = (0 until 10).firstOrNull { blanks.contains(it + 1) && inputs[it].isBlank() }
    }

    LaunchedEffect(resolvedTable, exerciseKey) { resetRound() }

    LaunchedEffect(activeBlankIndex, completed) {
        val targetIndex = activeBlankIndex
        if (!completed && targetIndex != null) {
            withFrameNanos { }
            focusRequesters[targetIndex].requestFocus()
        }
    }

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
            correctCount = if (isHomeworkMode) 0 else correctCount,
            ui = ui,
            bonusLabelOverride = bonusLabelOverride,
            bonusProgressOverride = bonusProgressOverride,
            message = msg,
            content = {
                SeaGlassPanel(title = "Tabellina del $resolvedTable") {
                    Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                        for (i in 1..10) {
                            val index = i - 1
                            val expected = resolvedTable * i
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
                                            if (completed) return@OutlinedTextField
                                            inputs[index] = it.filter { c -> c.isDigit() }.take(3)
                                            ok[index] = null
                                            if (inputs[index].length < expectedLength) return@OutlinedTextField
                                            val stepId = "gaps-$resolvedTable-$i"
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
                                                if (ok[index] != true) {
                                                    correctCount += 1
                                                }
                                                ok[index] = true
                                                if (soundEnabled) fx.correct()
                                                gameState = GameState.AWAITING_INPUT
                                                val allDone = blanks.all { blankIndex ->
                                                    ok[blankIndex - 1] == true
                                                }
                                                if (allDone) {
                                                    msg = "✅ Tabellina completata!"
                                                    if (isHomeworkMode) {
                                                        completed = true
                                                        gameState = GameState.GAME_COMPLETED
                                                    } else {
                                                        resetRound()
                                                    }
                                                } else {
                                                    val nextBlank = (index + 1 until 10).firstOrNull {
                                                        blanks.contains(it + 1) && ok[it] != true
                                                    } ?: (0 until index).firstOrNull {
                                                        blanks.contains(it + 1) && ok[it] != true
                                                    }
                                                    if (nextBlank != null) {
                                                        activeBlankIndex = nextBlank
                                                    }
                                                }
                                            } else {
                                                val locked = inputGuard.registerAttempt(stepId)
                                                ok[index] = false
                                                inputs[index] = ""
                                                if (soundEnabled) fx.wrong()
                                                if (it.isNotBlank()) {
                                                    wrongAnswers += it
                                                }
                                                gameState = GameState.AWAITING_INPUT
                                                if (locked) {
                                                    warningMessage = if (helps?.hintsEnabled == false) {
                                                        "Attenzione: prova ancora con calma su questa casella."
                                                    } else {
                                                        "Attenzione: al terzo errore fermiamoci un attimo e riproviamo insieme questa casella."
                                                    }
                                                    showWarningDialog = true
                                                    inputGuard.reset(stepId)
                                                    gameState = GameState.AWAITING_INPUT
                                                    activeBlankIndex = index
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .width(boxW)
                                            .height(boxH)
                                            .focusRequester(focusRequesters[index]),
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    ) {
                        Text("Avanti", fontWeight = FontWeight.Black)
                    }
                } else {
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
            }
        )

        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = {
                    showWarningDialog = false
                    activeBlankIndex = activeBlankIndex ?: (0 until 10).firstOrNull {
                        blanks.contains(it + 1) && ok[it] != true
                    }
                },
                title = { Text("Attenzione") },
                text = { Text(warningMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWarningDialog = false
                            activeBlankIndex = activeBlankIndex ?: (0 until 10).firstOrNull {
                                blanks.contains(it + 1) && ok[it] != true
                            }
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

@Composable
fun TabellinaReverseGame(
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
    data class ReverseQuestion(val a: Int, val b: Int)

    val rng = remember { Random(System.currentTimeMillis()) }
    val isHomeworkMode = exercise != null || onExerciseFinished != null
    var question by remember {
        mutableStateOf(
            ReverseQuestion(
                a = exercise?.a ?: rng.nextInt(1, 11),
                b = exercise?.b ?: exercise?.table ?: rng.nextInt(1, 11)
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    val wrongAnswers = remember { mutableStateListOf<String>() }
    var completed by remember { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    val inputGuard = remember { StepInputGuard() }

    fun newQuestion() {
        question = ReverseQuestion(
            a = rng.nextInt(1, 11),
            b = rng.nextInt(1, 11)
        )
        input = ""
        msg = null
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    LaunchedEffect(Unit) {
        if (!isHomeworkMode) {
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
        }
    }

    LaunchedEffect(exercise?.a, exercise?.b, exercise?.table, exerciseKey) {
        if (isHomeworkMode) {
            question = ReverseQuestion(
                a = exercise?.a ?: rng.nextInt(1, 11),
                b = exercise?.b ?: exercise?.table ?: rng.nextInt(1, 11)
            )
            input = ""
            msg = null
            attempts = 0
            wrongAnswers.clear()
            completed = false
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
        }
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
            correctCount = if (isHomeworkMode) 0 else correctCount,
            ui = ui,
            bonusLabelOverride = bonusLabelOverride,
            bonusProgressOverride = bonusProgressOverride,
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
                Spacer(Modifier.height(ui.spacing))
                Button(
                    onClick = {
                        if (completed) return@Button
                        val stepId = "reverse-${question.a}-${question.b}"
                        val validation = validateUserInput(
                            stepId = stepId,
                            value = input,
                            expectedRange = 1..10,
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
                        attempts += 1
                        val user = input.toIntOrNull()
                        if (user == question.a) {
                            msg = "✅ Corretto!"
                            if (soundEnabled) fx.correct()
                            if (isHomeworkMode) {
                                completed = true
                                gameState = GameState.GAME_COMPLETED
                                onExerciseFinished?.invoke(
                                    ExerciseResultPartial(
                                        correct = true,
                                        attempts = attempts,
                                        wrongAnswers = wrongAnswers.toList(),
                                        solutionUsed = false
                                    )
                                )
                            } else {
                                correctCount += 1
                                newQuestion()
                            }
                        } else {
                            val locked = inputGuard.registerAttempt(stepId)
                            msg = "❌ Riprova"
                            if (soundEnabled) fx.wrong()
                            if (input.isNotBlank()) {
                                wrongAnswers += input
                            }
                            gameState = GameState.AWAITING_INPUT
                            if (locked) {
                                msg = "Passiamo alla prossima domanda."
                                if (isHomeworkMode) {
                                    completed = true
                                    gameState = GameState.GAME_COMPLETED
                                    onExerciseFinished?.invoke(
                                        ExerciseResultPartial(
                                            correct = true,
                                            attempts = attempts,
                                            wrongAnswers = wrongAnswers.toList(),
                                            solutionUsed = false
                                        )
                                    )
                                } else {
                                    newQuestion()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(actionHeight),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) { Text("Controlla") }
            }
        )

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

@Composable
fun TabellineMultipleChoiceGame(
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
    val rng = remember { Random(System.currentTimeMillis()) }
    val isHomeworkMode = exercise != null || onExerciseFinished != null
    var a by remember { mutableStateOf(exercise?.a ?: rng.nextInt(1, 11)) }
    var b by remember { mutableStateOf(exercise?.b ?: rng.nextInt(1, 11)) }
    var options by remember { mutableStateOf(listOf<Int>()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    val wrongAnswers = remember { mutableStateListOf<String>() }
    var completed by remember { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    val inputGuard = remember { StepInputGuard() }

    fun newQuestion() {
        a = rng.nextInt(1, 11)
        b = rng.nextInt(1, 11)
        val correct = a * b
        val optionSet = mutableSetOf(correct)
        while (optionSet.size < 3) {
            optionSet.add(rng.nextInt(2, 101))
        }
        options = optionSet.shuffled(rng)
        msg = null
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
    }

    LaunchedEffect(isHomeworkMode) {
        if (!isHomeworkMode) {
            newQuestion()
        }
    }

    LaunchedEffect(exercise?.a, exercise?.b, exercise?.table, exerciseKey) {
        if (isHomeworkMode) {
            a = exercise?.a ?: rng.nextInt(1, 11)
            b = exercise?.b ?: rng.nextInt(1, 11)
            val correct = a * b
            val optionSet = mutableSetOf(correct)
            while (optionSet.size < 3) {
                optionSet.add(rng.nextInt(2, 101))
            }
            options = optionSet.shuffled(rng)
            msg = null
            attempts = 0
            wrongAnswers.clear()
            completed = false
            gameState = GameState.AWAITING_INPUT
            inputGuard.reset()
        }
    }

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
            correctCount = if (isHomeworkMode) 0 else correctCount,
            ui = ui,
            bonusLabelOverride = bonusLabelOverride,
            bonusProgressOverride = bonusProgressOverride,
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
                                        if (completed) return@Button
                                        val stepId = "choice-${a}-${b}"
                                        val validation = validateUserInput(
                                            stepId = stepId,
                                            value = option.toString(),
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
                                            msg = "Seleziona una risposta valida."
                                            return@Button
                                        }
                                        attempts += 1
                                        if (option == correctResult) {
                                            msg = "✅ Corretto!"
                                            if (soundEnabled) fx.correct()
                                            if (isHomeworkMode) {
                                                completed = true
                                                gameState = GameState.GAME_COMPLETED
                                                onExerciseFinished?.invoke(
                                                    ExerciseResultPartial(
                                                        correct = true,
                                                        attempts = attempts,
                                                        wrongAnswers = wrongAnswers.toList(),
                                                        solutionUsed = false
                                                    )
                                                )
                                            } else {
                                                correctCount += 1
                                                newQuestion()
                                            }
                                        } else {
                                            val locked = inputGuard.registerAttempt(stepId)
                                            msg = "❌ Riprova"
                                            if (soundEnabled) fx.wrong()
                                            wrongAnswers += option.toString()
                                            gameState = GameState.AWAITING_INPUT
                                            if (locked) {
                                                msg = "Passiamo alla prossima."
                                                if (isHomeworkMode) {
                                                    completed = true
                                                    gameState = GameState.GAME_COMPLETED
                                                    onExerciseFinished?.invoke(
                                                        ExerciseResultPartial(
                                                            correct = true,
                                                            attempts = attempts,
                                                            wrongAnswers = wrongAnswers.toList(),
                                                            solutionUsed = false
                                                        )
                                                    )
                                                } else {
                                                    newQuestion()
                                                }
                                            }
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
