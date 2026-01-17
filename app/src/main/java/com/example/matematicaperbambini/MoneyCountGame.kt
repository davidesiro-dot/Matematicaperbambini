package com.example.matematicaperbambini

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.random.Random

private data class MoneyItem(
    val id: String,
    val cents: Int,
    val drawableRes: Int,
    val label: String
)

private val MONEY_ITEMS: List<MoneyItem> = listOf(
    MoneyItem("1c", 1, R.drawable.coin_1c, "1 cent"),
    MoneyItem("2c", 2, R.drawable.coin_2c, "2 cent"),
    MoneyItem("5c", 5, R.drawable.coin_5c, "5 cent"),
    MoneyItem("10c", 10, R.drawable.coin_10c, "10 cent"),
    MoneyItem("20c", 20, R.drawable.coin_20c, "20 cent"),
    MoneyItem("50c", 50, R.drawable.coin_50c, "50 cent"),
    MoneyItem("1e", 100, R.drawable.coin_1e, "1 euro"),
    MoneyItem("2e", 200, R.drawable.coin_2e, "2 euro"),
    MoneyItem("5e", 500, R.drawable.note_5e, "5 euro"),
    MoneyItem("10e", 1000, R.drawable.note_10e, "10 euro"),
    MoneyItem("20e", 2000, R.drawable.note_20e, "20 euro"),
    MoneyItem("50e", 5000, R.drawable.note_50e, "50 euro")
)

private val COIN_ITEMS = MONEY_ITEMS.filter { it.cents < 500 }
private val BANKNOTE_ITEMS = MONEY_ITEMS.filter { it.cents >= 500 }

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

    fun formatEuro(cents: Int): String {
        val euros = cents / 100
        val leftover = cents % 100
        return "€$euros,${leftover.toString().padStart(2, '0')}"
    }

    fun parseInputToCents(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val normalized = trimmed.replace(',', '.')
        val parts = normalized.split('.')
        if (parts.size > 2) return null

        val eurosPart = parts[0]
        if (eurosPart.isEmpty() || !eurosPart.all { ch -> ch.isDigit() }) return null
        val euros = eurosPart.toIntOrNull() ?: return null

        val cents = if (parts.size == 2) {
            val raw = parts[1]
            if (!raw.all { ch -> ch.isDigit() }) return null
            when (raw.length) {
                0 -> 0
                1 -> (raw + "0").toInt()
                else -> raw.take(2).toInt()
            }
        } else 0

        return euros * 100 + cents
    }

    fun pickItem(banknoteChance: Float): MoneyItem {
        if (coinsOnly || BANKNOTE_ITEMS.isEmpty()) {
            return COIN_ITEMS[rng.nextInt(COIN_ITEMS.size)]
        }
        val pickBanknote = rng.nextFloat() < banknoteChance
        val pool = if (pickBanknote) BANKNOTE_ITEMS else COIN_ITEMS
        return pool[rng.nextInt(pool.size)]
    }

    fun generateRound(clearMessage: Boolean) {
        val stage = correctCount / 5
        val minItems = 2
        val maxItems = min(5 + stage, 7)

        // Probabilità banconote cresce con i successi, ma resta moderata
        val banknoteChance = (0.20f + 0.10f * stage).coerceAtMost(0.60f)

        var chosenItems: List<MoneyItem> = emptyList()
        var total = 0

        // Cerca una combinazione che NON superi 50€ (5000 cent)
        repeat(300) {
            val count = rng.nextInt(minItems, maxItems + 1)
            val generated = List(count) { pickItem(banknoteChance) }
            val sum = generated.sumOf { it.cents }
            if (sum <= 5000) {
                chosenItems = generated
                total = sum
                return@repeat
            }
        }

        // Fallback: solo monete
        if (chosenItems.isEmpty()) {
            chosenItems = List(minItems) { COIN_ITEMS[rng.nextInt(COIN_ITEMS.size)] }
            total = chosenItems.sumOf { it.cents }.coerceAtMost(5000)
        }

        items = chosenItems
        expectedTotalCents = total
        input = ""
        if (clearMessage) message = null
    }

    LaunchedEffect(Unit) { generateRound(clearMessage = true) }
    LaunchedEffect(coinsOnly) { generateRound(clearMessage = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        GameScreenFrame(
            title = "Conta i soldi",
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onBack = onBack,
            onOpenLeaderboard = onOpenLeaderboard,
            correctCount = correctCount,
            hintText = "Somma il valore delle monete e banconote e scrivi il totale in euro.",
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
                                    text = if (coinsOnly) "Solo centesimi e euro in moneta."
                                    else "Monete e banconote fino a 50€.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = coinsOnly,
                                onCheckedChange = { v -> coinsOnly = v }
                            )
                        }
                    }

                    SeaGlassPanel(title = "Conta le immagini") {
                        val columns = if (items.size <= 4) 2 else 3
                        val rows = (items.size + columns - 1) / columns

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    for (c in 0 until columns) {
                                        val idx = r * columns + c
                                        if (idx < items.size) {
                                            val item = items[idx]
                                            Image(
                                                painter = painterResource(id = item.drawableRes),
                                                contentDescription = item.label,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)      // mantiene “quadrato” lo slot
                                                    .padding(4.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
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
                                    input = value.filter { ch ->
                                        ch.isDigit() || ch == ',' || ch == '.'
                                    }
                                },
                                placeholder = { Text("Es. 3,50") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val parsed = parseInputToCents(input)
                                    if (parsed == null) {
                                        message = "Scrivi un importo valido (es. ${formatEuro(350)})"
                                        if (soundEnabled) fx.wrong()
                                        return@Button
                                    }

                                    if (parsed == expectedTotalCents) {
                                        correctCount += 1
                                        message = "Bravo! Totale ${formatEuro(expectedTotalCents)}"
                                        if (soundEnabled) fx.correct()
                                        generateRound(clearMessage = false)
                                    } else {
                                        message = "Riprova. Hai scritto ${formatEuro(parsed)}"
                                        if (soundEnabled) fx.wrong()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Verifica")
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
