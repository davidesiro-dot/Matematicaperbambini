package com.example.matematicaperbambini

import kotlin.random.Random

enum class GradeLevel { I, II, III, IV }

enum class LessonKind { BASE }

enum class OperationType { ADD, SUB, MUL, DIV }

enum class LessonCategory { BASE, SPECIAL, CHALLENGE }

data class LessonSpec(
    val id: String,
    val title: String,
    val description: String,
    val grade: GradeLevel,
    val operation: OperationType,
    val category: LessonCategory = LessonCategory.BASE,
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
        gameType: GameType,
        difficulty: Int = level,
        category: LessonCategory = LessonCategory.BASE,
        helps: HelpSettings = baseHelps(level),
        withMeta: ((Int, Pair<Int, Int>) -> Map<String, String>)? = null
    ) {
        lessons += LessonSpec(
            id = id,
            title = title,
            description = description,
            grade = grade,
            operation = operation,
            category = category,
            levelIndex = level,
            difficulty = difficulty,
            helpPreset = helps,
            fixedExercises = ops.mapIndexed { index, (a, b) ->
                ExerciseInstance(game = gameType, a = a, b = b, meta = withMeta?.invoke(index, a to b) ?: emptyMap())
            }
        )
    }

    fun addGenerated(
        id: String,
        title: String,
        description: String,
        grade: GradeLevel,
        level: Int,
        difficulty: Int,
        category: LessonCategory,
        helps: HelpSettings,
        generator: (Random) -> List<ExerciseInstance>
    ) {
        lessons += LessonSpec(
            id = id,
            title = title,
            description = description,
            grade = grade,
            operation = OperationType.ADD,
            category = category,
            levelIndex = level,
            difficulty = difficulty,
            helpPreset = helps,
            generator = generator
        )
    }

    fun chainCases(
        random: Random,
        count: Int,
        firstMin: Int,
        firstMax: Int,
        totalMax: Int,
        carryFirst: Boolean = false,
        carrySecond: Boolean = false
    ): List<ExerciseInstance> {
        val list = mutableListOf<ExerciseInstance>()
        repeat(count) {
            var a: Int
            var b: Int
            var step2: Int
            do {
                a = random.nextInt(firstMin, firstMax + 1)
                b = random.nextInt(1, (totalMax - a).coerceAtLeast(2))
                step2 = random.nextInt(2, 10)
                val firstCarryOk = !carryFirst || ((a % 10) + (b % 10) >= 10)
                val secondCarryOk = !carrySecond || ((((a + b) % 10) + step2) >= 10)
                if (firstCarryOk && secondCarryOk && a + b + step2 <= totalMax) break
            } while (true)

            list += ExerciseInstance(
                game = GameType.ADDITION,
                a = a,
                b = b,
                meta = mapOf(
                    "chainStep" to "2",
                    "followUpAdd" to step2.toString(),
                    "instruction" to "Calcola ${a}+${b}, poi aggiungi $step2"
                )
            )
        }
        return list
    }

    val specialHelps = HelpSettings(true, true, true, true, true)
    val specialSoftHelps = HelpSettings(true, true, false, true, true)
    val challengeHelps = HelpPreset.CHALLENGE.toHelpSettings()
    val borrowHighlightHelps = HelpSettings(false, true, false, true, true)

    // Classe I - ADD (SPECIAL + CHALLENGE)
    addFixed("I-ADD-S1", "Classe I • Addizioni • Special 1", "Somme entro 10, numeri superiori", GradeLevel.I, OperationType.ADD, 1,
        difficulty = 1, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(6 to 1, 7 to 2, 8 to 1, 9 to 1, 6 to 2, 7 to 3, 8 to 2, 9 to 2, 5 to 4, 4 to 5), gameType = GameType.ADDITION)
    addFixed("I-ADD-S2", "Classe I • Addizioni • Special 2", "Coppie che fanno 10", GradeLevel.I, OperationType.ADD, 2,
        difficulty = 2, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(1 to 9, 2 to 8, 3 to 7, 4 to 6, 5 to 5, 9 to 1, 8 to 2, 7 to 3, 6 to 4, 0 to 10), gameType = GameType.ADDITION)
    addFixed("I-ADD-S3", "Classe I • Addizioni • Special 3", "Somme entro 20 senza riempimento", GradeLevel.I, OperationType.ADD, 3,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(10 to 3, 9 to 4, 8 to 7, 11 to 5, 12 to 6, 7 to 8, 13 to 4, 6 to 9, 14 to 5, 15 to 3), gameType = GameType.ADDITION)
    addFixed("I-ADD-C1", "Classe I • Addizioni • Challenge 1", "Somme veloci entro 20", GradeLevel.I, OperationType.ADD, 4,
        difficulty = 3, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(9 to 8, 8 to 7, 7 to 6, 9 to 6, 8 to 9, 7 to 8, 6 to 9, 5 to 9, 4 to 8, 3 to 9), gameType = GameType.ADDITION)
    addFixed("I-ADD-C2", "Classe I • Addizioni • Challenge 2", "Somme miste avanzate", GradeLevel.I, OperationType.ADD, 5,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(11 to 9, 12 to 8, 13 to 7, 14 to 6, 15 to 5, 16 to 4, 17 to 3, 18 to 2, 19 to 1, 10 to 10), gameType = GameType.ADDITION)
    addGenerated("I-ADD-C3", "Classe I • Addizioni • Challenge 3", "Catena di somme entro 20", GradeLevel.I, 5, 5, LessonCategory.CHALLENGE, challengeHelps) { rng ->
        val fixed = listOf(
            ExerciseInstance(GameType.ADDITION, 7, 3, meta = mapOf("chainStep" to "2", "followUpAdd" to "5", "instruction" to "Calcola 7+3, poi aggiungi 5")),
            ExerciseInstance(GameType.ADDITION, 10, 4, meta = mapOf("chainStep" to "2", "followUpAdd" to "6", "instruction" to "Calcola 10+4, poi aggiungi 6"))
        )
        fixed + chainCases(rng, count = 8, firstMin = 4, firstMax = 12, totalMax = 20)
    }

    // Classe I - SUB (SPECIAL + CHALLENGE)
    addFixed("I-SUB-S1", "Classe I • Sottrazioni • Special 1", "Differenza sempre 5", GradeLevel.I, OperationType.SUB, 1,
        difficulty = 1, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(10 to 5, 9 to 4, 8 to 3, 7 to 2, 6 to 1, 5 to 0, 15 to 10, 14 to 9, 13 to 8, 12 to 7), gameType = GameType.SUBTRACTION)
    addFixed("I-SUB-S2", "Classe I • Sottrazioni • Special 2", "Differenza sempre 1", GradeLevel.I, OperationType.SUB, 2,
        difficulty = 2, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(7 to 6, 8 to 7, 9 to 8, 6 to 5, 5 to 4, 4 to 3, 3 to 2, 2 to 1, 1 to 0, 10 to 9), gameType = GameType.SUBTRACTION)
    addFixed("I-SUB-S3", "Classe I • Sottrazioni • Special 3", "Sottrazioni simmetriche", GradeLevel.I, OperationType.SUB, 3,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(9 to 3, 8 to 2, 7 to 1, 6 to 4, 5 to 3, 10 to 5, 12 to 6, 14 to 7, 16 to 8, 18 to 9), gameType = GameType.SUBTRACTION)
    addFixed("I-SUB-C1", "Classe I • Sottrazioni • Challenge 1", "Differenza sempre 9", GradeLevel.I, OperationType.SUB, 4,
        difficulty = 3, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(18 to 9, 27 to 18, 36 to 27, 45 to 36, 54 to 45, 63 to 54, 72 to 63, 81 to 72, 90 to 81, 99 to 90), gameType = GameType.SUBTRACTION)
    addFixed("I-SUB-C2", "Classe I • Sottrazioni • Challenge 2", "Entro 20 misto", GradeLevel.I, OperationType.SUB, 5,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(17 to 9, 18 to 7, 16 to 8, 19 to 6, 14 to 5, 13 to 9, 12 to 7, 15 to 8, 11 to 4, 10 to 6), gameType = GameType.SUBTRACTION)
    addFixed("I-SUB-C3", "Classe I • Sottrazioni • Challenge 3", "Catena sottrazioni entro 20", GradeLevel.I, OperationType.SUB, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(20 to 5, 15 to 3, 12 to 4, 8 to 2, 6 to 1, 5 to 2, 3 to 1, 2 to 1, 1 to 0, 1 to 1), gameType = GameType.SUBTRACTION)

    // Classe II - ADD (SPECIAL + CHALLENGE)
    addFixed("II-ADD-S1", "Classe II • Addizioni • Special 1", "Riempimento unità entro 50", GradeLevel.II, OperationType.ADD, 1,
        difficulty = 2, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(28 to 7, 19 to 8, 37 to 6, 45 to 9, 26 to 8, 32 to 9, 24 to 7, 41 to 8, 38 to 9, 27 to 6), gameType = GameType.ADDITION)
    addFixed("II-ADD-S2", "Classe II • Addizioni • Special 2", "Risultato fisso: totale 50", GradeLevel.II, OperationType.ADD, 2,
        difficulty = 2, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(25 to 25, 30 to 20, 20 to 30, 15 to 35, 40 to 10, 18 to 32, 22 to 28, 24 to 26, 19 to 31, 27 to 23), gameType = GameType.ADDITION)
    addFixed("II-ADD-S3", "Classe II • Addizioni • Special 3", "Coppie da 10…20 con totali 20–30", GradeLevel.II, OperationType.ADD, 3,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(12 to 8, 13 to 7, 14 to 6, 15 to 5, 16 to 4, 17 to 3, 18 to 2, 19 to 1, 11 to 9, 10 to 10), gameType = GameType.ADDITION)
    addFixed("II-ADD-C1", "Classe II • Addizioni • Challenge 1", "Doppio riempimento", GradeLevel.II, OperationType.ADD, 4,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(29 to 18, 37 to 16, 46 to 19, 58 to 17, 49 to 18, 53 to 27, 61 to 29, 72 to 18, 84 to 15, 67 to 28), gameType = GameType.ADDITION)
    addFixed("II-ADD-C2", "Classe II • Addizioni • Challenge 2", "Somme senza aiuti entro 60", GradeLevel.II, OperationType.ADD, 5,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(34 to 15, 28 to 19, 45 to 14, 31 to 27, 39 to 16, 47 to 12, 52 to 18, 43 to 17, 56 to 13, 49 to 15), gameType = GameType.ADDITION)
    addGenerated("II-ADD-C3", "Classe II • Addizioni • Challenge 3", "Catena di 2 passi entro 60", GradeLevel.II, 5, 5, LessonCategory.CHALLENGE, challengeHelps) { rng ->
        val fixed = listOf(
            ExerciseInstance(GameType.ADDITION, 21, 19, meta = mapOf("chainStep" to "2", "followUpAdd" to "7", "instruction" to "Calcola 21+19, poi aggiungi 7"))
        )
        fixed + chainCases(rng, count = 9, firstMin = 18, firstMax = 45, totalMax = 60, carryFirst = true)
    }

    // Classe II - SUB (SPECIAL + CHALLENGE)
    addFixed("II-SUB-S1", "Classe II • Sottrazioni • Special 1", "Tutti con prestito", GradeLevel.II, OperationType.SUB, 1,
        difficulty = 2, category = LessonCategory.SPECIAL, helps = borrowHighlightHelps,
        ops = listOf(32 to 17, 45 to 28, 53 to 26, 61 to 34, 74 to 39, 82 to 47, 90 to 58, 67 to 29, 58 to 19, 49 to 18), gameType = GameType.SUBTRACTION)
    addFixed("II-SUB-S2", "Classe II • Sottrazioni • Special 2", "Risultato sempre 20", GradeLevel.II, OperationType.SUB, 2,
        difficulty = 2, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(40 to 20, 35 to 15, 50 to 30, 45 to 25, 60 to 40, 38 to 18, 44 to 24, 70 to 50, 66 to 46, 52 to 32), gameType = GameType.SUBTRACTION)
    addFixed("II-SUB-S3", "Classe II • Sottrazioni • Special 3", "Differenza sempre 11", GradeLevel.II, OperationType.SUB, 3,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(22 to 11, 33 to 22, 44 to 33, 55 to 44, 66 to 55, 77 to 66, 88 to 77, 99 to 88, 110 to 99, 121 to 110), gameType = GameType.SUBTRACTION)
    addFixed("II-SUB-C1", "Classe II • Sottrazioni • Challenge 1", "Prestito multiplo", GradeLevel.II, OperationType.SUB, 4,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(73 to 48, 84 to 59, 95 to 67, 68 to 49, 79 to 58, 87 to 69, 96 to 78, 65 to 37, 74 to 58, 83 to 67), gameType = GameType.SUBTRACTION)
    addFixed("II-SUB-C2", "Classe II • Sottrazioni • Challenge 2", "Differenza palindroma", GradeLevel.II, OperationType.SUB, 5,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(123 to 22, 232 to 121, 345 to 222, 454 to 343, 567 to 444, 678 to 555, 789 to 666, 890 to 767, 901 to 878, 1000 to 999), gameType = GameType.SUBTRACTION)
    addFixed("II-SUB-C3", "Classe II • Sottrazioni • Challenge 3", "Differenza 99", GradeLevel.II, OperationType.SUB, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(198 to 99, 297 to 198, 396 to 297, 495 to 396, 594 to 495, 693 to 594, 792 to 693, 891 to 792, 990 to 891, 1089 to 990), gameType = GameType.SUBTRACTION)

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
        listOf(OperationType.SUB, OperationType.MUL, OperationType.DIV).forEach { op ->
            (1..5).forEach { level ->
                lessons += generatedLesson(grade, op, level, difficultyBase = if (grade == GradeLevel.IV) (level + 1).coerceAtMost(5) else level)
            }
        }
    }

    // Classe III - MUL (SPECIAL + CHALLENGE)
    addFixed("III-MUL-S1", "Classe III • Moltiplicazioni • Special 1", "Tabelline specchiate", GradeLevel.III, OperationType.MUL, 1,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(3 to 4, 4 to 3, 5 to 6, 6 to 5, 7 to 8, 8 to 7, 9 to 2, 2 to 9, 4 to 5, 5 to 4), gameType = GameType.MULTIPLICATION_HARD)
    addFixed("III-MUL-S2", "Classe III • Moltiplicazioni • Special 2", "Risultato sempre 24", GradeLevel.III, OperationType.MUL, 2,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(3 to 8, 4 to 6, 6 to 4, 8 to 3, 12 to 2, 2 to 12, 1 to 24, 24 to 1, 6 to 4, 4 to 6), gameType = GameType.MULTIPLICATION_HARD)
    addFixed("III-MUL-S3", "Classe III • Moltiplicazioni • Special 3", "Moltiplicazioni con 10", GradeLevel.III, OperationType.MUL, 3,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(12 to 10, 23 to 10, 34 to 10, 45 to 10, 56 to 10, 67 to 10, 78 to 10, 89 to 10, 90 to 10, 101 to 10), gameType = GameType.MULTIPLICATION_HARD)
    addFixed("III-MUL-C1", "Classe III • Moltiplicazioni • Challenge 1", "Doppia cifra", GradeLevel.III, OperationType.MUL, 4,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(12 to 7, 15 to 6, 18 to 9, 23 to 4, 27 to 5, 34 to 3, 45 to 2, 36 to 8, 29 to 7, 48 to 6), gameType = GameType.MULTIPLICATION_HARD)
    addFixed("III-MUL-C2", "Classe III • Moltiplicazioni • Challenge 2", "Risultato 144", GradeLevel.III, OperationType.MUL, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(12 to 12, 9 to 16, 8 to 18, 6 to 24, 4 to 36, 3 to 48, 2 to 72, 1 to 144, 18 to 8, 16 to 9), gameType = GameType.MULTIPLICATION_HARD)
    addFixed("III-MUL-C3", "Classe III • Moltiplicazioni • Challenge 3", "Catena moltiplicazioni", GradeLevel.III, OperationType.MUL, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(5 to 4, 20 to 2, 40 to 3, 120 to 2, 240 to 2, 480 to 1, 480 to 2, 960 to 1, 960 to 2, 1920 to 1), gameType = GameType.MULTIPLICATION_HARD)

    // Classe III - DIV (SPECIAL + CHALLENGE)
    addFixed("III-DIV-S1", "Classe III • Divisioni • Special 1", "Resto sempre 1", GradeLevel.III, OperationType.DIV, 1,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(7 to 3, 10 to 3, 13 to 3, 16 to 3, 19 to 3, 22 to 3, 25 to 3, 28 to 3, 31 to 3, 34 to 3), gameType = GameType.DIVISION_STEP)
    addFixed("III-DIV-S2", "Classe III • Divisioni • Special 2", "Resto sempre 2", GradeLevel.III, OperationType.DIV, 2,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(8 to 3, 11 to 3, 14 to 3, 17 to 3, 20 to 3, 23 to 3, 26 to 3, 29 to 3, 32 to 3, 35 to 3), gameType = GameType.DIVISION_STEP)
    addFixed("III-DIV-S3", "Classe III • Divisioni • Special 3", "Quoziente sempre 5", GradeLevel.III, OperationType.DIV, 3,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(25 to 5, 30 to 6, 35 to 7, 40 to 8, 45 to 9, 50 to 10, 55 to 11, 60 to 12, 65 to 13, 70 to 14), gameType = GameType.DIVISION_STEP)
    addFixed("III-DIV-C1", "Classe III • Divisioni • Challenge 1", "Resto 1 con 2 cifre", GradeLevel.III, OperationType.DIV, 4,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(41 to 4, 53 to 4, 65 to 4, 77 to 4, 89 to 4, 101 to 4, 113 to 4, 125 to 4, 137 to 4, 149 to 4), gameType = GameType.DIVISION_STEP)
    addFixed("III-DIV-C2", "Classe III • Divisioni • Challenge 2", "Divisioni difficili con resto", GradeLevel.III, OperationType.DIV, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(97 to 6, 83 to 7, 124 to 9, 158 to 8, 179 to 7, 205 to 9, 236 to 8, 257 to 6, 278 to 7, 299 to 8), gameType = GameType.DIVISION_STEP)
    addFixed("III-DIV-C3", "Classe III • Divisioni • Challenge 3", "Divisioni con quoziente 11", GradeLevel.III, OperationType.DIV, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(121 to 11, 132 to 12, 143 to 13, 154 to 14, 165 to 15, 176 to 16, 187 to 17, 198 to 18, 209 to 19, 220 to 20), gameType = GameType.DIVISION_STEP)

    // Classe III - ADD (SPECIAL + CHALLENGE)
    addFixed("III-ADD-S1", "Classe III • Addizioni • Special 1", "Doppi riempimenti 2 cifre", GradeLevel.III, OperationType.ADD, 1,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(58 to 47, 69 to 38, 77 to 46, 86 to 29, 95 to 17, 64 to 39, 73 to 28, 82 to 19, 54 to 48, 67 to 36), gameType = GameType.ADDITION)
    addFixed("III-ADD-S2", "Classe III • Addizioni • Special 2", "Somme a punteggio: multipli di 11", GradeLevel.III, OperationType.ADD, 2,
        difficulty = 3, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(61 to 50, 72 to 39, 83 to 28, 94 to 17, 105 to 6, 56 to 55, 44 to 67, 33 to 78, 22 to 89, 11 to 100), gameType = GameType.ADDITION)
    addGenerated("III-ADD-S3", "Classe III • Addizioni • Special 3", "Serie a progressione (risultato crescente)", GradeLevel.III, 3, 3, LessonCategory.SPECIAL, specialSoftHelps) { rng ->
        val bases = listOf(23 to 17, 40 to 12, 52 to 8)
        val generated = (0 until 7).map { idx ->
            val target = 50 + (idx + 1) * 10
            val a = rng.nextInt(20, (target - 10).coerceAtLeast(21))
            val b = target - a
            ExerciseInstance(GameType.ADDITION, a = a, b = b, meta = mapOf("patternTarget" to target.toString()))
        }
        bases.map { (a, b) -> ExerciseInstance(GameType.ADDITION, a = a, b = b) } + generated
    }
    addFixed("III-ADD-C1", "Classe III • Addizioni • Challenge 1", "3 cifre complesso", GradeLevel.III, OperationType.ADD, 4,
        difficulty = 4, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(347 to 586, 459 to 378, 568 to 497, 679 to 248, 785 to 169, 896 to 357, 734 to 289, 845 to 376, 957 to 284, 689 to 457), gameType = GameType.ADDITION)
    addFixed("III-ADD-C2", "Classe III • Addizioni • Challenge 2", "Senza aiuti multipli", GradeLevel.III, OperationType.ADD, 5,
        difficulty = 5, category = LessonCategory.CHALLENGE, helps = challengeHelps,
        ops = listOf(213 to 198, 307 to 289, 412 to 286, 389 to 174, 495 to 185, 568 to 329, 701 to 298, 624 to 375, 832 to 167, 759 to 238), gameType = GameType.ADDITION)
    addGenerated("III-ADD-C3", "Classe III • Addizioni • Challenge 3", "Sequenze senza aiuti", GradeLevel.III, 5, 5, LessonCategory.CHALLENGE, challengeHelps) { rng ->
        chainCases(rng, count = 10, firstMin = 80, firstMax = 220, totalMax = 350, carryFirst = true, carrySecond = true)
    }

    // Classe IV - ADD (SPECIAL + CHALLENGE)
    addFixed("IV-ADD-S1", "Classe IV • Addizioni • Special 1", "Quattro cifre ricorrenti", GradeLevel.IV, OperationType.ADD, 1,
        difficulty = 4, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(1111 to 2222, 1234 to 4321, 2468 to 1357, 3333 to 4444, 5555 to 4445, 6789 to 3211, 7890 to 2109, 1357 to 8643, 2468 to 7532, 999 to 9001), gameType = GameType.ADDITION)
    addFixed("IV-ADD-S2", "Classe IV • Addizioni • Special 2", "Numeri speculari", GradeLevel.IV, OperationType.ADD, 2,
        difficulty = 4, category = LessonCategory.SPECIAL, helps = specialHelps,
        ops = listOf(1023 to 3201, 2045 to 5402, 3056 to 6503, 4067 to 7604, 5078 to 8705, 6089 to 9806, 7090 to 907, 8011 to 1108, 9123 to 3219, 1230 to 321), gameType = GameType.ADDITION)
    addGenerated("IV-ADD-S3", "Classe IV • Addizioni • Special 3", "Catena cumulativa", GradeLevel.IV, 3, 4, LessonCategory.SPECIAL, specialSoftHelps) { rng ->
        chainCases(rng, count = 10, firstMin = 500, firstMax = 3000, totalMax = 5000)
    }
    addGenerated("IV-ADD-C1", "Classe IV • Addizioni • Challenge 1", "Quattro cifre avanzato", GradeLevel.IV, 4, 5, LessonCategory.CHALLENGE, challengeHelps) { rng ->
        val fixed = listOf(
            ExerciseInstance(GameType.ADDITION, 4587, 6398),
            ExerciseInstance(GameType.ADDITION, 5679, 7486)
        )
        val generated = List(8) {
            ExerciseInstance(GameType.ADDITION, a = rng.nextInt(3200, 9000), b = rng.nextInt(3200, 9000))
        }
        fixed + generated
    }
    addGenerated("IV-ADD-C2", "Classe IV • Addizioni • Challenge 2", "Problemi di catena complessa", GradeLevel.IV, 5, 5, LessonCategory.CHALLENGE, challengeHelps) { rng ->
        chainCases(rng, count = 10, firstMin = 900, firstMax = 5000, totalMax = 9000, carryFirst = true, carrySecond = true)
    }
    addGenerated("IV-ADD-C3", "Classe IV • Addizioni • Challenge 3", "Massimo senza aiuti", GradeLevel.IV, 5, 5, LessonCategory.CHALLENGE, challengeHelps) { rng ->
        List(10) {
            ExerciseInstance(GameType.ADDITION, a = rng.nextInt(5000, 15000), b = rng.nextInt(4000, 12000))
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
