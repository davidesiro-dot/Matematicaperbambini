package com.example.matematicaperbambini

import android.content.Context
import android.content.Intent

fun shareHomeworkCode(context: Context, entry: HomeworkCodeEntry) {
    val shareText = buildString {
        append("Titolo: ${entry.title}\n")
        append("Codice: ${entry.code}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Codice compito")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Condividi codice"))
}
