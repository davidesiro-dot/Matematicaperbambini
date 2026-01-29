package com.example.matematicaperbambini

enum class GameType(val title: String) {
    ADDITION("Addizioni"),
    SUBTRACTION("Sottrazioni"),
    MULTIPLICATION_MIXED("Tabelline miste"),
    MULTIPLICATION_TABLE("Tabellina"),
    DIVISION_STEP("Divisioni passo-passo"),
    MONEY_COUNT("Conta i soldi"),
    MULTIPLICATION_HARD("Moltiplicazioni difficili")
}

data class HomeworkTaskConfig(
    val game: GameType,
    val difficulty: DifficultyConfig,
    val helps: HelpSettings,
    val source: ExerciseSourceConfig,
    val amount: AmountConfig
)

data class HomeworkExerciseEntry(
    val instance: ExerciseInstance,
    val helps: HelpSettings
)

data class DifficultyConfig(
    val digits: Int? = null,
    val level: Int? = null,
    val maxA: Int? = null,
    val maxB: Int? = null
)

data class HelpSettings(
    val hintsEnabled: Boolean,
    val highlightsEnabled: Boolean,
    val allowSolution: Boolean,
    val autoCheck: Boolean
)

sealed class ExerciseSourceConfig {
    object Random : ExerciseSourceConfig()
    data class Manual(val ops: List<ManualOp>) : ExerciseSourceConfig()
}

data class AmountConfig(
    val exercisesCount: Int,
    val repeatsPerExercise: Int
)

sealed class ManualOp {
    data class AB(val a: Int, val b: Int) : ManualOp()
    data class Table(val table: Int) : ManualOp()
}

data class ExerciseInstance(
    val game: GameType,
    val a: Int? = null,
    val b: Int? = null,
    val table: Int? = null,
    val meta: Map<String, String> = emptyMap()
)

data class ExerciseResult(
    val instance: ExerciseInstance,
    val correct: Boolean,
    val attempts: Int,
    val wrongAnswers: List<String>,
    val solutionUsed: Boolean,
    val startedAt: Long,
    val endedAt: Long
)

data class ExerciseResultPartial(
    val correct: Boolean,
    val attempts: Int,
    val wrongAnswers: List<String>,
    val solutionUsed: Boolean
)

data class HomeworkReport(
    val childName: String,
    val createdAt: Long,
    val results: List<ExerciseResult>
)
