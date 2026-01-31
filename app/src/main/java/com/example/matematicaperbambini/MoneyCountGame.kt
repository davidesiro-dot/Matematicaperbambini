package com.example.matematicaperbambini

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction


import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun MoneyCountGame(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLeaderboardFromBonus: (LeaderboardTab) -> Unit
) {
    val rng = remember { Random(System.currentTimeMillis()) }
    var items by remember { mutableStateOf<List<MoneyItem>>(emptyList()) }
    var expectedTotalCents by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var rewardsEarned by remember { mutableStateOf(0) }
    var coinsOnly by remember { mutableStateOf(false) }
    var wrongAttempts by remember { mutableStateOf(0) }
    var revealSolution by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.INIT) }
    val inputGuard = remember { StepInputGuard() }

    fun generateRound(clearMessage: Boolean) {
        val round = MoneyRoundGenerator.generateRound(
            rng = rng,
            coinsOnly = coinsOnly,
            correctCount = correctCount
        )
        items = round.items
        expectedTotalCents = round.totalCents
        input = ""
        wrongAttempts = 0
        revealSolution = false
        showSuccessDialog = false
        gameState = GameState.AWAITING_INPUT
        inputGuard.reset()
        if (clearMessage) {
            message = null
        }
    }

    LaunchedEffect(Unit) {
        generateRound(clearMessage = true)
    }

    LaunchedEffect(coinsOnly) {
        generateRound(clearMessage = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val ui = rememberUiSizing()
        val actionHeight = if (ui.isCompact) 44.dp else 52.dp

        GameScreenFrame(
            title = "Conta i soldi",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Somma il valore delle monete e delle banconote e scrivi il totale in euro.",
            ui = ui,
            message = message,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(ui.spacing)) {
                    SeaGlassPanel(title = "Modalità") {
                        if (ui.isCompact) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (coinsOnly) "Solo monete" else "Monete + banconote",
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = coinsOnly,
                                    onCheckedChange = { coinsOnly = it }
                                )
                            }
                            Text(
                                text = if (coinsOnly) {
                                    "Solo centesimi e euro in moneta."
                                } else {
                                    "Monete e banconote fino a 50€."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (coinsOnly) "Solo monete" else "Monete + banconote",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (coinsOnly) {
                                            "Solo centesimi e euro in moneta."
                                        } else {
                                            "Monete e banconote fino a 50€."
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Switch(
                                    checked = coinsOnly,
                                    onCheckedChange = { coinsOnly = it }
                                )
                            }
                        }
                    }

                    SeaGlassPanel(title = "Conta le immagini") {
                        val columns = remember(items.size) { if (items.size <= 4) 2 else 3 }
                        MoneyItemsGrid(
                            items = items,
                            columns = columns,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (ui.isCompact) 4.dp else 6.dp),
                            ui = ui
                        )
                    }

                    SeaGlassPanel(title = "Scrivi il totale. Es: 3,50") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ui.spacing),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { value ->
                                    input = value.filter { it.isDigit() || it == ',' || it == '.' }
                                },
                                placeholder = { Text("") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ui.spacing)
                            ) {
                                Button(
                                    onClick = {
                                        val parsed = parseInputToCents(input)
                                        val stepId = "money-count-$expectedTotalCents"
                                        val validation = if (parsed == null) {
                                            ValidationResult.invalid(ValidationFailure.NON_NUMERIC)
                                        } else {
                                            validateUserInput(
                                                stepId = stepId,
                                                value = parsed.toString(),
                                                expectedRange = 0..10000,
                                                gameState = gameState,
                                                guard = inputGuard
                                            )
                                        }
                                        if (!validation.isValid) {
                                            if (validation.failure == ValidationFailure.TOO_FAST ||
                                                validation.failure == ValidationFailure.NOT_AWAITING_INPUT
                                            ) {
                                                return@Button
                                            }
                                            if (soundEnabled) fx.wrong()
                                            message = "Scrivi un importo valido (esempio ${formatEuro(350)})"
                                            return@Button
                                        }
                                        if (parsed == null) return@Button

                                        if (parsed == expectedTotalCents) {
                                            correctCount += 1
                                            message = "Bravo! Totale ${formatEuro(expectedTotalCents)}"
                                            if (soundEnabled) fx.correct()
                                            if (!showSuccessDialog) {
                                                showSuccessDialog = true
                                            }
                                            gameState = GameState.STEP_COMPLETED
                                        } else {
                                            wrongAttempts += 1
                                            val locked = inputGuard.registerAttempt(stepId)
                                            message = "Riprova. Hai scritto ${formatEuro(parsed)}"
                                            if (soundEnabled) fx.wrong()
                                            gameState = GameState.AWAITING_INPUT
                                            if (locked) {
                                                message = "Vediamo insieme il totale corretto."
                                                revealSolution = true
                                                gameState = GameState.STEP_COMPLETED
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(actionHeight)
                                ) {
                                    Text("Verifica")
                                }

                                OutlinedButton(
                                    onClick = {
                                        generateRound(clearMessage = true)
                                    },
                                    modifier = Modifier.weight(1f).height(actionHeight)
                                ) {
                                    Text("Nuovo")
                                }
                            }

                            if (wrongAttempts >= 2 && !revealSolution) {
                                OutlinedButton(
                                    onClick = { revealSolution = true },
                                    modifier = Modifier.fillMaxWidth().height(actionHeight)
                                ) {
                                    Text("Mostra soluzione")
                                }
                            }

                            if (revealSolution) {
                                Text(
                                    text = "Totale: ${formatEuro(expectedTotalCents)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        )

        SuccessDialog(
            show = showSuccessDialog,
            onNew = {
                showSuccessDialog = false
                generateRound(clearMessage = true)
            },
            onDismiss = { showSuccessDialog = false },
            resultText = formatEuro(expectedTotalCents)
        )

        BonusRewardHost(
            correctCount = correctCount,
            rewardsEarned = rewardsEarned,
            soundEnabled = soundEnabled,
            fx = fx,
            onOpenLeaderboard = onOpenLeaderboardFromBonus,
            onBonusPromptAction = { showSuccessDialog = false },
            onRewardEarned = { rewardsEarned += 1 },
            onRewardSkipped = { rewardsEarned += 1 }
        )
    }
}

@Composable
private fun MoneyItemsGrid(
    items: List<MoneyItem>,
    columns: Int,
    modifier: Modifier = Modifier,
    ui: UiSizing
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ui.spacing),
        modifier = modifier
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ui.spacing)
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        MoneyItemImage(item, ui)
                    }
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoneyItemImage(item: MoneyItem, ui: UiSizing) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val res = ctx.resources

    // ✅ scala visiva (centesimi più piccoli)
    val scale = moneyVisualScale(item.cents)

    // ✅ controllo "safe" senza crash
    val canDraw = remember(item.drawableRes) {
        runCatching { res.getResourceEntryName(item.drawableRes) }.isSuccess
    }

    val baseSize = if (ui.isCompact) 86.dp else 110.dp
    val pad = if (ui.isCompact) 2.dp else 4.dp

    if (canDraw) {
        Image(
            painter = painterResource(id = item.drawableRes),
            contentDescription = item.label,
            modifier = Modifier
                .size(baseSize)
                .padding(pad)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(baseSize)
                .padding(pad)
                .background(Color.White.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
