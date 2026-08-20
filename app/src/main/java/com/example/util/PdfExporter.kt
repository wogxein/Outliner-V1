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
import com.example.domain.model.TreeItemNode
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width at 72dpi
    private const val PAGE_HEIGHT = 842 // A4 standard height at 72dpi
    private const val MARGIN_X = 40f
    private const val MARGIN_Y = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    /**
     * Exports an outline note to a formatted multi-page PDF document.
     */
    fun exportNoteToPdf(
        context: Context,
        note: Note,
        items: List<TreeItemNode>,
        fontFamilyName: String? = null
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39) // Slate 900
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1f
        }
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(59, 130, 246) // Blue 500
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = MARGIN_Y

        // Draw Document Header
        val displayTitle = note.title.ifBlank { "Untitled Note" }
        canvas.drawText(displayTitle, MARGIN_X, currentY + 18f, titlePaint)
        currentY += 28f

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val dateStr = "Created: ${dateFormat.format(Date(note.createdAt))}   |   Updated: ${dateFormat.format(Date(note.updatedAt))}"
        canvas.drawText(dateStr, MARGIN_X, currentY + 8f, subPaint)
        currentY += 16f

        // Divider
        canvas.drawLine(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY, linePaint)
        currentY += 20f

        // Draw Tree Items
        for (node in items) {
            val item = node.item
            if (item.text.isBlank()) continue

            // Setup text style per heading / bold
            val textSize = when (item.headingLevel) {
                1 -> 15f
                2 -> 13.5f
                3 -> 12f
                else -> 11f
            }
            val textStyle = when {
                item.headingLevel > 0 || item.isBold -> Typeface.BOLD
                item.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            paint.apply {
                color = if (item.isChecked) Color.rgb(148, 163, 184) else Color.rgb(30, 41, 59)
                this.textSize = textSize
                typeface = Typeface.create(Typeface.DEFAULT, textStyle)
                isStrikeThruText = item.isStrikethrough || (item.hasCheckbox && item.isChecked)
            }

            val indent = (node.level * 16f).coerceAtMost(CONTENT_WIDTH - 80f)
            val itemX = MARGIN_X + indent
            val textStartX = itemX + 16f

            // Line height calculation
            val textLineHeight = textSize * 1.4f

            // Split long lines
            val maxTextWidth = (MARGIN_X + CONTENT_WIDTH) - textStartX
            val wrappedLines = wrapText(item.text, paint, maxTextWidth)
            val itemHeight = wrappedLines.size * textLineHeight + 6f

            // Check if we need a new page
            if (currentY + itemHeight > PAGE_HEIGHT - MARGIN_Y) {
                // Draw footer on current page
                drawPageFooter(canvas, pageNumber, subPaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN_Y
            }

            // Draw bullet or checkbox
            val bulletCenterY = currentY + (textLineHeight / 2) - 2f
            if (item.hasCheckbox) {
                val boxSize = 9f
                val boxRect = RectF(itemX, bulletCenterY - boxSize / 2, itemX + boxSize, bulletCenterY + boxSize / 2)
                val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = if (item.isChecked) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                    color = if (item.isChecked) Color.rgb(59, 130, 246) else Color.rgb(148, 163, 184)
                    strokeWidth = 1.2f
                }
                canvas.drawRoundRect(boxRect, 2f, 2f, checkPaint)
            } else {
                val bulletRadius = when (node.level) {
                    0 -> 2.5f
                    1 -> 2.0f
                    else -> 1.5f
                }
                canvas.drawCircle(itemX + 4f, bulletCenterY, bulletRadius, bulletPaint)
            }

            // Draw lines of text
            var lineY = currentY + textSize - 2f
            for (line in wrappedLines) {
                canvas.drawText(line, textStartX, lineY, paint)
                lineY += textLineHeight
            }

            currentY += itemHeight
        }

        // Draw footer on last page
        drawPageFooter(canvas, pageNumber, subPaint)
        pdfDocument.finishPage(page)

        // Write to cache file
        return try {
            val safeTitle = note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "note" }
            val pdfFile = File(context.cacheDir, "${safeTitle}_outline.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Exports a visual Mind Map as a landscape/portrait PDF.
     */
    fun exportMindMapToPdf(
        context: Context,
        noteTitle: String,
        rootTitle: String,
        nodes: List<TreeItemNode>
    ): File? {
        val pdfDocument = PdfDocument()
        val width = 842 // Landscape A4
        val height = 595
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) } // Slate 50
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Mind Map: $noteTitle", 30f, 40f, titlePaint)

        // Draw Root Node in Center-Left or Center
        val rootX = 120f
        val rootY = height / 2f
        val rootRect = RectF(rootX - 70f, rootY - 24f, rootX + 70f, rootY + 24f)
        val rootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Blue 600
            style = Paint.Style.FILL
        }
        val rootTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(rootRect, 12f, 12f, rootPaint)
        val shortRoot = rootTitle.take(18)
        canvas.drawText(shortRoot, rootX, rootY + 4f, rootTextPaint)

        // Group level 0 and children
        val topLevelNodes = nodes.filter { it.level == 0 }
        val branchCount = topLevelNodes.size.coerceAtLeast(1)
        val branchSpacing = (height - 120f) / branchCount

        val branchLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(147, 197, 253)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val nodeCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 255, 255)
            style = Paint.Style.FILL
        }
        val nodeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val nodeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
        }

        topLevelNodes.forEachIndexed { idx, topNode ->
            val branchY = 70f + (idx + 0.5f) * branchSpacing
            val branchX = 320f

            // Line from Root to Branch
            val path = android.graphics.Path().apply {
                moveTo(rootRect.right, rootY)
                cubicTo(
                    rootRect.right + 60f, rootY,
                    branchX - 60f, branchY,
                    branchX, branchY
                )
            }
            canvas.drawPath(path, branchLinePaint)

            // Branch Node Card
            val branchTitle = topNode.item.text.take(22).ifBlank { "Branch ${idx + 1}" }
            val branchRect = RectF(branchX, branchY - 18f, branchX + 140f, branchY + 18f)
            canvas.drawRoundRect(branchRect, 8f, 8f, nodeCardPaint)
            canvas.drawRoundRect(branchRect, 8f, 8f, nodeBorderPaint)
            canvas.drawText(branchTitle, branchX + 12f, branchY + 4f, nodeTextPaint)

            // Draw sub-children
            val childNodes = nodes.filter { it.item.parentId == topNode.item.id }
            val childCount = childNodes.size
            if (childCount > 0) {
                val childSpacing = 32f
                val childStartY = branchY - ((childCount - 1) * childSpacing / 2f)
                val childX = 530f

                childNodes.forEachIndexed { cIdx, cNode ->
                    val cY = childStartY + cIdx * childSpacing
                    // Line from branch to child
                    val cPath = android.graphics.Path().apply {
                        moveTo(branchRect.right, branchY)
                        cubicTo(
                            branchRect.right + 40f, branchY,
                            childX - 40f, cY,
                            childX, cY
                        )
                    }
                    canvas.drawPath(cPath, branchLinePaint)

                    val cTitle = cNode.item.text.take(25).ifBlank { "Subnode" }
                    val cRect = RectF(childX, cY - 14f, childX + 160f, cY + 14f)
                    canvas.drawRoundRect(cRect, 6f, 6f, nodeCardPaint)
                    canvas.drawRoundRect(cRect, 6f, 6f, nodeBorderPaint)
                    canvas.drawText(cTitle, childX + 10f, cY + 3f, subTextPaint)
                }
            }
        }

        pdfDocument.finishPage(page)

        return try {
            val safeTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "mindmap" }
            val pdfFile = File(context.cacheDir, "${safeTitle}_mindmap.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Exports Flashcards to printable 2-column card deck PDF.
     */
    fun exportFlashcardsToPdf(
        context: Context,
        noteTitle: String,
        cards: List<Pair<String, List<String>>>
    ): File? {
        val pdfDocument = PdfDocument()
        val cardWidth = (CONTENT_WIDTH - 20f) / 2f
        val cardHeight = 120f
        val cardsPerPage = 8 // 4 rows x 2 cols

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 255, 255)
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val qPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138) // Dark Blue
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val aPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Header on first page
        canvas.drawText("Flashcards: $noteTitle", MARGIN_X, MARGIN_Y + 10f, titlePaint)
        canvas.drawText("Total cards: ${cards.size}", MARGIN_X, MARGIN_Y + 24f, subPaint)

        var cardIndex = 0
        for ((idx, card) in cards.withIndex()) {
            val pageCardIdx = idx % cardsPerPage
            if (idx > 0 && pageCardIdx == 0) {
                drawPageFooter(canvas, pageNumber, subPaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
            }

            val row = pageCardIdx / 2
            val col = pageCardIdx % 2

            val startY = if (pageNumber == 1) MARGIN_Y + 36f else MARGIN_Y
            val left = MARGIN_X + col * (cardWidth + 20f)
            val top = startY + row * (cardHeight + 14f)
            val right = left + cardWidth
            val bottom = top + cardHeight

            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(rect, 8f, 8f, cardBorderPaint)

            // Draw Card Header badge
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(238, 242, 255)
            }
            canvas.drawRoundRect(RectF(left, top, right, top + 22f), 8f, 8f, badgePaint)
            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(79, 70, 229)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("CARD #${idx + 1}", left + 10f, top + 14f, badgeTextPaint)

            // Front Question / Topic
            val qText = card.first.take(50)
            canvas.drawText(qText, left + 10f, top + 38f, qPaint)

            // Back Supporting Answers
            var ansY = top + 52f
            for (bullet in card.second.take(3)) {
                if (ansY > bottom - 12f) break
                val bText = "• ${bullet.take(45)}"
                canvas.drawText(bText, left + 10f, ansY, aPaint)
                ansY += 14f
            }
        }

        drawPageFooter(canvas, pageNumber, subPaint)
        pdfDocument.finishPage(page)

        return try {
            val safeTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "flashcards" }
            val pdfFile = File(context.cacheDir, "${safeTitle}_flashcards.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Exports Presentation Slides to landscape PDF.
     */
    fun exportSlidesToPdf(
        context: Context,
        noteTitle: String,
        slides: List<Pair<String, List<String>>>
    ): File? {
        val pdfDocument = PdfDocument()
        val width = 842 // Landscape A4
        val height = 595

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val slideCounterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 11f
        }

        for ((idx, slide) in slides.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, idx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Slide Background
            val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Inner Presentation Card
            val cardRect = RectF(40f, 40f, width - 40f, height - 40f)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)
            canvas.drawRoundRect(cardRect, 16f, 16f, borderPaint)

            // Slide Title
            val titleY = 110f
            canvas.drawText(slide.first, 80f, titleY, titlePaint)

            // Accent bar under title
            val barPaint = Paint().apply { color = Color.rgb(59, 130, 246) }
            canvas.drawRect(80f, titleY + 12f, 160f, titleY + 16f, barPaint)

            // Bullets
            var bulletY = titleY + 60f
            for (bullet in slide.second) {
                if (bulletY > height - 100f) break
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(59, 130, 246) }
                canvas.drawCircle(88f, bulletY - 5f, 4f, dotPaint)
                canvas.drawText(bullet, 106f, bulletY, bulletPaint)
                bulletY += 34f
            }

            // Slide Counter Footer
            val footerText = "$noteTitle   |   Slide ${idx + 1} of ${slides.size}"
            canvas.drawText(footerText, 80f, height - 60f, slideCounterPaint)

            pdfDocument.finishPage(page)
        }

        return try {
            val safeTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "slides" }
            val pdfFile = File(context.cacheDir, "${safeTitle}_slides.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Launches Android Share/View Intent for generated PDF file.
     */
    fun sharePdf(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export & Share PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open share sheet: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines.ifEmpty { listOf(text) }
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, paint: Paint) {
        val footerY = PAGE_HEIGHT - MARGIN_Y / 2
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber", MARGIN_X + CONTENT_WIDTH, footerY, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}
