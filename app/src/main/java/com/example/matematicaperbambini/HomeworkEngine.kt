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
        GameType.MULTIPLICATION_TABLE -> {
            val table = config.difficulty.level ?: Random.nextInt(1, 11)
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
            val divisorRange = digitsRange(1)
            val dividend = Random.nextInt(range.first, range.last + 1)
            val divisor = Random.nextInt(divisorRange.first, divisorRange.last + 1).coerceAtLeast(1)
            ExerciseInstance(game = config.game, a = dividend, b = divisor)
        }
        GameType.MULTIPLICATION_HARD -> {
            val maxA = (config.difficulty.maxA ?: 99).coerceIn(10, 99)
            val maxB = (config.difficulty.maxB ?: 99).coerceIn(1, 99)
            val rangeA = 10..maxA
            val rangeB = 1..maxB
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
