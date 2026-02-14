package com.example.matematicaperbambini

import kotlin.random.Random

enum class GradeLevel { I, II, III, IV }

enum class LessonKind { BASE }

enum class OperationType { ADD, SUB, MUL, DIV }

data class LessonSpec(
    val id: String,
    val title: String,
    val description: String,
    val grade: GradeLevel,
    val operation: OperationType,
    val kind: LessonKind = LessonKind.BASE,
    val levelIndex: Int,
    val difficulty: Int,
    val helpPreset: HelpSettings?,
    val fixedExercises: List<ExerciseInstance> = emptyList(),
    val generator: ((Random) -> List<ExerciseInstance>)? = null
)

fun baseHelps(levelIndex: Int): HelpSettings = when (levelIndex) {
    1 -> HelpSettings(true, true, true, true, true)
    2 -> HelpSettings(true, true, true, true, true)
    3 -> HelpSettings(true, true, true, true, false)
    4 -> HelpSettings(false, true, false, true, false)
    else -> HelpSettings(false, false, false, true, false)
}

fun buildBaseCatalog(): List<LessonSpec> {
    val lessons = mutableListOf<LessonSpec>()

    fun addFixed(
        id: String,
        title: String,
        description: String,
        grade: GradeLevel,
        operation: OperationType,
        level: Int,
        ops: List<Pair<Int, Int>>,
        gameType: GameType
    ) {
        lessons += LessonSpec(
            id = id,
            title = title,
            description = description,
            grade = grade,
            operation = operation,
            levelIndex = level,
            difficulty = level,
            helpPreset = baseHelps(level),
            fixedExercises = ops.map { (a, b) -> ExerciseInstance(game = gameType, a = a, b = b) }
        )
    }

    // Classe I - ADD
    addFixed("I-ADD-1", "Classe I • Addizioni 1", "Entro 10 facilissime", GradeLevel.I, OperationType.ADD, 1,
        listOf(1 to 1,2 to 1,3 to 1,2 to 2,3 to 2,4 to 1,4 to 2,5 to 1,3 to 3,5 to 2), GameType.ADDITION)
    addFixed("I-ADD-2", "Classe I • Addizioni 2", "Entro 10 consolidamento", GradeLevel.I, OperationType.ADD, 2,
        listOf(2 to 3,4 to 4,5 to 3,6 to 2,7 to 1,8 to 1,9 to 0,6 to 3,7 to 2,8 to 2), GameType.ADDITION)
    addFixed("I-ADD-3", "Classe I • Addizioni 3", "Entro 20 senza riporto", GradeLevel.I, OperationType.ADD, 3,
        listOf(10 to 2,11 to 3,12 to 5,14 to 2,15 to 3,16 to 2,13 to 4,17 to 1,18 to 1,19 to 0), GameType.ADDITION)
    addFixed("I-ADD-4", "Classe I • Addizioni 4", "Ponte al 10 entro 20", GradeLevel.I, OperationType.ADD, 4,
        listOf(8 to 5,7 to 6,9 to 4,6 to 8,5 to 9,4 to 7,3 to 9,6 to 7,8 to 6,7 to 5), GameType.ADDITION)
    addFixed("I-ADD-5", "Classe I • Addizioni 5", "Sfida entro 20", GradeLevel.I, OperationType.ADD, 5,
        listOf(9 to 8,7 to 9,8 to 7,6 to 9,5 to 8,4 to 9,3 to 8,2 to 9,8 to 8,9 to 9), GameType.ADDITION)

    // Classe II - ADD + SUB
    addFixed("II-ADD-1", "Classe II • Addizioni 1", "Fino a 100 senza riporto", GradeLevel.II, OperationType.ADD, 1,
        listOf(21 to 13,32 to 14,41 to 28,53 to 16,64 to 25,72 to 17,84 to 15,93 to 16,55 to 24,62 to 18), GameType.ADDITION)
    addFixed("II-ADD-2", "Classe II • Addizioni 2", "Riporto semplice", GradeLevel.II, OperationType.ADD, 2,
        listOf(27 to 18,46 to 27,58 to 36,79 to 18,86 to 47,95 to 16,48 to 39,67 to 28,39 to 47,74 to 19), GameType.ADDITION)
    addFixed("II-ADD-3", "Classe II • Addizioni 3", "Riporto sistematico", GradeLevel.II, OperationType.ADD, 3,
        listOf(38 to 27,49 to 36,57 to 48,69 to 27,78 to 46,89 to 15,46 to 57,68 to 39,79 to 28,58 to 47), GameType.ADDITION)
    addFixed("II-ADD-4", "Classe II • Addizioni 4", "3 cifre senza riporto", GradeLevel.II, OperationType.ADD, 4,
        listOf(121 to 132,243 to 214,356 to 241,478 to 321,589 to 310,612 to 221,734 to 120,845 to 132,956 to 210,321 to 432), GameType.ADDITION)
    addFixed("II-ADD-5", "Classe II • Addizioni 5", "3 cifre con riporto", GradeLevel.II, OperationType.ADD, 5,
        listOf(245 to 178,367 to 245,489 to 376,578 to 294,689 to 357,754 to 268,826 to 379,945 to 286,632 to 489,718 to 356), GameType.ADDITION)

    addFixed("II-SUB-1", "Classe II • Sottrazioni 1", "Fino a 100 senza prestito", GradeLevel.II, OperationType.SUB, 1,
        listOf(32 to 11,43 to 21,54 to 32,65 to 43,76 to 54,87 to 65,98 to 76,59 to 38,68 to 47,79 to 58), GameType.SUBTRACTION)
    addFixed("II-SUB-2", "Classe II • Sottrazioni 2", "Prestito semplice", GradeLevel.II, OperationType.SUB, 2,
        listOf(52 to 27,63 to 48,74 to 39,85 to 57,96 to 68,84 to 29,73 to 46,62 to 37,51 to 28,94 to 56), GameType.SUBTRACTION)
    addFixed("II-SUB-3", "Classe II • Sottrazioni 3", "Prestito sistematico", GradeLevel.II, OperationType.SUB, 3,
        listOf(71 to 36,82 to 47,93 to 58,64 to 29,75 to 38,86 to 49,97 to 58,83 to 67,92 to 75,61 to 48), GameType.SUBTRACTION)
    addFixed("II-SUB-4", "Classe II • Sottrazioni 4", "3 cifre senza prestito", GradeLevel.II, OperationType.SUB, 4,
        listOf(654 to 321,876 to 543,965 to 432,742 to 231,853 to 421,964 to 531,731 to 210,842 to 321,953 to 420,864 to 532), GameType.SUBTRACTION)
    addFixed("II-SUB-5", "Classe II • Sottrazioni 5", "3 cifre con prestito", GradeLevel.II, OperationType.SUB, 5,
        listOf(652 to 287,743 to 368,854 to 479,965 to 587,784 to 296,673 to 385,562 to 274,951 to 486,842 to 375,731 to 264), GameType.SUBTRACTION)

    fun generatedLesson(grade: GradeLevel, op: OperationType, level: Int, difficultyBase: Int = level): LessonSpec {
        val (idPrefix, gameType) = when (op) {
            OperationType.ADD -> "${grade.name}-ADD" to GameType.ADDITION
            OperationType.SUB -> "${grade.name}-SUB" to GameType.SUBTRACTION
            OperationType.MUL -> "${grade.name}-MUL" to GameType.MULTIPLICATION_HARD
            OperationType.DIV -> "${grade.name}-DIV" to GameType.DIVISION_STEP
        }
        return LessonSpec(
            id = "$idPrefix-$level",
            title = "Classe ${grade.name} • ${opLabel(op)} $level",
            description = "Percorso base livello $level",
            grade = grade,
            operation = op,
            levelIndex = level,
            difficulty = difficultyBase,
            helpPreset = baseHelps(level),
            generator = { rng ->
                when (op) {
                    OperationType.ADD -> List(10) {
                        val max = if (grade == GradeLevel.III) 500 else 999
                        val a = rng.nextInt(max / 3, max)
                        val b = rng.nextInt(max / 4, max / 2)
                        ExerciseInstance(gameType, a = a, b = b)
                    }
                    OperationType.SUB -> List(10) {
                        val max = if (grade == GradeLevel.III) 600 else 999
                        val a = rng.nextInt(max / 2, max)
                        val b = rng.nextInt(max / 4, a - 1)
                        ExerciseInstance(gameType, a = a, b = b)
                    }
                    OperationType.MUL -> List(10) {
                        val a = rng.nextInt(if (grade == GradeLevel.III) 10 else 12, if (grade == GradeLevel.III) 99 else 100)
                        val b = rng.nextInt(if (grade == GradeLevel.III) 2 else 4, if (grade == GradeLevel.III) 10 else 100)
                        ExerciseInstance(gameType, a = a, b = b)
                    }
                    OperationType.DIV -> List(10) {
                        val divisor = rng.nextInt(if (grade == GradeLevel.III) 2 else 4, if (grade == GradeLevel.III) 10 else 100)
                        val quotient = rng.nextInt(2, if (grade == GradeLevel.III) 30 else 80)
                        val dividend = divisor * quotient + rng.nextInt(0, divisor.coerceAtMost(9))
                        ExerciseInstance(gameType, a = dividend, b = divisor)
                    }
                }
            }
        )
    }

    listOf(GradeLevel.III, GradeLevel.IV).forEach { grade ->
        listOf(OperationType.ADD, OperationType.SUB, OperationType.MUL, OperationType.DIV).forEach { op ->
            (1..5).forEach { level ->
                lessons += generatedLesson(grade, op, level, difficultyBase = if (grade == GradeLevel.IV) (level + 1).coerceAtMost(5) else level)
            }
        }
    }

    return lessons
}

private fun opLabel(op: OperationType): String = when (op) {
    OperationType.ADD -> "Addizioni"
    OperationType.SUB -> "Sottrazioni"
    OperationType.MUL -> "Moltiplicazioni"
    OperationType.DIV -> "Divisioni"
}
