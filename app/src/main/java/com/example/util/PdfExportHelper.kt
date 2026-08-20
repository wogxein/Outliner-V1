package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.model.Note
import com.example.domain.model.OutlineItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    private const val PAGE_WIDTH = 595 // A4 standard point width (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard point height
    private const val MARGIN_HORIZONTAL = 40f
    private const val MARGIN_VERTICAL = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)

    /**
     * Exports an outline note to a clean, multi-page PDF document.
     */
    fun exportNoteToPdf(
        context: Context,
        note: Note,
        items: List<OutlineItem>,
        enabledItemIds: Set<String>? = null
    ): File? {
        return try {
            val document = PdfDocument()
            var pageNumber = 1
            var currentY = MARGIN_VERTICAL

            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(20, 24, 33)
                textSize = 20f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val metaPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
                isAntiAlias = true
            }

            val bulletPaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                isAntiAlias = true
            }

            val textPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val checkboxPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                strokeWidth = 1.2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val checkboxCheckedPaint = Paint().apply {
                color = Color.rgb(16, 185, 129)
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val footerPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 9f
                isAntiAlias = true
            }

            // Draw Note Header
            val noteTitle = note.title.ifBlank { "Untitled Note" }
            canvas.drawText(noteTitle, MARGIN_HORIZONTAL, currentY + 16f, titlePaint)
            currentY += 28f

            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
            val metaStr = "Updated $dateStr • ${items.size} outline items"
            canvas.drawText(metaStr, MARGIN_HORIZONTAL, currentY + 8f, metaPaint)
            currentY += 16f

            canvas.drawLine(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY, linePaint)
            currentY += 18f

            // Filter items if selection is provided
            val activeItems = if (enabledItemIds != null) {
                items.filter { it.id in enabledItemIds }
            } else {
                items
            }

            // Build hierarchical levels map
            val itemMap = activeItems.associateBy { it.id }
            val depthMap = mutableMapOf<String, Int>()

            fun getDepth(item: OutlineItem): Int {
                if (depthMap.containsKey(item.id)) return depthMap[item.id]!!
                val parentId = item.parentId
                val depth = if (parentId == null || !itemMap.containsKey(parentId)) {
                    0
                } else {
                    getDepth(itemMap[parentId]!!) + 1
                }
                depthMap[item.id] = depth
                return depth
            }

            for (item in activeItems) {
                val depth = getDepth(item)
                val indent = MARGIN_HORIZONTAL + (depth * 18f)
                val textStartX = indent + 16f
                val availableWidth = (PAGE_WIDTH - MARGIN_HORIZONTAL) - textStartX

                val lines = wrapText(item.text.ifBlank { "(empty)" }, textPaint, availableWidth)
                val itemBlockHeight = lines.size * 16f + 6f

                // Check if we need a new page
                if (currentY + itemBlockHeight > PAGE_HEIGHT - MARGIN_VERTICAL - 20f) {
                    // Draw footer on current page
                    canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN_HORIZONTAL - 40f, PAGE_HEIGHT - MARGIN_VERTICAL / 2, footerPaint)
                    document.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN_VERTICAL
                }

                // Draw bullet or checkbox
                val rowCenterY = currentY + 9f
                if (item.hasCheckbox) {
                    val boxRect = RectF(indent, rowCenterY - 4.5f, indent + 9f, rowCenterY + 4.5f)
                    if (item.isChecked) {
                        canvas.drawRoundRect(boxRect, 2f, 2f, checkboxCheckedPaint)
                    } else {
                        canvas.drawRoundRect(boxRect, 2f, 2f, checkboxPaint)
                    }
                } else {
                    canvas.drawCircle(indent + 4.5f, rowCenterY, 2.5f, bulletPaint)
                }

                // Draw text lines
                for (line in lines) {
                    canvas.drawText(line, textStartX, currentY + 12f, textPaint)
                    currentY += 16f
                }
                currentY += 4f
            }

            // Draw footer on final page
            canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN_HORIZONTAL - 40f, PAGE_HEIGHT - MARGIN_VERTICAL / 2, footerPaint)
            document.finishPage(page)

            // Save to file
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
            val outFile = File(exportDir, "${sanitizedTitle}_Note.pdf")
            FileOutputStream(outFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports Flashcards to a formatted study PDF.
     */
    fun exportFlashcardsToPdf(
        context: Context,
        noteTitle: String,
        flashcards: List<Pair<String, List<String>>>
    ): File? {
        return try {
            val document = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = MARGIN_VERTICAL

            val titlePaint = Paint().apply {
                color = Color.rgb(20, 24, 33)
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val cardBgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
                isAntiAlias = true
            }

            val cardBorderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            val frontPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val backPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val labelPaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            // Header
            canvas.drawText("Flashcards: $noteTitle", MARGIN_HORIZONTAL, currentY + 16f, titlePaint)
            currentY += 32f

            for ((index, card) in flashcards.withIndex()) {
                val answersText = if (card.second.isEmpty()) "• (No sub-points)" else card.second.joinToString("\n") { "• $it" }
                val backLines = wrapText(answersText, backPaint, CONTENT_WIDTH - 24f).take(5)
                val cardHeight = (80f + (backLines.size * 14f)).coerceAtLeast(110f)

                if (currentY + cardHeight > PAGE_HEIGHT - MARGIN_VERTICAL - 20f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN_VERTICAL
                }

                val cardRect = RectF(MARGIN_HORIZONTAL, currentY, PAGE_WIDTH - MARGIN_HORIZONTAL, currentY + cardHeight)
                canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
                canvas.drawRoundRect(cardRect, 8f, 8f, cardBorderPaint)

                // Card number
                canvas.drawText("CARD ${index + 1} OF ${flashcards.size}", MARGIN_HORIZONTAL + 12f, currentY + 16f, labelPaint)

                // Front / Question
                val frontLines = wrapText(card.first, frontPaint, CONTENT_WIDTH - 24f).take(2)
                var yOffset = currentY + 34f
                for (l in frontLines) {
                    canvas.drawText(l, MARGIN_HORIZONTAL + 12f, yOffset, frontPaint)
                    yOffset += 16f
                }

                // Divider line inside card
                canvas.drawLine(MARGIN_HORIZONTAL + 12f, yOffset + 2f, PAGE_WIDTH - MARGIN_HORIZONTAL - 12f, yOffset + 2f, cardBorderPaint)
                yOffset += 14f

                // Back / Details
                for (l in backLines) {
                    canvas.drawText(l, MARGIN_HORIZONTAL + 12f, yOffset, backPaint)
                    yOffset += 14f
                }

                currentY += cardHeight + 14f
            }

            document.finishPage(page)

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
            val outFile = File(exportDir, "${sanitizedTitle}_Flashcards.pdf")
            FileOutputStream(outFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports Presentation Slides to PDF (Landscape 1 slide per page).
     */
    fun exportSlidesToPdf(
        context: Context,
        noteTitle: String,
        slides: List<Pair<String, List<String>>>
    ): File? {
        return try {
            val slideWidth = 842 // A4 Landscape width
            val slideHeight = 595 // A4 Landscape height
            val document = PdfDocument()

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val slideHeaderPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 15f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val bulletPaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                isAntiAlias = true
            }

            val footerPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 11f
                isAntiAlias = true
            }

            val slideBorderPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            for ((index, slide) in slides.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(slideWidth, slideHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Draw slide border frame
                val frameRect = RectF(24f, 24f, slideWidth - 24f, slideHeight - 24f)
                canvas.drawRoundRect(frameRect, 16f, 16f, slideBorderPaint)

                var currentY = 70f

                if (index == 0) {
                    // Title Slide Layout
                    val titleLines = wrapText(slide.first, titlePaint, slideWidth - 140f)
                    var tY = (slideHeight / 2f) - (titleLines.size * 20f)
                    for (l in titleLines) {
                        canvas.drawText(l, 70f, tY, titlePaint)
                        tY += 34f
                    }
                    if (slide.second.isNotEmpty()) {
                        val sub = slide.second.first()
                        canvas.drawText(sub, 70f, tY + 10f, bodyPaint)
                    }
                } else {
                    // Content Slide Layout
                    canvas.drawText(slide.first, 60f, currentY + 10f, slideHeaderPaint)
                    currentY += 40f
                    canvas.drawLine(60f, currentY, slideWidth - 60f, currentY, slideBorderPaint)
                    currentY += 30f

                    for (bullet in slide.second) {
                        val bulletLines = wrapText(bullet, bodyPaint, slideWidth - 160f)
                        val bCenterY = currentY + 6f
                        canvas.drawCircle(70f, bCenterY, 3.5f, bulletPaint)
                        for (l in bulletLines) {
                            canvas.drawText(l, 86f, currentY + 11f, bodyPaint)
                            currentY += 24f
                        }
                        currentY += 10f
                    }
                }

                // Slide Footer
                val footerText = "$noteTitle • Slide ${index + 1} of ${slides.size}"
                canvas.drawText(footerText, 60f, slideHeight - 45f, footerPaint)

                document.finishPage(page)
            }

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
            val outFile = File(exportDir, "${sanitizedTitle}_Slides.pdf")
            FileOutputStream(outFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports Mind Map to a formatted visual tree PDF.
     */
    fun exportMindMapToPdf(
        context: Context,
        noteTitle: String,
        rootTitle: String,
        treeNodes: List<Pair<String, List<String>>>
    ): File? {
        // We can reuse the structured multi-page PDF generator
        val slidesData = mutableListOf<Pair<String, List<String>>>()
        slidesData.add(Pair(rootTitle, listOf("Visual Mind Map of Note Outline")))
        for (node in treeNodes) {
            slidesData.add(Pair(node.first, node.second))
        }
        return exportSlidesToPdf(context, noteTitle, slidesData)
    }

    /**
     * Launches Android Share Intent for the generated PDF.
     */
    fun sharePdf(context: Context, file: File, title: String = "Share PDF") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export & Share PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open share dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    result.add(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return if (result.isEmpty()) listOf(text) else result
    }
}
