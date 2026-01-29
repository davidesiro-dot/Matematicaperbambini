package com.example.matematicaperbambini

import kotlin.math.pow
import kotlin.random.Random

fun buildExerciseQueue(configs: List<HomeworkTaskConfig>): List<ExerciseInstance> {
    val queue = mutableListOf<ExerciseInstance>()
    configs.forEach { config ->
        val repeats = config.amount.repeatsPerExercise.coerceAtLeast(1)
        when (val source = config.source) {
            is ExerciseSourceConfig.Random -> {
                val count = config.amount.exercisesCount.coerceAtLeast(1)
                repeat(count) {
                    val instance = generateRandomInstance(config)
                    repeat(repeats) {
                        queue += instance
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
                            table = op.table
                        )
                    }
                    repeat(repeats) {
                        queue += instance
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
            ExerciseInstance(game = config.game, table = table)
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
        GameType.DIVISION_STEP,
        GameType.MONEY_COUNT,
        GameType.MULTIPLICATION_HARD -> ExerciseInstance(game = config.game)
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
