package com.example.matematicaperbambini

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.guidedPathDataStore: DataStore<Preferences> by preferencesDataStore(name = "guided_path_progress")

private val unlockedAdditionPathKey = intPreferencesKey("unlocked_addition_path")
private val unlockedSubtractionPathKey = intPreferencesKey("unlocked_subtraction_path")
private val unlockedMultiplicationPathKey = intPreferencesKey("unlocked_multiplication_path")
private val unlockedDivisionPathKey = intPreferencesKey("unlocked_division_path")
private val unlockedTimesTablePathKey = intPreferencesKey("unlocked_times_table_path")

enum class GuidedLessonCategory {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION,
    TIMES_TABLE
}

data class GuidedPathProgress(
    val unlockedAdditionPath: Int = 1,
    val unlockedSubtractionPath: Int = 1,
    val unlockedMultiplicationPath: Int = 1,
    val unlockedDivisionPath: Int = 1,
    val unlockedTimesTablePath: Int = 1
) {
    fun unlockedFor(category: GuidedLessonCategory): Int = when (category) {
        GuidedLessonCategory.ADDITION -> unlockedAdditionPath
        GuidedLessonCategory.SUBTRACTION -> unlockedSubtractionPath
        GuidedLessonCategory.MULTIPLICATION -> unlockedMultiplicationPath
        GuidedLessonCategory.DIVISION -> unlockedDivisionPath
        GuidedLessonCategory.TIMES_TABLE -> unlockedTimesTablePath
    }
}

class GuidedPathProgressRepository(private val context: Context) {
    val progressFlow: Flow<GuidedPathProgress> = context.guidedPathDataStore.data.map { prefs ->
        GuidedPathProgress(
            unlockedAdditionPath = prefs[unlockedAdditionPathKey] ?: 1,
            unlockedSubtractionPath = prefs[unlockedSubtractionPathKey] ?: 1,
            unlockedMultiplicationPath = prefs[unlockedMultiplicationPathKey] ?: 1,
            unlockedDivisionPath = prefs[unlockedDivisionPathKey] ?: 1,
            unlockedTimesTablePath = prefs[unlockedTimesTablePathKey] ?: 1
        )
    }

    suspend fun unlockNext(category: GuidedLessonCategory, nextUnlocked: Int) {
        context.guidedPathDataStore.edit { prefs ->
            when (category) {
                GuidedLessonCategory.ADDITION -> prefs[unlockedAdditionPathKey] = maxOf(prefs[unlockedAdditionPathKey] ?: 1, nextUnlocked)
                GuidedLessonCategory.SUBTRACTION -> prefs[unlockedSubtractionPathKey] = maxOf(prefs[unlockedSubtractionPathKey] ?: 1, nextUnlocked)
                GuidedLessonCategory.MULTIPLICATION -> prefs[unlockedMultiplicationPathKey] = maxOf(prefs[unlockedMultiplicationPathKey] ?: 1, nextUnlocked)
                GuidedLessonCategory.DIVISION -> prefs[unlockedDivisionPathKey] = maxOf(prefs[unlockedDivisionPathKey] ?: 1, nextUnlocked)
                GuidedLessonCategory.TIMES_TABLE -> prefs[unlockedTimesTablePathKey] = maxOf(prefs[unlockedTimesTablePathKey] ?: 1, nextUnlocked)
            }
        }
    }
}
