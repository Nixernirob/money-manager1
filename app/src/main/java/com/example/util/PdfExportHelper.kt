package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import com.example.data.db.Account
import com.example.data.db.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    data class ExportSummary(
        val filterSummary: String,
        val currency: String,
        val totalIncome: Double,
        val totalExpense: Double,
        val netBalance: Double
    )

    fun exportTransactionsToPdf(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        accounts: List<Account>,
        summary: ExportSummary
    ): Result<Int> {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val marginH = 28f
            val marginTop = 32f
            val marginBottom = 36f
            val usableWidth = pageWidth - (marginH * 2f)

            val accountMap = accounts.associateBy { it.id }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val rowDateFormat = SimpleDateFormat("dd MMM yy HH:mm", Locale.getDefault())

            // Column widths summing to 539f (595 - 56)
            val colWidths = floatArrayOf(78f, 156f, 78f, 88f, 57f, 82f)
            val colHeaders = arrayOf("Date", "Title / Note", "Category", "Vault", "Type", "Amount")

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val summaryBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            val summaryBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val summaryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111827")
                style = Paint.Style.FILL
            }
            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rowEvenBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val rowOddBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 0.6f
            }
            val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val amountTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 7.5f
            }

            fun drawTableHeader(canvas: Canvas, topY: Float): Float {
                val headerHeight = 24f
                canvas.drawRect(marginH, topY, marginH + usableWidth, topY + headerHeight, headerBgPaint)
                var x = marginH
                for (i in colHeaders.indices) {
                    val colW = colWidths[i]
                    if (i == colHeaders.lastIndex) {
                        headerTextPaint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(colHeaders[i], x + colW - 6f, topY + 15.5f, headerTextPaint)
                        headerTextPaint.textAlign = Paint.Align.LEFT
                    } else {
                        canvas.drawText(colHeaders[i], x + 6f, topY + 15.5f, headerTextPaint)
                    }
                    x += colW
                }
                return topY + headerHeight
            }

            fun drawFooter(canvas: Canvas, pageNum: Int) {
                val y = pageHeight - 16f
                footerPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("Aureum Wallet • Exported Statement", marginH, y, footerPaint)
                footerPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Page $pageNum", marginH + usableWidth, y, footerPaint)
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Page 1 Header Block
            var cursorY = marginTop
            canvas.drawText("AUREUM WALLET — LEDGER STATEMENT", marginH, cursorY, titlePaint)
            cursorY += 15f
            canvas.drawText(
                "Generated: ${dateFormat.format(Date())}   •   Records: ${transactions.size}",
                marginH,
                cursorY,
                metaPaint
            )
            cursorY += 13f
            val filterLine = TextUtils.ellipsize(
                summary.filterSummary,
                TextPaint(metaPaint),
                usableWidth,
                TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(filterLine, marginH, cursorY, metaPaint)
            cursorY += 12f

            // Summary Box
            val boxRect = RectF(marginH, cursorY, marginH + usableWidth, cursorY + 26f)
            canvas.drawRoundRect(boxRect, 5f, 5f, summaryBoxPaint)
            canvas.drawRoundRect(boxRect, 5f, 5f, summaryBorderPaint)

            val inStr = "In: +${summary.currency}${String.format(Locale.getDefault(), "%,.2f", summary.totalIncome)}"
            val outStr = "Out: -${summary.currency}${String.format(Locale.getDefault(), "%,.2f", summary.totalExpense)}"
            val netPrefix = if (summary.netBalance >= 0) "+" else ""
            val netStr = "Net: $netPrefix${summary.currency}${String.format(Locale.getDefault(), "%,.2f", summary.netBalance)}"

            summaryLabelPaint.color = Color.parseColor("#059669")
            canvas.drawText(inStr, marginH + 10f, cursorY + 16.5f, summaryLabelPaint)
            summaryLabelPaint.color = Color.parseColor("#DC2626")
            canvas.drawText(outStr, marginH + (usableWidth * 0.36f), cursorY + 16.5f, summaryLabelPaint)
            summaryLabelPaint.color = if (summary.netBalance >= 0) Color.parseColor("#0F172A") else Color.parseColor("#DC2626")
            canvas.drawText(netStr, marginH + (usableWidth * 0.70f), cursorY + 16.5f, summaryLabelPaint)

            cursorY += 36f
            cursorY = drawTableHeader(canvas, cursorY)

            val rowHeight = 22f
            val maxContentY = pageHeight - marginBottom

            if (transactions.isEmpty()) {
                canvas.drawRect(marginH, cursorY, marginH + usableWidth, cursorY + 32f, rowEvenBgPaint)
                canvas.drawRect(marginH, cursorY, marginH + usableWidth, cursorY + 32f, cellBorderPaint)
                canvas.drawText("No transactions match the selected filters.", marginH + 10f, cursorY + 19f, metaPaint)
            } else {
                transactions.forEachIndexed { index, tx ->
                    if (cursorY + rowHeight > maxContentY) {
                        drawFooter(canvas, pageNumber)
                        pdfDocument.finishPage(page)

                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        cursorY = marginTop
                        cursorY = drawTableHeader(canvas, cursorY)
                    }

                    val bgPaint = if (index % 2 == 0) rowEvenBgPaint else rowOddBgPaint
                    canvas.drawRect(marginH, cursorY, marginH + usableWidth, cursorY + rowHeight, bgPaint)

                    val dateStr = rowDateFormat.format(Date(tx.timestamp))
                    val titleStr = tx.note.ifBlank { "Untitled ${tx.category}" }
                    val categoryStr = if (tx.type == "TRANSFER") "Transfer" else tx.category
                    val fromAcc = accountMap[tx.accountId]?.name ?: "Vault #${tx.accountId}"
                    val toAcc = tx.toAccountId?.let { accountMap[it]?.name }
                    val vaultStr = if (tx.type == "TRANSFER" && toAcc != null) "$fromAcc -> $toAcc" else fromAcc
                    val typeStr = when (tx.type) {
                        "INCOME" -> "Income"
                        "EXPENSE" -> "Expense"
                        "TRANSFER" -> "Transfer"
                        else -> tx.type
                    }
                    val amountPrefix = when (tx.type) {
                        "INCOME" -> "+"
                        "EXPENSE" -> "-"
                        else -> ""
                    }
                    val amountStr = "$amountPrefix${summary.currency}${String.format(Locale.getDefault(), "%,.2f", tx.amount)}"

                    val rowValues = arrayOf(dateStr, titleStr, categoryStr, vaultStr, typeStr, amountStr)
                    var x = marginH
                    for (colIdx in rowValues.indices) {
                        val colW = colWidths[colIdx]
                        canvas.drawRect(x, cursorY, x + colW, cursorY + rowHeight, cellBorderPaint)

                        if (colIdx == rowValues.lastIndex) {
                            amountTextPaint.color = when (tx.type) {
                                "INCOME" -> Color.parseColor("#059669")
                                "EXPENSE" -> Color.parseColor("#DC2626")
                                else -> Color.parseColor("#1E293B")
                            }
                            val clipped = TextUtils.ellipsize(
                                rowValues[colIdx],
                                amountTextPaint,
                                colW - 10f,
                                TextUtils.TruncateAt.END
                            ).toString()
                            canvas.drawText(clipped, x + colW - 6f, cursorY + 14f, amountTextPaint)
                        } else {
                            val clipped = TextUtils.ellipsize(
                                rowValues[colIdx],
                                cellTextPaint,
                                colW - 10f,
                                TextUtils.TruncateAt.END
                            ).toString()
                            canvas.drawText(clipped, x + 5f, cursorY + 14f, cellTextPaint)
                        }
                        x += colW
                    }
                    cursorY += rowHeight
                }
            }

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
                out.flush()
            } ?: throw IllegalStateException("Could not open output stream for selected file")

            pdfDocument.close()
            Result.success(transactions.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
