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
    printHomeworkReports(context, listOf(report))
}

internal fun printHomeworkReports(context: Context, reports: List<HomeworkReport>) {
    if (reports.isEmpty()) return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = if (reports.size == 1) {
        val report = reports.first()
        "Report_${report.childName}_${report.createdAt}"
    } else {
        "Report_compiti_${reports.size}"
    }
    val adapter = HomeworkReportPrintAdapter(reports)
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

private enum class LineStyle {
    TITLE,
    SECTION,
    BODY,
    PAGE_BREAK
}

private data class PrintLine(val text: String, val style: LineStyle)

private class HomeworkReportPrintAdapter(
    private val reports: List<HomeworkReport>
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
        val info = PrintDocumentInfo.Builder("report_compiti.pdf")
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
            val lines = buildReportLines(reports)
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
            if (line.style == LineStyle.PAGE_BREAK) {
                pdfDocument.finishPage(page)
                pageNumber += 1
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin
                return@forEach
            }

            val paint = when (line.style) {
                LineStyle.TITLE -> titlePaint
                LineStyle.SECTION -> sectionPaint
                LineStyle.BODY -> bodyPaint
                LineStyle.PAGE_BREAK -> bodyPaint
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

private fun buildReportLines(reports: List<HomeworkReport>): List<PrintLine> {
    val lines = mutableListOf<PrintLine>()
    reports.forEachIndexed { index, report ->
        lines += buildSingleReportLines(report, index, reports.size)
        if (index < reports.lastIndex) {
            lines += PrintLine("", LineStyle.PAGE_BREAK)
        }
    }
    return lines
}

private fun buildSingleReportLines(report: HomeworkReport, index: Int, totalReports: Int): List<PrintLine> {
    val lines = mutableListOf<PrintLine>()
    val perfectCount = report.results.count { it.outcome() == ExerciseOutcome.PERFECT }
    val withErrorsCount = report.results.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
    val wrongCount = report.results.size - perfectCount - withErrorsCount
    val durationMillis = report.results.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0) }
    val solutionUsedCount = report.results.count { it.solutionUsed }
    val homeworkTypes = report.results.map { it.instance.game.title }.distinct().ifEmpty { listOf("Compito") }

    val title = if (totalReports > 1) {
        "Report Compiti ${index + 1} di $totalReports"
    } else {
        "Report Compiti"
    }
    lines += PrintLine(title, LineStyle.TITLE)
    lines += PrintLine("Bambino: ${report.childName}", LineStyle.BODY)
    lines += PrintLine("Data e ora: ${formatTimestamp(report.createdAt)}", LineStyle.BODY)
    lines += PrintLine("Durata sessione: ${formatDurationMillis(durationMillis)}", LineStyle.BODY)
    lines += PrintLine("Modalità: Compiti", LineStyle.BODY)
    lines += PrintLine("Titolo compito: Compito di matematica", LineStyle.BODY)
    lines += PrintLine("Tipo di esercizi: ${homeworkTypes.joinToString(", ")}", LineStyle.BODY)
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Riepilogo", LineStyle.SECTION)
    lines += PrintLine("Totale esercizi: ${report.results.size}", LineStyle.BODY)
    lines += PrintLine("Corretto: $perfectCount", LineStyle.BODY)
    lines += PrintLine("Completato con errori (⚠️): $withErrorsCount", LineStyle.BODY)
    lines += PrintLine("Da ripassare: $wrongCount", LineStyle.BODY)
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Aiuti usati durante la sessione", LineStyle.SECTION)
    lines += PrintLine("Suggerimenti: non registrati nei report salvati", LineStyle.BODY)
    lines += PrintLine("Evidenziazioni: non registrate nei report salvati", LineStyle.BODY)
    lines += PrintLine("Soluzione guidata: $solutionUsedCount utilizzi", LineStyle.BODY)
    lines += PrintLine("Auto-check: non registrato nei report salvati", LineStyle.BODY)
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Dettaglio esercizi", LineStyle.SECTION)
    report.results.forEachIndexed { index, result ->
        val outcome = when (result.outcome()) {
            ExerciseOutcome.PERFECT -> "Corretto"
            ExerciseOutcome.COMPLETED_WITH_ERRORS -> "Completato con errori (⚠️)"
            ExerciseOutcome.FAILED -> "Sbagliato"
        }
        lines += PrintLine("Esercizio ${index + 1}: ${exerciseLabel(result.instance)}", LineStyle.BODY)
        lines += PrintLine("Tipo di gioco: ${result.instance.game.title}", LineStyle.BODY)
        lines += PrintLine("Esito finale: $outcome", LineStyle.BODY)
        lines += PrintLine("Numero di tentativi: ${result.attempts}", LineStyle.BODY)
        lines += PrintLine(
            "Tempo impiegato: ${formatDurationMillis(result.endedAt - result.startedAt)}",
            LineStyle.BODY
        )
        if (result.wrongAnswers.isNotEmpty()) {
            lines += PrintLine("Risposte errate: ${result.wrongAnswers.joinToString(", ")}", LineStyle.BODY)
        }
        if (result.stepErrors.isNotEmpty()) {
            lines += PrintLine("Passaggi con errore da rinforzare:", LineStyle.BODY)
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

    lines += PrintLine("Errori commessi nella sessione", LineStyle.SECTION)
    if (patterns.isEmpty()) {
        lines += PrintLine("Nessun errore rilevato.", LineStyle.BODY)
    } else {
        patterns.forEach { pattern ->
            lines += PrintLine("• ${pattern.category} (${pattern.occurrences})", LineStyle.BODY)
        }
    }
    lines += PrintLine("", LineStyle.BODY)

    lines += PrintLine("Errori più frequenti", LineStyle.SECTION)
    if (patterns.isEmpty()) {
        lines += PrintLine("Nessun errore frequente rilevato.", LineStyle.BODY)
    } else {
        patterns.take(3).forEach { pattern ->
            lines += PrintLine("• ${pattern.category} (${pattern.occurrences})", LineStyle.BODY)
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
