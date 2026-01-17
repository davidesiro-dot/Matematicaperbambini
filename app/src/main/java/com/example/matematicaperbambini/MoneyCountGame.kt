package com.example.matematicaperbambini

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.random.Random

data class MoneyItem(
    val id: String,
    val cents: Int,
    val drawableRes: Int,
    val label: String
)

val MONEY_ITEMS = listOf(
    MoneyItem(id = "1c", cents = 1, drawableRes = R.drawable.`1centesimo`, label = "1 centesimo"),
    MoneyItem(id = "2c", cents = 2, drawableRes = R.drawable.`2centesimi`, label = "2 centesimi"),
    MoneyItem(id = "5c", cents = 5, drawableRes = R.drawable.`5centesimi`, label = "5 centesimi"),
    MoneyItem(id = "10c", cents = 10, drawableRes = R.drawable.`10centesimi`, label = "10 centesimi"),
    MoneyItem(id = "20c", cents = 20, drawableRes = R.drawable.`20centesimi`, label = "20 centesimi"),
    MoneyItem(id = "50c", cents = 50, drawableRes = R.drawable.`50centesimi`, label = "50 centesimi"),
    MoneyItem(id = "1e", cents = 100, drawableRes = R.drawable.`1euro`, label = "1 euro"),
    MoneyItem(id = "2e", cents = 200, drawableRes = R.drawable.`2euro`, label = "2 euro"),
    MoneyItem(id = "5e", cents = 500, drawableRes = R.drawable.`5euro`, label = "5 euro"),
    MoneyItem(id = "10e", cents = 1000, drawableRes = R.drawable.`10euro`, label = "10 euro"),
    MoneyItem(id = "20e", cents = 2000, drawableRes = R.drawable.`20euro`, label = "20 euro"),
    MoneyItem(id = "50e", cents = 5000, drawableRes = R.drawable.`50euro`, label = "50 euro")
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
        return "€${euros},${leftover.toString().padStart(2, '0')}"
    }

    fun parseInputToCents(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace(',', '.')
        val parts = normalized.split('.')
        if (parts.size > 2) return null
        val eurosPart = parts[0].ifEmpty { "0" }
        if (!eurosPart.all { it.isDigit() }) return null
        val euros = eurosPart.toIntOrNull() ?: return null
        val cents = if (parts.size == 2) {
            val raw = parts[1]
            if (!raw.all { it.isDigit() }) return null
            when (raw.length) {
                0 -> 0
                1 -> (raw + "0").toInt()
                else -> raw.take(2).toInt()
            }
        } else 0
        return euros * 100 + cents
    }

    fun pickItem(banknoteChance: Float): MoneyItem {
        return if (coinsOnly || BANKNOTE_ITEMS.isEmpty()) {
            COIN_ITEMS[rng.nextInt(COIN_ITEMS.size)]
        } else {
            val pickBanknote = rng.nextFloat() < banknoteChance
            val pool = if (pickBanknote) BANKNOTE_ITEMS else COIN_ITEMS
            pool[rng.nextInt(pool.size)]
        }
    }

    fun generateRound(clearMessage: Boolean) {
        val stage = correctCount / 5
        val minItems = 2
        val maxItems = min(5 + stage, 7)
        val banknoteChance = (0.2f + 0.1f * stage).coerceAtMost(0.6f)
        var chosenItems: List<MoneyItem> = emptyList()
        var total = 0

        repeat(200) {
            val count = rng.nextInt(minItems, maxItems + 1)
            val generated = List(count) { pickItem(banknoteChance) }
            val sum = generated.sumOf { it.cents }
            if (sum <= 10000) {
                chosenItems = generated
                total = sum
                return@repeat
            }
        }

        if (chosenItems.isEmpty()) {
            chosenItems = List(minItems) { COIN_ITEMS[rng.nextInt(COIN_ITEMS.size)] }
            total = chosenItems.sumOf { it.cents }.coerceAtMost(10000)
        }

        items = chosenItems
        expectedTotalCents = total
        input = ""
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
                        val columns = if (items.size <= 4) 2 else 3
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            userScrollEnabled = false
                        ) {
                            items(items) { item ->
                                Image(
                                    painter = painterResource(id = item.drawableRes),
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .padding(4.dp)
                                )
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
                                    input = value.filter { it.isDigit() || it == ',' || it == '.' }
                                },
                                placeholder = { Text("Es. 3,50") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

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
                                        message = "Riprova. Hai scritto ${formatEuro(parsed)}"
                                        if (soundEnabled) fx.wrong()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp)
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
