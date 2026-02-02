package com.example.matematicaperbambini

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.roundToInt

internal fun printHomeworkReport(context: Context, report: HomeworkReport) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Report_${report.childName}_${report.createdAt}"
    val adapter = HomeworkReportPrintAdapter(report)
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

private enum class LineStyle {
    TITLE,
    SECTION,
    BODY
}

private data class PrintLine(val text: String, val style: LineStyle)

private class HomeworkReportPrintAdapter(
    private val report: HomeworkReport
) : PrintDocumentAdapter() {
    private var printAttributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        printAttributes = newAttributes
        val info = PrintDocumentInfo.Builder("report_${report.createdAt}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val attributes = printAttributes
        val mediaSize = attributes?.mediaSize
        val pageWidth = mediaSize?.widthMils?.let { milsToPoints(it) } ?: 595
        val pageHeight = mediaSize?.heightMils?.let { milsToPoints(it) } ?: 842

        val pdfDocument = PdfDocument()
        try {
            val lines = buildReportLines(report)
            renderLines(pdfDocument, lines, pageWidth, pageHeight, cancellationSignal)
            if (!cancellationSignal.isCanceled) {
                FileOutputStream(destination.fileDescriptor).use { output ->
                    pdfDocument.writeTo(output)
                }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } else {
                callback.onWriteCancelled()
            }
        } catch (error: Exception) {
            callback.onWriteFailed(error.message)
        } finally {
            pdfDocument.close()
        }
    }

    private fun renderLines(
        pdfDocument: PdfDocument,
        lines: List<PrintLine>,
        pageWidth: Int,
        pageHeight: Int,
        cancellationSignal: CancellationSignal
    ) {
        val margin = 40f
        val maxWidth = pageWidth - margin * 2
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var pageNumber = 1
        var page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = page.canvas
        var y = margin

        fun ensureSpace(lineHeight: Float) {
            if (y + lineHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber += 1
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin
            }
        }

        lines.forEach { line ->
            if (cancellationSignal.isCanceled) return
            val paint = when (line.style) {
                LineStyle.TITLE -> titlePaint
                LineStyle.SECTION -> sectionPaint
                LineStyle.BODY -> bodyPaint
            }
            val lineHeight = paint.fontSpacing
            if (line.text.isBlank()) {
                y += lineHeight * 0.6f
                return@forEach
            }
            val wrappedLines = wrapText(line.text, paint, maxWidth)
            wrappedLines.forEach { wrapped ->
                ensureSpace(lineHeight)
                canvas.drawText(wrapped, margin, y, paint)
                y += lineHeight
            }
        }
        pdfDocument.finishPage(page)
    }
}

private fun buildReportLines(report: HomeworkReport): List<PrintLine> {
    val lines = mutableListOf<PrintLine>()
    val perfectCount = report.results.count { it.outcome() == ExerciseOutcome.PERFECT }
    val withErrorsCount = report.results.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
    val wrongCount = report.results.size - perfectCount - withErrorsCount
    val homeworkTypes = report.results.map { it.instance.game.title }.distinct().ifEmpty { listOf("Compito") }

    lines += PrintLine("Report Compiti", LineStyle.TITLE)
    lines += PrintLine("Bambino: ${report.childName}", LineStyle.BODY)
    lines += PrintLine("Data e ora: ${formatTimestamp(report.createdAt)}", LineStyle.BODY)
    lines += PrintLine("Titolo compito: Compito di matematica", LineStyle.BODY)
    lines += PrintLine("Tipo di esercizi: ${homeworkTypes.joinToString(", ")}", LineStyle.BODY)
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Riepilogo", LineStyle.SECTION)
    lines += PrintLine("Totale esercizi: ${report.results.size}", LineStyle.BODY)
    lines += PrintLine("Corretto: $perfectCount", LineStyle.BODY)
    lines += PrintLine("Completato con errori: $withErrorsCount", LineStyle.BODY)
    lines += PrintLine("Sbagliato: $wrongCount", LineStyle.BODY)
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Dettaglio esercizi", LineStyle.SECTION)
    report.results.forEachIndexed { index, result ->
        val outcome = when (result.outcome()) {
            ExerciseOutcome.PERFECT -> "Corretto"
            ExerciseOutcome.COMPLETED_WITH_ERRORS -> "Completato con errori (⚠️)"
            ExerciseOutcome.FAILED -> "Sbagliato"
        }
        lines += PrintLine("Esercizio ${index + 1}: ${exerciseLabel(result.instance)}", LineStyle.BODY)
        lines += PrintLine("Esito finale: $outcome", LineStyle.BODY)
        lines += PrintLine("Numero di tentativi: ${result.attempts}", LineStyle.BODY)
        if (result.wrongAnswers.isNotEmpty()) {
            lines += PrintLine("Risposte errate: ${result.wrongAnswers.joinToString(", ")}", LineStyle.BODY)
        }
        if (result.stepErrors.isNotEmpty()) {
            lines += PrintLine("Passaggi da rinforzare:", LineStyle.BODY)
            result.stepErrors.forEach { error ->
                lines += PrintLine("• ${stepErrorDescription(error)}", LineStyle.BODY)
            }
        }
        lines += PrintLine(
            if (result.solutionUsed) {
                "Soluzione guidata usata: sì"
            } else {
                "Soluzione guidata usata: no"
            },
            LineStyle.BODY
        )
        lines += PrintLine("", LineStyle.BODY)
    }

    val patterns = analyzeErrorPatterns(report.results)
    val suggestions = suggestionsForPatterns(patterns)

    lines += PrintLine("Recurring difficulties detected", LineStyle.SECTION)
    if (patterns.isEmpty()) {
        lines += PrintLine("Nessuna difficoltà ricorrente rilevata.", LineStyle.BODY)
    } else {
        patterns.take(3).forEach { pattern ->
            lines += PrintLine("• ${pattern.category}", LineStyle.BODY)
        }
    }
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Suggested focus for practice", LineStyle.SECTION)
    if (suggestions.isEmpty()) {
        lines += PrintLine("Nessun suggerimento disponibile al momento.", LineStyle.BODY)
    } else {
        suggestions.forEach { suggestion ->
            lines += PrintLine("• $suggestion", LineStyle.BODY)
        }
    }

    return lines
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val words = text.split(" ")
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    words.forEach { word ->
        val tentative = if (current.isEmpty()) word else "${current} $word"
        if (paint.measureText(tentative) <= maxWidth) {
            current = StringBuilder(tentative)
        } else {
            if (current.isNotEmpty()) {
                lines += current.toString()
            }
            if (paint.measureText(word) > maxWidth) {
                lines += breakLongWord(word, paint, maxWidth)
                current = StringBuilder()
            } else {
                current = StringBuilder(word)
            }
        }
    }
    if (current.isNotEmpty()) {
        lines += current.toString()
    }
    return lines
}

private fun breakLongWord(word: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    if (word.isBlank()) return lines
    val maxChars = ceil(word.length / 2.0).toInt().coerceAtLeast(1)
    var start = 0
    while (start < word.length) {
        var end = (start + maxChars).coerceAtMost(word.length)
        while (end < word.length && paint.measureText(word.substring(start, end)) > maxWidth) {
            end -= 1
        }
        if (end == start) {
            end = (start + 1).coerceAtMost(word.length)
        }
        lines += word.substring(start, end)
        start = end
    }
    return lines
}

private fun milsToPoints(mils: Int): Int {
    return (mils / 1000f * 72f).roundToInt()
}
