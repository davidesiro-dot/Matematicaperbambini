package com.example.matematicaperbambini

import kotlin.math.pow
import kotlin.random.Random

fun buildExerciseQueue(configs: List<HomeworkTaskConfig>): List<HomeworkExerciseEntry> {
    val queue = mutableListOf<HomeworkExerciseEntry>()
    configs.forEach { config ->
        val repeats = config.amount.repeatsPerExercise.coerceAtLeast(1)
        when (val source = config.source) {
            is ExerciseSourceConfig.Random -> {
                val count = config.amount.exercisesCount.coerceAtLeast(1)
                repeat(count) {
                    val instance = generateRandomInstance(config)
                    repeat(repeats) {
                        queue += HomeworkExerciseEntry(instance = instance, helps = config.helps)
                    }
                }
            }
            is ExerciseSourceConfig.Manual -> {
                source.ops.forEach { op ->
                    val instance = when (op) {
                        is ManualOp.AB -> ExerciseInstance(
                            game = config.game,
                            a = op.a,
                            b = op.b
                        )
                        is ManualOp.Table -> ExerciseInstance(
                            game = config.game,
                            a = op.table,
                            b = Random.nextInt(1, 11),
                            table = op.table
                        )
                    }
                    repeat(repeats) {
                        queue += HomeworkExerciseEntry(instance = instance, helps = config.helps)
                    }
                }
            }
        }
    }
    return queue
}

private fun generateRandomInstance(config: HomeworkTaskConfig): ExerciseInstance {
    return when (config.game) {
        GameType.MULTIPLICATION_TABLE,
        GameType.MULTIPLICATION_GAPS -> {
            val table = config.difficulty.tables?.randomOrNull()
                ?: config.difficulty.level
                ?: Random.nextInt(1, 11)
            val factor = Random.nextInt(1, 11)
            ExerciseInstance(game = config.game, a = table, b = factor, table = table)
        }
        GameType.MULTIPLICATION_REVERSE -> {
            val table = config.difficulty.tables?.randomOrNull()
                ?: config.difficulty.level
                ?: Random.nextInt(1, 11)
            val factor = Random.nextInt(1, 11)
            ExerciseInstance(game = config.game, a = factor, b = table, table = table)
        }
        GameType.MULTIPLICATION_MULTIPLE_CHOICE -> {
            val table = config.difficulty.tables?.randomOrNull()
                ?: config.difficulty.level
                ?: Random.nextInt(1, 11)
            val factor = Random.nextInt(1, 11)
            ExerciseInstance(game = config.game, a = table, b = factor, table = table)
        }
        GameType.MULTIPLICATION_MIXED -> {
            val range = digitsRange(config.difficulty.digits)
            ExerciseInstance(
                game = config.game,
                a = Random.nextInt(range.first, range.last + 1),
                b = Random.nextInt(range.first, range.last + 1)
            )
        }
        GameType.ADDITION,
        GameType.SUBTRACTION -> {
            val range = digitsRange(config.difficulty.digits)
            ExerciseInstance(
                game = config.game,
                a = Random.nextInt(range.first, range.last + 1),
                b = Random.nextInt(range.first, range.last + 1)
            )
        }
        GameType.DIVISION_STEP -> {
            val range = digitsRange(config.difficulty.digits)
            val divisorRange = digitsRange(config.difficulty.divisorDigits ?: 1)
            var dividend = range.first
            var divisor = divisorRange.first.coerceAtLeast(2)
            var found = false
            val divisorMin = maxOf(divisorRange.first, 2)
            val divisorMax = maxOf(divisorRange.last, divisorMin)
            repeat(100) {
                val candidateDividend = Random.nextInt(range.first, range.last + 1)
                val maxDivisor = minOf(divisorMax, candidateDividend / 2)
                if (maxDivisor >= divisorMin) {
                    dividend = candidateDividend
                    divisor = Random.nextInt(divisorMin, maxDivisor + 1)
                    found = true
                    return@repeat
                }
            }
            if (!found) {
                val safeDividend = maxOf(range.first, divisorMin * 2)
                dividend = safeDividend.coerceAtMost(range.last).coerceAtLeast(range.first)
                val maxDivisor = minOf(divisorMax, dividend / 2)
                divisor = maxDivisor.coerceAtLeast(divisorMin)
            }
            // why: random divisions must never yield b <= 0 or a < b * 2.
            val safeDivisor = divisor.coerceAtLeast(2)
            val safeDividend = maxOf(dividend, safeDivisor * 2)
            dividend = safeDividend
            divisor = safeDivisor
            ExerciseInstance(game = config.game, a = dividend, b = divisor)
        }
        GameType.MULTIPLICATION_HARD -> {
            val multiplicandDigits = (config.difficulty.maxA ?: 2).coerceIn(2, 3)
            val multiplierDigits = if (multiplicandDigits == 3) {
                1
            } else {
                (config.difficulty.maxB ?: 1).coerceIn(1, 2)
            }
            val rangeA = digitsRange(multiplicandDigits)
            val rangeB = digitsRange(multiplierDigits)
            ExerciseInstance(
                game = config.game,
                a = Random.nextInt(rangeA.first, rangeA.last + 1),
                b = Random.nextInt(rangeB.first, rangeB.last + 1)
            )
        }
        GameType.MONEY_COUNT -> ExerciseInstance(game = config.game)
    }
}

private fun digitsRange(digits: Int?): IntRange {
    return if (digits == null) {
        1..10
    } else {
        val safeDigits = digits.coerceAtLeast(1)
        val min = 10.0.pow(safeDigits - 1).toInt()
        val max = 10.0.pow(safeDigits).toInt() - 1
        min..max
    }
}
