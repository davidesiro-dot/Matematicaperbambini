package com.example.matematicaperbambini

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

internal fun printTeacherHomeworkCodes(context: Context, codes: List<TeacherHomeworkCode>) {
    if (codes.isEmpty()) return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = if (codes.size == 1) {
        "Codice_compito_${codes.first().code}"
    } else {
        "Codici_compiti_${codes.size}"
    }
    val adapter = TeacherHomeworkPrintAdapter(context, codes)
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

internal fun shareTeacherHomeworkCodes(context: Context, codes: List<TeacherHomeworkCode>) {
    if (codes.isEmpty()) return
    val pdfFile = createTeacherHomeworkPdf(context, codes) ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val subject = if (codes.size == 1) {
        "Codice compito"
    } else {
        "Codici compiti (${codes.size})"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Condividi codici")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
}

internal fun exportTeacherHomeworkCodes(context: Context, codes: List<TeacherHomeworkCode>) {
    if (codes.isEmpty()) return
    val pdfFile = createTeacherHomeworkPdf(context, codes) ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

internal fun createTeacherHomeworkPdf(context: Context, codes: List<TeacherHomeworkCode>): File? {
    if (codes.isEmpty()) return null
    val outputFile = File(context.cacheDir, teacherHomeworkPdfFileName(codes))
    val pdfDocument = PdfDocument()
    return try {
        renderTeacherHomeworkPdf(
            context = context,
            pdfDocument = pdfDocument,
            codes = codes,
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

private fun teacherHomeworkPdfFileName(codes: List<TeacherHomeworkCode>): String {
    return if (codes.size == 1) {
        "codice_compito_${codes.first().code}.pdf"
    } else {
        "codici_compiti_${codes.size}.pdf"
    }
}

private class TeacherHomeworkPrintAdapter(
    private val context: Context,
    private val codes: List<TeacherHomeworkCode>
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
        val info = PrintDocumentInfo.Builder(teacherHomeworkPdfFileName(codes))
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
            renderTeacherHomeworkPdf(
                context = context,
                pdfDocument = pdfDocument,
                codes = codes,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                cancellationSignal = cancellationSignal
            )
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

private fun renderTeacherHomeworkPdf(
    context: Context,
    pdfDocument: PdfDocument,
    codes: List<TeacherHomeworkCode>,
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
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = accentColor
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = 0xFF000000.toInt()
    }
    val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = 0xFF111827.toInt()
    }

    val logoBitmap = runCatching {
        BitmapFactory.decodeResource(context.resources, R.drawable.math_kids_logo)
    }.getOrNull()

    codes.forEachIndexed { index, code ->
        if (cancellationSignal?.isCanceled == true) return
        val pageNumber = index + 1
        val page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        val canvas = page.canvas
        var y = margin

        logoBitmap?.let { bitmap ->
            val desiredWidth = maxWidth * 0.45f
            val scale = desiredWidth / max(1f, bitmap.width.toFloat())
            val targetWidth = bitmap.width * scale
            val targetHeight = bitmap.height * scale
            val logoRect = RectF(margin, y, margin + targetWidth, y + targetHeight)
            canvas.drawBitmap(bitmap, null, logoRect, null)
            y += targetHeight + 12f
        }

        canvas.drawText("MateMatt – Codice Compito", margin, y + titlePaint.fontSpacing, titlePaint)
        y += titlePaint.fontSpacing * 2

        val codeText = code.code
        val codeWidth = codePaint.measureText(codeText)
        val codeX = (pageWidth - codeWidth) / 2f
        canvas.drawText(codeText, codeX, y + codePaint.fontSpacing, codePaint)
        y += codePaint.fontSpacing * 2

        canvas.drawText("Descrizione compito", margin, y + subtitlePaint.fontSpacing, subtitlePaint)
        y += subtitlePaint.fontSpacing * 1.4f
        val descriptionLines = wrapPdfText(code.description, bodyPaint, maxWidth)
        descriptionLines.forEach { line ->
            canvas.drawText(line, margin, y + bodyPaint.fontSpacing, bodyPaint)
            y += bodyPaint.fontSpacing
        }

        y += bodyPaint.fontSpacing
        canvas.drawText("Istruzioni rapide", margin, y + subtitlePaint.fontSpacing, subtitlePaint)
        y += subtitlePaint.fontSpacing * 1.4f
        val instruction =
            "Inserisci questo codice nell'Area Bambino per avviare il compito."
        wrapPdfText(instruction, bodyPaint, maxWidth).forEach { line ->
            canvas.drawText(line, margin, y + bodyPaint.fontSpacing, bodyPaint)
            y += bodyPaint.fontSpacing
        }

        y += bodyPaint.fontSpacing
        canvas.drawText("Data creazione: ${formatTimestamp(code.createdAt)}", margin, y + bodyPaint.fontSpacing, bodyPaint)

        pdfDocument.finishPage(page)
    }
}

private fun wrapPdfText(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (text.isBlank()) return listOf("")
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""
    for (word in words) {
        val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (paint.measureText(candidate) <= maxWidth) {
            currentLine = candidate
        } else {
            if (currentLine.isNotEmpty()) {
                lines += currentLine
            }
            currentLine = word
        }
    }
    if (currentLine.isNotEmpty()) {
        lines += currentLine
    }
    return lines
}

private fun milsToPoints(mils: Int): Int {
    return (mils / 1000f * 72f).toInt()
}
