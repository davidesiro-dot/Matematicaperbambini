package com.example.matematicaperbambini

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.teacherHomeworkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "teacher_homework_codes"
)

private val teacherHomeworkKey = stringPreferencesKey("teacher_homework_codes_json")

private val teacherHomeworkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class TeacherHomeworkRepository(private val context: Context) {
    suspend fun save(code: TeacherHomeworkCode) {
        val existing = getAll().filterNot { it.code == code.code }
        persist(listOf(code) + existing)
    }

    suspend fun getAll(): List<TeacherHomeworkCode> {
        return try {
            context.teacherHomeworkDataStore.data
                .map { prefs -> prefs[teacherHomeworkKey].orEmpty() }
                .map { json ->
                    try {
                        if (json.isBlank()) {
                            emptyList()
                        } else {
                            teacherHomeworkJson.decodeFromString<List<TeacherHomeworkCode>>(json)
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                .first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun delete(codes: List<String>) {
        val remaining = getAll().filterNot { it.code in codes }
        persist(remaining)
    }

    private suspend fun persist(codes: List<TeacherHomeworkCode>) {
        try {
            val payload = teacherHomeworkJson.encodeToString(codes)
            context.teacherHomeworkDataStore.edit { prefs ->
                prefs[teacherHomeworkKey] = payload
            }
        } catch (e: Exception) {
            // Ignora errori di persistenza per evitare crash.
        }
    }
}
