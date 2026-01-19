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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun MoneyCountGame(
    boardId: String,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    fx: SoundFx,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit
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
        GameScreenFrame(
            title = "Conta i soldi",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Somma il valore delle monete e delle banconote e scrivi il totale in euro.",
            message = message,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeaGlassPanel(title = "Modalità") {
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

                    SeaGlassPanel(title = "Conta le immagini") {
                        val columns = remember(items.size) { if (items.size <= 4) 2 else 3 }
                        MoneyItemsGrid(
                            items = items,
                            columns = columns,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        )
                    }

                    SeaGlassPanel(title = "Scrivi il totale") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { value ->
                                    input = value.filter { it.isDigit() || it == ',' || it == '.' }
                                },
                                placeholder = { Text("Es. 3,50") },

                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val parsed = parseInputToCents(input)
                                        if (parsed == null) {
                                            message = "Scrivi un importo valido (esempio ${formatEuro(350)})"
                                            if (soundEnabled) fx.wrong()
                                            return@Button
                                        }

                                        if (parsed == expectedTotalCents) {
                                            correctCount += 1
                                            message = "Bravo! Totale ${formatEuro(expectedTotalCents)}"
                                            if (soundEnabled) fx.correct()
                                            generateRound(clearMessage = false)
                                        } else {
                                            wrongAttempts += 1
                                            message = "Riprova. Hai scritto ${formatEuro(parsed)}"
                                            if (soundEnabled) fx.wrong()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp)
                                ) {
                                    Text("Verifica")
                                }

                                OutlinedButton(
                                    onClick = {
                                        generateRound(clearMessage = true)
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp)
                                ) {
                                    Text("Nuovo")
                                }
                            }

                            if (wrongAttempts >= 2 && !revealSolution) {
                                OutlinedButton(
                                    onClick = { revealSolution = true },
                                    modifier = Modifier.fillMaxWidth()
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

@Composable
private fun MoneyItemsGrid(
    items: List<MoneyItem>,
    columns: Int,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        MoneyItemImage(item)
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
private fun MoneyItemImage(item: MoneyItem) {
    val painter = remember(item.drawableRes) {
        runCatching { item.drawableRes }.getOrNull()
    }

    // painterResource è composable: la chiamiamo fuori da try/catch
    val resolvedPainter = remember(item.drawableRes) {
        // solo per trigger di remember; la risoluzione vera la facciamo sotto
        0
    }

    // Tentativo “safe”: se la risorsa non esiste, Android lancia prima ancora di disegnare.
    // Quindi invece facciamo un check usando getIdentifier.
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    val pkg = androidx.compose.ui.platform.LocalContext.current.packageName
    val name = res.getResourceEntryName(item.drawableRes) // se item.drawableRes è valido
    val id = res.getIdentifier(name, "drawable", pkg)

    if (id != 0) {
        Image(
            painter = painterResource(id = id),
            contentDescription = item.label,
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp)
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

