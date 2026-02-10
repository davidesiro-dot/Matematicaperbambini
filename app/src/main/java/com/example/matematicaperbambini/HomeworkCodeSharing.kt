package com.example.matematicaperbambini

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log

private const val SHARE_HOMEWORK_TAG = "HomeworkCodeSharing"

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
    val chooser = Intent.createChooser(intent, "Condividi codice")
    val canHandle = intent.resolveActivity(context.packageManager) != null ||
        chooser.resolveActivity(context.packageManager) != null
    if (!canHandle) {
        Log.w(SHARE_HOMEWORK_TAG, "No activity can handle homework code sharing")
        return
    }
    try {
        context.startActivity(chooser)
    } catch (error: ActivityNotFoundException) {
        Log.w(SHARE_HOMEWORK_TAG, "Unable to open share chooser", error)
    } catch (error: SecurityException) {
        Log.w(SHARE_HOMEWORK_TAG, "Unable to share homework code due to security policy", error)
    }
}
