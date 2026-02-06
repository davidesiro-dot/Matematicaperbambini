package com.example.matematicaperbambini

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

fun exportTeacherHomeworkPdf(context: Context, homework: TeacherHomework, draft: TeacherHomeworkDraft) {
    val file = createTeacherHomeworkPdf(context, homework, draft)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

fun shareTeacherHomeworkPdf(context: Context, homework: TeacherHomework, draft: TeacherHomeworkDraft) {
    val file = createTeacherHomeworkPdf(context, homework, draft)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Condividi compito"))
}

fun printTeacherHomeworkPdf(context: Context, homework: TeacherHomework, draft: TeacherHomeworkDraft) {
    val file = createTeacherHomeworkPdf(context, homework, draft)
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Compito ${homework.codice}"
    val adapter = TeacherPdfPrintAdapter(file)
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

private fun createTeacherHomeworkPdf(
    context: Context,
    homework: TeacherHomework,
    draft: TeacherHomeworkDraft
): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    drawTeacherPdfContent(page.canvas, homework, draft)
    document.finishPage(page)

    val outputFile = File(context.cacheDir, "compito_${homework.codice}.pdf")
    FileOutputStream(outputFile).use { stream ->
        document.writeTo(stream)
    }
    document.close()
    return outputFile
}

private fun drawTeacherPdfContent(canvas: Canvas, homework: TeacherHomework, draft: TeacherHomeworkDraft) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.BLACK

    val left = 48f
    var y = 80f

    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText("Mate Matt", left, y, paint)

    paint.isFakeBoldText = false
    paint.textSize = 14f
    canvas.drawText("(logo placeholder)", left, y + 22f, paint)

    y += 70f
    paint.textSize = 26f
    paint.isFakeBoldText = true
    canvas.drawText("Compito", left, y, paint)

    y += 50f
    paint.textSize = 36f
    paint.isFakeBoldText = true
    canvas.drawText(homework.codice, left, y, paint)

    y += 40f
    paint.textSize = 16f
    paint.isFakeBoldText = false
    canvas.drawText("Tipo esercizio: ${draft.tipoEsercizio}", left, y, paint)
    y += 24f
    canvas.drawText("Numero domande: ${draft.numeroDomande}", left, y, paint)
    y += 24f
    canvas.drawText("Difficoltà: ${draft.difficoltaParametri}", left, y, paint)

    y += 50f
    paint.textSize = 14f
    canvas.drawText("Inserisci questo codice nell’app Mate Matt", left, y, paint)
}

private class TeacherPdfPrintAdapter(
    private val file: File
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>?,
        destination: android.os.ParcelFileDescriptor?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination?.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (ex: IOException) {
            callback?.onWriteFailed(ex.message)
        }
    }
}
