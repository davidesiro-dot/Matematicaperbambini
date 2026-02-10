package com.example.matematicaperbambini

import android.content.Context
import android.util.Log
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

private val Context.homeworkCodeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "homework_codes"
)

private val homeworkCodesKey = stringPreferencesKey("homework_codes_json")

private val homeworkCodeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class HomeworkCodeRepository(private val context: Context) {
    private companion object {
        const val TAG = "HomeworkCodeRepository"
    }

    suspend fun save(code: HomeworkCodeEntry) {
        val existing = getAll().filterNot { it.id == code.id }
        persist(listOf(code) + existing)
    }

    suspend fun getAll(): List<HomeworkCodeEntry> {
        return try {
            context.homeworkCodeDataStore.data
                .map { prefs -> prefs[homeworkCodesKey].orEmpty() }
                .map { json ->
                    try {
                        if (json.isBlank()) {
                            emptyList()
                        } else {
                            homeworkCodeJson.decodeFromString<List<HomeworkCodeEntry>>(json)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid homework code payload, returning empty list", e)
                        emptyList()
                    }
                }
                .first()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to load homework codes, returning empty list", e)
            emptyList()
        }
    }

    suspend fun delete(id: String) {
        val remaining = getAll().filterNot { it.id == id }
        persist(remaining)
    }

    private suspend fun persist(codes: List<HomeworkCodeEntry>) {
        try {
            val payload = homeworkCodeJson.encodeToString(codes)
            context.homeworkCodeDataStore.edit { prefs ->
                prefs[homeworkCodesKey] = payload
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to persist homework codes, keeping runtime data only", e)
        }
    }
}
