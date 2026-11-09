package com.braveboy.hesabi.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.braveboy.hesabi.R
import java.io.ByteArrayOutputStream

/**
 * Persian-first PDF report writer built on [PdfDocument] and [StaticLayout].
 *
 * StaticLayout is what makes the output correct: it applies Arabic shaping and the
 * right-to-left bidi algorithm. Drawing raw [Canvas.drawText] lines would reverse
 * mixed numbers and break the letter joins.
 */
object PdfReportWriter {

    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f

    data class Spec(
        val title: String,
        val subtitle: String = "",
        val metaLines: List<String> = emptyList(),
        val sections: List<Section> = emptyList(),
        val footer: String = "حسابی — گزارش محلی",
    )

    data class Section(
        val title: String,
        val paragraphs: List<String> = emptyList(),
        val rows: List<Row> = emptyList(),
    )

    data class Row(val cells: List<String>, val bold: Boolean = false)

    fun write(spec: Spec, context: Context? = null): ByteArray {
        val document = PdfDocument()
        val contentWidth = PAGE_WIDTH - MARGIN * 2
        val regular = context?.let { ResourcesCompat.getFont(it, R.font.vazirmatn_regular) }
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        val bold = context?.let { ResourcesCompat.getFont(it, R.font.vazirmatn_bold) }
            ?: Typeface.create("sans-serif", Typeface.BOLD)
        val titlePaint = textPaint(19f, true, Color.rgb(20, 42, 36), regular, bold)
        val subtitlePaint = textPaint(12f, false, Color.rgb(70, 90, 84), regular, bold)
        val metaPaint = textPaint(9f, false, Color.rgb(120, 130, 127), regular, bold)
        val sectionPaint = textPaint(13f, true, Color.rgb(16, 120, 90), regular, bold)
        val bodyPaint = textPaint(10f, false, Color.rgb(32, 40, 38), regular, bold)
        val rowPaint = textPaint(9.5f, false, Color.rgb(40, 48, 46), regular, bold)
        val rowBoldPaint = textPaint(9.5f, true, Color.rgb(20, 42, 36), regular, bold)
        val rulePaint = Paint().apply {
            color = Color.rgb(220, 228, 225)
            strokeWidth = 0.8f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        fun finishPage() {
            canvas.drawText(
                spec.footer + " — صفحهٔ $pageNumber",
                MARGIN,
                PAGE_HEIGHT - 20f,
                metaPaint,
            )
            document.finishPage(page)
        }

        fun newPage() {
            finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensure(space: Float) {
            if (y + space > PAGE_HEIGHT - MARGIN - 12f) newPage()
        }

        fun drawParagraph(text: String, paint: TextPaint, spacingAfter: Float, width: Float = contentWidth) {
            if (text.isBlank()) return
            val layout = layout(text, paint, width.toInt())
            ensure(layout.height + spacingAfter)
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spacingAfter
        }

        // Header
        drawParagraph(spec.title, titlePaint, 6f)
        if (spec.subtitle.isNotBlank()) drawParagraph(spec.subtitle, subtitlePaint, 8f)
        if (spec.metaLines.isNotEmpty()) {
            spec.metaLines.forEach { drawParagraph(it, metaPaint, 2f) }
            y += 4f
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
        y += 14f

        spec.sections.forEach { section ->
            ensure(34f)
            drawParagraph(section.title, sectionPaint, 6f)
            section.paragraphs.forEach { paragraph ->
                drawParagraph(paragraph, bodyPaint, 4f)
            }
            if (section.rows.isNotEmpty()) {
                val columns = section.rows.maxOf { it.cells.size }.coerceAtLeast(1)
                val columnWidth = contentWidth / columns
                section.rows.forEachIndexed { index, row ->
                    val paint = if (row.bold || index == 0) rowBoldPaint else rowPaint
                    val layouts = row.cells.map { layout(it, paint, columnWidth.toInt() - 6) }
                    val rowHeight = (layouts.maxOfOrNull { it.height } ?: 12) + 6f
                    ensure(rowHeight)
                    var x = MARGIN
                    layouts.forEach { cellLayout ->
                        canvas.save()
                        canvas.translate(x, y)
                        cellLayout.draw(canvas)
                        canvas.restore()
                        x += columnWidth
                    }
                    y += rowHeight
                    if (index == 0) {
                        canvas.drawLine(MARGIN, y - 3f, PAGE_WIDTH - MARGIN, y - 3f, rulePaint)
                    }
                }
            }
            y += 12f
        }

        finishPage()
        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    private fun layout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(40))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setLineSpacing(2f, 1.25f)
            .build()

    private fun textPaint(
        size: Float,
        bold: Boolean = false,
        color: Int,
        regular: Typeface,
        boldTypeface: Typeface,
    ): TextPaint = TextPaint().apply {
        textSize = size
        this.color = color
        isAntiAlias = true
        typeface = if (bold) boldTypeface else regular
    }
}
