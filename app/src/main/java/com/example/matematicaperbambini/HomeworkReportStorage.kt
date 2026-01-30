package com.example.matematicaperbambini

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private val Context.homeworkReportDataStore: DataStore<Preferences> by preferencesDataStore(name = "homework_reports")

private val reportsKey = stringPreferencesKey("reports_json")

private val reportJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class HomeworkReportStorage(private val context: Context) {
    // Persistenza locale dei report (DataStore): nessun backend, solo dispositivo.
    suspend fun saveReport(report: HomeworkReport) {
        val existing = loadReports().toMutableList()
        existing.add(0, report)
        persistReports(existing)
    }

    suspend fun loadReports(): List<HomeworkReport> {
        return context.homeworkReportDataStore.data
            .map { prefs -> prefs[reportsKey].orEmpty() }
            .map { json ->
                if (json.isBlank()) emptyList() else reportJson.decodeFromString<List<HomeworkReport>>(json)
            }
            .first()
    }

    private suspend fun persistReports(reports: List<HomeworkReport>) {
        val payload = reportJson.encodeToString(reports)
        context.homeworkReportDataStore.edit { prefs ->
            prefs[reportsKey] = payload
        }
    }
}
