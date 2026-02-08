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
import java.io.File
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

internal fun createHomeworkReportPdf(context: Context, reports: List<HomeworkReport>): File? {
    if (reports.isEmpty()) return null
    val fileName = reportFileName(reports)
    val outputFile = File(context.cacheDir, fileName)
    val pdfDocument = PdfDocument()
    return try {
        val lines = buildReportLines(reports)
        renderLines(
            pdfDocument = pdfDocument,
            lines = lines,
            pageWidth = 595,
            pageHeight = 842,
            cancellationSignal = null
        )
        FileOutputStream(outputFile).use { output ->
            pdfDocument.writeTo(output)
        }
        outputFile
    } catch (error: Exception) {
        null
    } finally {
        pdfDocument.close()
    }
}

private enum class TextStyle {
    TITLE,
    SECTION,
    BODY,
    SUBTLE
}

private sealed class PdfLine {
    data class TextLine(val text: String, val style: TextStyle) : PdfLine()
    data object DividerLine : PdfLine()
    data object PageBreakLine : PdfLine()
    data class TableLine(val columns: List<String>, val isHeader: Boolean) : PdfLine()
}

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
        val info = PrintDocumentInfo.Builder(reportFileName(reports))
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

}

private fun renderLines(
    pdfDocument: PdfDocument,
    lines: List<PdfLine>,
    pageWidth: Int,
    pageHeight: Int,
    cancellationSignal: CancellationSignal?
) {
    val margin = 40f
    val maxWidth = pageWidth - margin * 2
    val accentColor = 0xFF1E5AA8.toInt()
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = accentColor
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = accentColor
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = 0xFF000000.toInt()
    }
    val subtlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = 0xFF000000.toInt()
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        color = accentColor
    }

    val tableFractions = listOf(0.08f, 0.42f, 0.2f, 0.15f, 0.15f)
    val tableWidths = tableFractions.map { it * maxWidth }
    val tableStarts = tableWidths.runningFold(margin) { acc, width -> acc + width }.dropLast(1)

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

    fun drawTextLine(text: String, paint: Paint) {
        val lineHeight = paint.fontSpacing
        if (text.isBlank()) {
            y += lineHeight * 0.6f
            return
        }
        wrapText(text, paint, maxWidth).forEach { wrapped ->
            ensureSpace(lineHeight)
            canvas.drawText(wrapped, margin, y, paint)
            y += lineHeight
        }
    }

    fun drawTableLine(line: PdfLine.TableLine) {
        val paint = if (line.isHeader) {
            Paint(bodyPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        } else {
            bodyPaint
        }
        val lineHeight = paint.fontSpacing
        val wrappedColumns = line.columns.mapIndexed { index, text ->
            wrapText(text, paint, tableWidths[index])
        }
        val maxLines = wrappedColumns.maxOfOrNull { it.size } ?: 1
        val rowHeight = lineHeight * maxLines
        ensureSpace(rowHeight)
        for (rowIndex in 0 until maxLines) {
            tableStarts.forEachIndexed { columnIndex, startX ->
                val columnLines = wrappedColumns[columnIndex]
                val text = columnLines.getOrNull(rowIndex) ?: ""
                canvas.drawText(text, startX, y, paint)
            }
            y += lineHeight
        }
        y += lineHeight * 0.2f
    }

    lines.forEach { line ->
        if (cancellationSignal?.isCanceled == true) return
        when (line) {
            is PdfLine.PageBreakLine -> {
                pdfDocument.finishPage(page)
                pageNumber += 1
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin
            }
            is PdfLine.DividerLine -> {
                val lineHeight = bodyPaint.fontSpacing
                ensureSpace(lineHeight)
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += lineHeight
            }
            is PdfLine.TextLine -> {
                val paint = when (line.style) {
                    TextStyle.TITLE -> titlePaint
                    TextStyle.SECTION -> sectionPaint
                    TextStyle.BODY -> bodyPaint
                    TextStyle.SUBTLE -> subtlePaint
                }
                drawTextLine(line.text, paint)
            }
            is PdfLine.TableLine -> {
                drawTableLine(line)
            }
        }
    }
    pdfDocument.finishPage(page)
}

private fun buildReportLines(reports: List<HomeworkReport>): List<PdfLine> {
    val lines = mutableListOf<PdfLine>()
    reports.forEachIndexed { index, report ->
        lines += buildSingleReportLines(report, index, reports.size)
        if (index < reports.lastIndex) {
            lines += PdfLine.PageBreakLine
        }
    }
    return lines
}

private fun buildSingleReportLines(report: HomeworkReport, index: Int, totalReports: Int): List<PdfLine> {
    val lines = mutableListOf<PdfLine>()
    val completedExercises = if (report.totalExercises > 0) {
        report.completedExercises
    } else {
        report.results.size
    }
    val plannedTotal = if (report.totalExercises > 0) {
        report.totalExercises
    } else {
        completedExercises
    }
    val perfectCount = report.results.count { it.outcome() == ExerciseOutcome.PERFECT }
    val withErrorsCount = report.results.count { it.outcome() == ExerciseOutcome.COMPLETED_WITH_ERRORS }
    val wrongCount = completedExercises - perfectCount - withErrorsCount
    val durationMillis = report.results.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0) }
    val homeworkTypes = report.results.map { it.instance.game.title }.distinct().ifEmpty { listOf("Compito") }
    val hasErrors = report.results.any { it.outcome() != ExerciseOutcome.PERFECT }

    lines += PdfLine.TextLine("MateMatt – Report Compiti", TextStyle.TITLE)
    val date = formatReportDate(report.createdAt)
    val time = formatReportTime(report.createdAt)
    lines += PdfLine.TextLine("Data: $date   Ora: $time", TextStyle.SUBTLE)
    lines += PdfLine.TextLine("Bambino: ${report.childName}", TextStyle.SUBTLE)
    lines += PdfLine.DividerLine
    if (totalReports > 1) {
        lines += PdfLine.TextLine("Report ${index + 1} di $totalReports", TextStyle.SUBTLE)
    }
    lines += PdfLine.TextLine("", TextStyle.BODY)

    lines += PdfLine.TextLine("Riepilogo generale", TextStyle.SECTION)
    if (report.interrupted) {
        lines += PdfLine.TextLine("⚠ Compito interrotto prima del completamento", TextStyle.BODY)
    }
    lines += PdfLine.TextLine("Totale esercizi: $plannedTotal", TextStyle.BODY)
    lines += PdfLine.TextLine("Esercizi corretti: $perfectCount", TextStyle.BODY)
    lines += PdfLine.TextLine("Completati con errori: $withErrorsCount", TextStyle.BODY)
    lines += PdfLine.TextLine("Da ripassare: $wrongCount", TextStyle.BODY)
    lines += PdfLine.TextLine("Durata totale del compito: ${formatDurationMillis(durationMillis)}", TextStyle.BODY)
    lines += PdfLine.TextLine("", TextStyle.BODY)

    lines += PdfLine.TextLine("Tipologie svolte", TextStyle.SECTION)
    homeworkTypes.forEach { type ->
        lines += PdfLine.TextLine("• $type", TextStyle.BODY)
    }
    lines += PdfLine.TextLine("", TextStyle.BODY)

    if (hasErrors) {
        lines += PdfLine.TextLine("Dettaglio degli errori", TextStyle.SECTION)
        report.results.forEachIndexed { resultIndex, result ->
            if (result.outcome() == ExerciseOutcome.PERFECT) return@forEachIndexed
            lines += PdfLine.TextLine(
                "Esercizio ${resultIndex + 1} – ${result.instance.game.title}",
                TextStyle.BODY
            )
            lines += PdfLine.TextLine("Operazione: ${exerciseLabel(result.instance)}", TextStyle.BODY)
            if (result.wrongAnswers.isNotEmpty()) {
                val expected = expectedAnswer(result.instance)
                lines += PdfLine.TextLine("Risposte errate:", TextStyle.BODY)
                lines += PdfLine.TextLine("• ${result.wrongAnswers.joinToString()}", TextStyle.BODY)
                lines += PdfLine.TextLine(
                    "• Risposta corretta: ${expected ?: "non disponibile"}",
                    TextStyle.BODY
                )
            } else if (!result.correct) {
                val expected = expectedAnswer(result.instance)
                lines += PdfLine.TextLine(
                    "Risposta corretta: ${expected ?: "non disponibile"}",
                    TextStyle.BODY
                )
            }
            if (result.stepErrors.isNotEmpty()) {
                lines += PdfLine.TextLine("Errori nei passaggi:", TextStyle.BODY)
                result.stepErrors.forEach { error ->
                    lines += PdfLine.TextLine("• ${stepErrorDescription(error)}", TextStyle.BODY)
                }
            }
            lines += PdfLine.TextLine("", TextStyle.BODY)
        }
    }

    lines += PdfLine.TextLine("Dettaglio completo degli esercizi", TextStyle.SECTION)
    lines += PdfLine.TableLine(
        listOf("#", "Operazione", "Esito", "Tentativi", "Tempo"),
        isHeader = true
    )
    report.results.forEachIndexed { index, result ->
        val outcome = when (result.outcome()) {
            ExerciseOutcome.PERFECT -> "✔"
            ExerciseOutcome.COMPLETED_WITH_ERRORS -> "⚠"
            ExerciseOutcome.FAILED -> "✖"
        }
        lines += PdfLine.TableLine(
            listOf(
                "${index + 1}",
                exerciseLabel(result.instance),
                outcome,
                result.attempts.toString(),
                formatDurationMillis(result.endedAt - result.startedAt)
            ),
            isHeader = false
        )
    }
    lines += PdfLine.TextLine(
        "Legenda: ✔ corretto   ⚠ corretto con errori   ✖ da ripassare",
        TextStyle.SUBTLE
    )
    lines += PdfLine.TextLine("", TextStyle.BODY)

    lines += PdfLine.TextLine("Report generato con MateMatt", TextStyle.SUBTLE)

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

private fun reportFileName(reports: List<HomeworkReport>): String {
    val report = reports.first()
    val safeName = report.childName
        .ifBlank { "Bambino" }
        .replace(Regex("[^A-Za-z0-9_-]"), "_")
    val date = formatReportFilenameDate(report.createdAt)
    return "MateMatt_Report_${safeName}_$date.pdf"
}

private fun milsToPoints(mils: Int): Int {
    return (mils / 1000f * 72f).roundToInt()
}
