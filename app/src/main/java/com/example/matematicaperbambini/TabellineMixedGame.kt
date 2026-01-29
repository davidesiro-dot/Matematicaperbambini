package com.example.matematicaperbambini

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun TabellineMixedGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit,
    exercise: ExerciseInstance? = null,
    onExerciseFinished: ((ExerciseResultPartial) -> Unit)? = null
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    val isHomeworkMode = exercise != null || onExerciseFinished != null
    var a by remember { mutableStateOf(0) }
    var b by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var waitTap by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    val wrongAnswers = remember { mutableStateListOf<String>() }
    var completed by remember { mutableStateOf(false) }

    fun randomNumber() = rng.nextInt(1, 11)

    fun loadExercise() {
        a = exercise?.a ?: randomNumber()
        b = exercise?.b ?: randomNumber()
        input = ""
        msg = null
        waitTap = false
        attempts = 0
        wrongAnswers.clear()
        completed = false
    }

    LaunchedEffect(exercise?.a, exercise?.b) {
        loadExercise()
    }

    val correct = a * b

    fun next() {
        if (exercise == null) {
            loadExercise()
        }
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
            correctCount = if (isHomeworkMode) 0 else correctCount,
            hintText = "Scrivi il risultato e premi Controlla.",
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
                                "$a × $b",
                                fontSize = questionSize,
                                fontWeight = FontWeight.ExtraBold
                            )

                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it.filter { c -> c.isDigit() }.take(5) },
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
                            if (completed) return@Button
                            attempts += 1
                            val user = input.toIntOrNull()
                            if (user == correct) {
                                if (soundEnabled) fx.correct()
                                if (isHomeworkMode) {
                                    completed = true
                                    msg = "✅ Corretto!"
                                    onExerciseFinished?.invoke(
                                        ExerciseResultPartial(
                                            correct = true,
                                            attempts = attempts,
                                            wrongAnswers = wrongAnswers.toList()
                                        )
                                    )
                                } else {
                                    val hitBonus = (correctCount + 1) % BONUS_TARGET == 0
                                    correctCount += 1
                                    msg = if (hitBonus) "🎉 Bonus sbloccato!" else "✅ Corretto! Tappa per continuare"
                                    waitTap = !hitBonus
                                }
                            } else {
                                if (soundEnabled) fx.wrong()
                                msg = "❌ Riprova"
                                if (input.isNotBlank()) {
                                    wrongAnswers += input
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(actionHeight)
                    ) { Text("Controlla") }
                }
            }
        )

        if (!isHomeworkMode) {
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
    }

    if (waitTap && !isHomeworkMode) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable { next() },
            contentAlignment = Alignment.Center
        ) {
            SeaGlassPanel(title = "Bravo!") {
                Text("Tappa per continuare", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
