package com.example.matematicaperbambini

import kotlinx.serialization.Serializable

@Serializable
enum class GameType(val title: String) {
    ADDITION("Addizioni"),
    SUBTRACTION("Sottrazioni"),
    MULTIPLICATION_MIXED("Tabelline miste"),
    MULTIPLICATION_TABLE("Tabellina"),
    MULTIPLICATION_GAPS("Buchi nella tabellina"),
    MULTIPLICATION_REVERSE("Tabellina al contrario"),
    MULTIPLICATION_MULTIPLE_CHOICE("Scelta multipla"),
    DIVISION_STEP("Divisioni passo-passo"),
    MONEY_COUNT("Conta i soldi"),
    MULTIPLICATION_HARD("Moltiplicazioni difficili")
}

@Serializable
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

@Serializable
data class DifficultyConfig(
    val digits: Int? = null,
    val divisorDigits: Int? = null,
    val level: Int? = null,
    val tables: List<Int>? = null,
    val maxA: Int? = null,
    val maxB: Int? = null
)

@Serializable
data class HelpSettings(
    val hintsEnabled: Boolean,
    val highlightsEnabled: Boolean,
    val allowSolution: Boolean,
    val autoCheck: Boolean,
    val showCellHelper: Boolean
)

enum class HelpPreset {
    GUIDED,
    TRAINING,
    CHALLENGE
}

fun HelpPreset.toHelpSettings(): HelpSettings = when (this) {
    HelpPreset.GUIDED -> HelpSettings(
        hintsEnabled = true,
        highlightsEnabled = true,
        allowSolution = true,
        autoCheck = true,
        showCellHelper = true
    )
    HelpPreset.TRAINING -> HelpSettings(
        hintsEnabled = false,
        highlightsEnabled = true,
        allowSolution = false,
        autoCheck = true,
        showCellHelper = true
    )
    HelpPreset.CHALLENGE -> HelpSettings(
        hintsEnabled = false,
        highlightsEnabled = false,
        allowSolution = false,
        autoCheck = false,
        showCellHelper = false
    )
}

fun HelpPreset.description(): String = when (this) {
    HelpPreset.GUIDED ->
        "Suggerimenti attivi, evidenziazioni attive, soluzione disponibile, controllo automatico."
    HelpPreset.TRAINING ->
        "Evidenziazioni attive. Nessuna soluzione. Controllo automatico."
    HelpPreset.CHALLENGE ->
        "Nessun aiuto attivo. Risolvi tutto da solo, come in classe."
}

@Serializable
sealed class ExerciseSourceConfig {
    @Serializable
    object Random : ExerciseSourceConfig()

    @Serializable
    data class Manual(val ops: List<ManualOp>) : ExerciseSourceConfig()
}

@Serializable
data class AmountConfig(
    val exercisesCount: Int,
    val repeatsPerExercise: Int
)

@Serializable
sealed class ManualOp {
    @Serializable
    data class AB(val a: Int, val b: Int) : ManualOp()

    @Serializable
    data class Table(val table: Int) : ManualOp()
}

@Serializable
data class ExerciseInstance(
    val game: GameType,
    val a: Int? = null,
    val b: Int? = null,
    val table: Int? = null,
    val meta: Map<String, String> = emptyMap()
)

@Serializable
data class ExerciseResult(
    val instance: ExerciseInstance,
    val correct: Boolean,
    val attempts: Int,
    val wrongAnswers: List<String>,
    val stepErrors: List<StepError> = emptyList(),
    val solutionUsed: Boolean,
    val startedAt: Long,
    val endedAt: Long
)

enum class ExerciseOutcome {
    PERFECT,
    COMPLETED_WITH_ERRORS,
    FAILED
}

fun ExerciseResult.hasErrors(): Boolean {
    return wrongAnswers.isNotEmpty() || stepErrors.isNotEmpty()
}

fun ExerciseResult.outcome(): ExerciseOutcome {
    return when {
        !correct -> ExerciseOutcome.FAILED
        hasErrors() -> ExerciseOutcome.COMPLETED_WITH_ERRORS
        else -> ExerciseOutcome.PERFECT
    }
}

data class ErrorPattern(
    val category: String,
    val occurrences: Int,
    val games: List<GameType>
)

@Serializable
data class ExerciseResultPartial(
    val correct: Boolean,
    val attempts: Int,
    val wrongAnswers: List<String>,
    val stepErrors: List<StepError> = emptyList(),
    val solutionUsed: Boolean
)

@Serializable
data class StepError(
    val stepLabel: String,
    val expected: String,
    val actual: String
)

@Serializable
data class HomeworkReport(
    val childName: String,
    val createdAt: Long,
    val results: List<ExerciseResult>,
    val interrupted: Boolean = false,
    val completedExercises: Int = 0,
    val totalExercises: Int = 0
)

@Serializable
data class SavedHomework(
    val id: String,
    val name: String,
    val createdAt: Long,
    val tasks: List<HomeworkTaskConfig>
)
