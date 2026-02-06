package com.example.matematicaperbambini

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TeacherRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("teacher_repository", Context.MODE_PRIVATE)
    private val key = "teacher_homeworks"

    fun save(homework: TeacherHomework) {
        val updated = getAll().toMutableList()
        updated.add(homework)
        persist(updated)
    }

    fun getAll(): List<TeacherHomework> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val jsonArray = JSONArray(raw)
        return buildList {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                add(
                    TeacherHomework(
                        id = obj.getString("id"),
                        codice = obj.getString("codice"),
                        tipoEsercizio = obj.getString("tipoEsercizio"),
                        parametri = obj.getString("parametri"),
                        seed = obj.getLong("seed"),
                        dataCreazione = obj.getLong("dataCreazione")
                    )
                )
            }
        }
    }

    fun delete(ids: List<String>) {
        val remaining = getAll().filterNot { ids.contains(it.id) }
        persist(remaining)
    }

    private fun persist(items: List<TeacherHomework>) {
        val array = JSONArray()
        items.forEach { homework ->
            val obj = JSONObject()
            obj.put("id", homework.id)
            obj.put("codice", homework.codice)
            obj.put("tipoEsercizio", homework.tipoEsercizio)
            obj.put("parametri", homework.parametri)
            obj.put("seed", homework.seed)
            obj.put("dataCreazione", homework.dataCreazione)
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
