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

private val Context.savedHomeworkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "saved_homeworks"
)

private val savedHomeworksKey = stringPreferencesKey("saved_homeworks_json")

private val savedHomeworkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class SavedHomeworkRepository(private val context: Context) {
    private companion object {
        const val TAG = "SavedHomeworkRepository"
    }

    suspend fun save(savedHomework: SavedHomework) {
        val existing = getAll().filterNot { it.id == savedHomework.id }
        persist(listOf(savedHomework) + existing)
    }

    suspend fun getAll(): List<SavedHomework> {
        return try {
            context.savedHomeworkDataStore.data
                .map { prefs -> prefs[savedHomeworksKey].orEmpty() }
                .map { json ->
                    try {
                        if (json.isBlank()) {
                            emptyList()
                        } else {
                            savedHomeworkJson.decodeFromString<List<SavedHomework>>(json)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid saved homework payload, returning empty list", e)
                        emptyList()
                    }
                }
                .first()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to load saved homeworks, returning empty list", e)
            emptyList()
        }
    }

    suspend fun delete(id: String) {
        val remaining = getAll().filterNot { it.id == id }
        persist(remaining)
    }

    private suspend fun persist(homeworks: List<SavedHomework>) {
        try {
            val payload = savedHomeworkJson.encodeToString(homeworks)
            context.savedHomeworkDataStore.edit { prefs ->
                prefs[savedHomeworksKey] = payload
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to persist saved homeworks, keeping runtime data only", e)
        }
    }
}
