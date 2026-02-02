package com.example.matematicaperbambini

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun exerciseLabel(instance: ExerciseInstance): String {
    val a = instance.a ?: "?"
    val b = instance.b ?: "?"
    return when (instance.game) {
        GameType.ADDITION -> "$a + $b"
        GameType.SUBTRACTION -> "$a - $b"
        GameType.MULTIPLICATION_TABLE,
        GameType.MULTIPLICATION_GAPS -> "Tabellina del ${instance.table ?: a}"
        GameType.MULTIPLICATION_REVERSE,
        GameType.MULTIPLICATION_MULTIPLE_CHOICE -> "${instance.table ?: a} × $b"
        GameType.DIVISION_STEP -> "$a ÷ $b"
        else -> "$a × $b"
    }
}

internal fun expectedAnswer(instance: ExerciseInstance): String? {
    val a = instance.a
    val b = instance.b
    return when (instance.game) {
        GameType.ADDITION -> if (a != null && b != null) (a + b).toString() else null
        GameType.SUBTRACTION -> if (a != null && b != null) (a - b).toString() else null
        GameType.MULTIPLICATION_TABLE,
        GameType.MULTIPLICATION_GAPS,
        GameType.MULTIPLICATION_REVERSE,
        GameType.MULTIPLICATION_MULTIPLE_CHOICE,
        GameType.MULTIPLICATION_MIXED,
        GameType.MULTIPLICATION_HARD -> if (a != null && b != null) (a * b).toString() else null
        GameType.DIVISION_STEP -> {
            if (a != null && b != null && b != 0) {
                "Quoziente ${a / b}, resto ${a % b}"
            } else {
                null
            }
        }
        GameType.MONEY_COUNT -> null
    }
}

internal fun outcomeLabel(outcome: ExerciseOutcome): String {
    return when (outcome) {
        ExerciseOutcome.PERFECT -> "✅ Corretto"
        ExerciseOutcome.COMPLETED_WITH_ERRORS -> "⚠️ Completato con errori"
        ExerciseOutcome.FAILED -> "❌ Da ripassare"
    }
}

internal fun stepErrorDescription(error: StepError): String {
    val label = error.stepLabel.lowercase(Locale.getDefault())
    if (label.contains("borrow_chain_error")) {
        val parts = error.expected.split("->")
        if (parts.size == 2) {
            return "Errore nel prestito dalle ${parts[0]} alle ${parts[1]}"
        }
        return "Errore nella catena del prestito"
    }
    if (label.contains("borrow_value_error")) {
        return "Errore nella scrittura del prestito"
    }
    if (label.contains("borrow_target_error")) {
        return "Errore nel calcolo del numero dopo il prestito (${error.expected})"
    }
    if (label.contains("subtraction_calculation_error")) {
        return "Errore nel calcolo della sottrazione"
    }
    return "${error.stepLabel}: inserito ${error.actual}, corretto ${error.expected}"
}

internal fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
