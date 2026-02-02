package com.example.matematicaperbambini

import java.util.Locale

internal fun genericCategory(game: GameType): String {
    return when (game) {
        GameType.ADDITION -> "Addizioni da ripassare"
        GameType.SUBTRACTION -> "Sottrazioni da ripassare"
        GameType.DIVISION_STEP -> "Divisioni da ripassare"
        GameType.MONEY_COUNT -> "Conta dei soldi da ripassare"
        else -> "Moltiplicazioni e tabelline da ripassare"
    }
}

internal fun classifyStepError(stepLabel: String, game: GameType): String {
    val label = stepLabel.lowercase(Locale.getDefault())
    if (label.contains("borrow_chain_error")) {
        return "Prestiti nelle sottrazioni"
    }
    if (label.contains("borrow_value_error")) {
        return "Prestiti nelle sottrazioni"
    }
    if (label.contains("borrow_target_error")) {
        return "Prestiti nelle sottrazioni"
    }
    if (label.contains("subtraction_calculation_error")) {
        return "Sottrazioni da ripassare"
    }
    if (label.contains("riporto")) {
        return "Riporti nelle addizioni"
    }
    if (label.contains("prestito")) {
        return "Prestiti nelle sottrazioni"
    }
    if (label.contains("quoziente")) {
        return "Cifre del quoziente nelle divisioni"
    }
    if (label.contains("prodotto")) {
        return "Prodotti nelle divisioni"
    }
    if (label.contains("resto")) {
        return "Resti nelle divisioni"
    }
    return genericCategory(game)
}

internal fun analyzeErrorPatterns(results: List<ExerciseResult>): List<ErrorPattern> {
    if (results.isEmpty()) return emptyList()
    val patterns = mutableMapOf<String, Pair<Int, MutableSet<GameType>>>()
    results.forEach { result ->
        if (!result.hasErrors()) return@forEach
        val categories = mutableSetOf<String>()
        if (result.stepErrors.isNotEmpty()) {
            result.stepErrors.forEach { step ->
                categories += classifyStepError(step.stepLabel, result.instance.game)
            }
        } else {
            categories += genericCategory(result.instance.game)
        }
        categories.forEach { category ->
            val current = patterns[category]
            val count = (current?.first ?: 0) + 1
            val games = current?.second ?: mutableSetOf()
            games += result.instance.game
            patterns[category] = count to games
        }
    }
    return patterns.entries
        .sortedByDescending { it.value.first }
        .map { (category, data) ->
            ErrorPattern(
                category = category,
                occurrences = data.first,
                games = data.second.sortedBy { it.name }
            )
        }
}

internal fun suggestionsForPatterns(patterns: List<ErrorPattern>): List<String> {
    if (patterns.isEmpty()) return emptyList()
    val suggestionMap = mapOf(
        "Riporti nelle addizioni" to listOf(
            "Potrebbe essere utile ripassare le addizioni con riporto",
            "Consigliati esercizi guidati con numeri a due cifre"
        ),
        "Prestiti nelle sottrazioni" to listOf(
            "Potrebbe aiutare ripassare le sottrazioni con prestito",
            "Utile lavorare su esempi passo-passo"
        ),
        "Cifre del quoziente nelle divisioni" to listOf(
            "Potrebbe essere utile ripassare la scelta del quoziente",
            "Consigliati esercizi guidati con divisori semplici"
        ),
        "Prodotti nelle divisioni" to listOf(
            "Potrebbe essere utile ripassare le moltiplicazioni collegate alle divisioni",
            "Utile esercitarsi su prodotti entro le tabelline base"
        ),
        "Resti nelle divisioni" to listOf(
            "Potrebbe essere utile ripassare il concetto di resto",
            "Consigliati esercizi guidati con resto semplice"
        ),
        "Addizioni da ripassare" to listOf(
            "Potrebbe essere utile ripassare le addizioni di base",
            "Utile usare esercizi con numeri piccoli e graduali"
        ),
        "Sottrazioni da ripassare" to listOf(
            "Potrebbe essere utile ripassare le sottrazioni di base",
            "Utile partire da esempi senza prestito"
        ),
        "Divisioni da ripassare" to listOf(
            "Potrebbe essere utile ripassare le divisioni con quozienti semplici",
            "Consigliati esercizi guidati con resti piccoli"
        ),
        "Conta dei soldi da ripassare" to listOf(
            "Potrebbe essere utile esercitarsi con somme di monete semplici",
            "Utile usare esempi con pochi valori alla volta"
        ),
        "Moltiplicazioni e tabelline da ripassare" to listOf(
            "Potrebbe essere utile ripassare le tabelline principali",
            "Consigliati esercizi guidati sulle moltiplicazioni di base"
        )
    )
    return patterns.asSequence()
        .flatMap { pattern -> suggestionMap[pattern.category].orEmpty().asSequence() }
        .distinct()
        .take(2)
        .toList()
}
