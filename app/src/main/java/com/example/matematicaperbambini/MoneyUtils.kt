package com.example.matematicaperbambini

import kotlin.math.min
import kotlin.random.Random

data class MoneyItem(
    val id: String,
    val cents: Int,
    val drawableRes: Int,
    val label: String
)

data class MoneyRound(
    val items: List<MoneyItem>,
    val totalCents: Int
)

val MONEY_ITEMS = listOf(
    MoneyItem(id = "1c",  cents = 1,    drawableRes = R.drawable.coin_1c,  label = "1 centesimo"),
    MoneyItem(id = "2c",  cents = 2,    drawableRes = R.drawable.coin_2c,  label = "2 centesimi"),
    MoneyItem(id = "5c",  cents = 5,    drawableRes = R.drawable.coin_5c,  label = "5 centesimi"),
    MoneyItem(id = "10c", cents = 10,   drawableRes = R.drawable.coin_10c, label = "10 centesimi"),
    MoneyItem(id = "20c", cents = 20,   drawableRes = R.drawable.coin_20c, label = "20 centesimi"),
    MoneyItem(id = "50c", cents = 50,   drawableRes = R.drawable.coin_50c, label = "50 centesimi"),

    MoneyItem(id = "1e",  cents = 100,  drawableRes = R.drawable.coin_1e,  label = "1 euro"),
    MoneyItem(id = "2e",  cents = 200,  drawableRes = R.drawable.coin_2e,  label = "2 euro"),

    MoneyItem(id = "5e",  cents = 500,  drawableRes = R.drawable.note_5e,  label = "5 euro"),
    MoneyItem(id = "10e", cents = 1000, drawableRes = R.drawable.note_10e, label = "10 euro"),
    MoneyItem(id = "20e", cents = 2000, drawableRes = R.drawable.note_20e, label = "20 euro"),
    MoneyItem(id = "50e", cents = 5000, drawableRes = R.drawable.note_50e, label = "50 euro")
)


private val COIN_ITEMS = MONEY_ITEMS.filter { it.cents < 500 }
private val BANKNOTE_ITEMS = MONEY_ITEMS.filter { it.cents >= 500 }

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

fun moneyVisualScale(cents: Int): Float {
    return when {
        cents <= 5 -> 0.68f        // 1c, 2c, 5c
        cents <= 50 -> 0.80f       // 10c, 20c, 50c
        cents == 100 -> 0.92f      // 1€
        cents == 200 -> 1.00f      // 2€
        else -> 1.05f              // banconote
    }
}

object MoneyRoundGenerator {
    fun generateRound(
        rng: Random,
        coinsOnly: Boolean,
        correctCount: Int,
        maxTotalCents: Int = 10000
    ): MoneyRound {
        val stage = correctCount / 5
        val minItems = 2
        val maxItems = min(5 + stage, 7)
        val banknoteChance = (0.2f + 0.1f * stage).coerceAtMost(0.6f)
        var chosenItems: List<MoneyItem> = emptyList()
        var total = 0

        fun pickItem(): MoneyItem {
            return if (coinsOnly || BANKNOTE_ITEMS.isEmpty()) {
                COIN_ITEMS[rng.nextInt(COIN_ITEMS.size)]
            } else {
                val pickBanknote = rng.nextFloat() < banknoteChance
                val pool = if (pickBanknote) BANKNOTE_ITEMS else COIN_ITEMS
                pool[rng.nextInt(pool.size)]
            }
        }

        repeat(200) {
            val count = rng.nextInt(minItems, maxItems + 1)
            val generated = List(count) { pickItem() }
            val sum = generated.sumOf { it.cents }
            if (sum <= maxTotalCents) {
                chosenItems = generated
                total = sum
                return@repeat
            }
        }

        if (chosenItems.isEmpty()) {
            chosenItems = List(minItems) { COIN_ITEMS[rng.nextInt(COIN_ITEMS.size)] }
            total = chosenItems.sumOf { it.cents }.coerceAtMost(maxTotalCents)
        }

        return MoneyRound(items = chosenItems, totalCents = total)
    }
}
