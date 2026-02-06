package com.example.matematicaperbambini

import kotlinx.serialization.Serializable

@Serializable
data class TeacherHomeworkCode(
    val code: String,
    val description: String,
    val createdAt: Long,
    val seed: Long
)

data class TeacherHomework(
    val id: String,
    val codice: String,
    val tipoEsercizio: String,
    val parametri: String,
    val seed: Long,
    val dataCreazione: Long
)

data class TeacherHomeworkDraft(
    val tipoEsercizio: String,
    val numeroDomande: Int,
    val difficoltaParametri: String
)

data class TeacherHomeworkParsed(
    val homework: TeacherHomework,
    val draft: TeacherHomeworkDraft
)

private const val TEACHER_DESCRIPTION_SEPARATOR = "§"

fun encodeTeacherDescription(draft: TeacherHomeworkDraft): String {
    return listOf(
        draft.tipoEsercizio,
        draft.numeroDomande.toString(),
        draft.difficoltaParametri
    ).joinToString(TEACHER_DESCRIPTION_SEPARATOR)
}

fun decodeTeacherDescription(description: String): TeacherHomeworkDraft? {
    val parts = description.split(TEACHER_DESCRIPTION_SEPARATOR)
    if (parts.size < 3) return null
    val numeroDomande = parts[1].toIntOrNull() ?: return null
    return TeacherHomeworkDraft(
        tipoEsercizio = parts[0],
        numeroDomande = numeroDomande,
        difficoltaParametri = parts.drop(2).joinToString(TEACHER_DESCRIPTION_SEPARATOR)
    )
}

fun parseTeacherHomeworkCode(code: TeacherHomeworkCode): TeacherHomeworkParsed? {
    val draft = decodeTeacherDescription(code.description) ?: return null
    val homework = TeacherHomework(
        id = code.code,
        codice = code.code,
        tipoEsercizio = draft.tipoEsercizio,
        parametri = "Domande: ${draft.numeroDomande} | ${draft.difficoltaParametri}",
        seed = code.seed,
        dataCreazione = code.createdAt
    )
    return TeacherHomeworkParsed(homework = homework, draft = draft)
}
