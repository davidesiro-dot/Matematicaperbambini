package com.example.matematicaperbambini

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val homeworkCodeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

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

fun generateHomeworkCode(tasks: List<HomeworkTaskConfig>): GeneratedHomeworkCode {
    val payload = HomeworkCodePayload(
        id = UUID.randomUUID().toString(),
        createdAt = System.currentTimeMillis(),
        tasks = tasks
    )
    return GeneratedHomeworkCode(payload = payload, code = encodeHomeworkCode(payload))
}

fun encodeHomeworkCode(payload: HomeworkCodePayload): String {
    val json = homeworkCodeJson.encodeToString(payload)
    val encoded = Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP
    )
    return "HW1-$encoded"
}

fun decodeHomeworkCode(code: String): HomeworkCodePayload? {
    val trimmed = code.trim()
    if (trimmed.isBlank()) return null
    val payloadPart = trimmed.removePrefix("HW1-")
    if (payloadPart.isBlank()) return null
    val json = try {
        val decoded = Base64.decode(payloadPart, Base64.URL_SAFE or Base64.NO_WRAP)
        String(decoded, Charsets.UTF_8)
    } catch (e: Exception) {
        return null
    }
    return try {
        val payload = homeworkCodeJson.decodeFromString<HomeworkCodePayload>(json)
        if (payload.tasks.isEmpty()) null else payload
    } catch (e: Exception) {
        null
    }
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
