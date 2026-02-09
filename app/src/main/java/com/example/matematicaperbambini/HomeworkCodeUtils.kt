package com.example.matematicaperbambini

import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.UUID

@Serializable
data class HomeworkCodePayload(
    val version: Int = 1,
    val id: String,
    val createdAt: Long,
    val tasks: List<HomeworkTaskConfig>
)

data class GeneratedHomeworkCode(
    val payload: HomeworkCodePayload,
    val code: String
)

private val homeworkCodeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private val homeworkCodeRandom = SecureRandom()

fun generateHomeworkCode(tasks: List<HomeworkTaskConfig>): GeneratedHomeworkCode {
    val payload = HomeworkCodePayload(
        id = UUID.randomUUID().toString(),
        createdAt = System.currentTimeMillis(),
        tasks = tasks
    )
    return GeneratedHomeworkCode(payload = payload, code = generateReadableHomeworkCode())
}

fun generateReadableHomeworkCode(): String {
    val raw = CharArray(8) {
        homeworkCodeAlphabet[homeworkCodeRandom.nextInt(homeworkCodeAlphabet.length)]
    }.concatToString()
    return "${raw.substring(0, 4)}-${raw.substring(4)}"
}

fun normalizeHomeworkCode(code: String): String {
    return code.trim().uppercase().filter { it.isLetterOrDigit() }
}

fun findHomeworkCodePayload(code: String, entries: List<HomeworkCodeEntry>): HomeworkCodePayload? {
    val normalized = normalizeHomeworkCode(code)
    if (normalized.length !in 6..10) return null
    val entry = entries.firstOrNull { normalizeHomeworkCode(it.code) == normalized } ?: return null
    if (entry.tasks.isEmpty()) return null
    return HomeworkCodePayload(
        id = entry.id,
        createdAt = entry.createdAt,
        tasks = entry.tasks
    )
}

fun formatHomeworkCodePreview(code: String): String {
    val clean = code.trim()
    if (clean.length <= 12) return clean
    val start = clean.take(8)
    val end = clean.takeLast(4)
    return "$start…$end"
}

fun buildHomeworkCodeDescription(tasks: List<HomeworkTaskConfig>): String {
    val totalExercises = tasks.sumOf { it.amount.exercisesCount * it.amount.repeatsPerExercise }
    val types = tasks.map { it.game.title }.distinct()
    return if (totalExercises > 0) {
        val typesLabel = if (types.isNotEmpty()) " (${types.joinToString(", ")})" else ""
        "Questo compito contiene $totalExercises esercizi$typesLabel"
    } else {
        val typesLabel = if (types.isNotEmpty()) " (${types.joinToString(", ")})" else ""
        "Questo compito contiene ${tasks.size} attività$typesLabel"
    }
}
