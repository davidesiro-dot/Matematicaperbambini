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
    private companion object {
        const val MAX_REPORTS = 300
    }

    // Persistenza locale dei report (DataStore): nessun backend, solo dispositivo.
    suspend fun saveReport(report: HomeworkReport) {
        val existing = loadReports()
        val limited = (listOf(report) + existing).take(MAX_REPORTS)
        persistReports(limited)
    }

    suspend fun loadReports(): List<HomeworkReport> {
        return try {
            context.homeworkReportDataStore.data
                .map { prefs -> prefs[reportsKey].orEmpty() }
                .map { json ->
                    try {
                        if (json.isBlank()) {
                            emptyList()
                        } else {
                            reportJson.decodeFromString<List<HomeworkReport>>(json)
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

    suspend fun deleteReports(reportsToDelete: List<HomeworkReport>) {
        if (reportsToDelete.isEmpty()) return
        val deleteKeys = reportsToDelete
            .map { "${it.childName}_${it.createdAt}" }
            .toSet()
        val remaining = loadReports().filterNot { report ->
            "${report.childName}_${report.createdAt}" in deleteKeys
        }
        persistReports(remaining)
    }

    private suspend fun persistReports(reports: List<HomeworkReport>) {
        try {
            val payload = reportJson.encodeToString(reports)
            context.homeworkReportDataStore.edit { prefs ->
                prefs[reportsKey] = payload
            }
        } catch (e: Exception) {
            // Ignora errori di persistenza per evitare crash.
        }
    }
}
