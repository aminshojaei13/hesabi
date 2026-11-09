package com.braveboy.hesabi.core.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free XLSX writer.
 *
 * The project ships without Apache POI on purpose (APK size, build time). The files
 * produced here use inline strings, one stylesheet and right-to-left sheet views, and
 * open cleanly in Excel, LibreOffice and Google Sheets.
 */
object XlsxWriter {

    data class Sheet(
        val name: String,
        val rows: List<List<Any?>>,
        val rightToLeft: Boolean = true,
    )

    fun write(out: OutputStream, sheets: List<Sheet>) {
        require(sheets.isNotEmpty()) { "at least one sheet is required" }
        val safeSheets = sheets.mapIndexed { index, sheet ->
            sheet.copy(name = sanitizeName(sheet.name, index))
        }
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypes(safeSheets.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(ROOT_RELS.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(workbook(safeSheets).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(workbookRels(safeSheets.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(STYLES.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            safeSheets.forEachIndexed { index, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${index + 1}.xml"))
                zip.write(worksheet(sheet).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        out.flush()
    }

    private fun sanitizeName(name: String, index: Int): String {
        val cleaned = name.replace(Regex("""[\[\]:*?/\\]"""), " ").trim().ifBlank { "Sheet${index + 1}" }
        return cleaned.take(31)
    }

    private fun contentTypes(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (index in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$index.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun workbook(sheets: List<Sheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
        append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("<sheets>")
        sheets.forEachIndexed { index, sheet ->
            append("""<sheet name="${escape(sheet.name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (index in 1..sheetCount) {
            append("""<Relationship Id="rId$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$index.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun worksheet(sheet: Sheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("<sheetViews><sheetView workbookViewId=\"0\"")
        if (sheet.rightToLeft) append(" rightToLeft=\"1\"")
        append(">")
        append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
        append("</sheetView></sheetViews>")
        append("""<sheetFormatPr defaultRowHeight="16"/>""")
        append("<sheetData>")
        sheet.rows.forEachIndexed { rowIndex, row ->
            val rowNumber = rowIndex + 1
            append("""<row r="$rowNumber">""")
            row.forEachIndexed { columnIndex, value ->
                val ref = columnName(columnIndex) + rowNumber
                when (value) {
                    null -> Unit
                    is Number -> append("""<c r="$ref"><v>$value</v></c>""")
                    is Boolean -> append("""<c r="$ref" t="b"><v>${if (value) 1 else 0}</v></c>""")
                    else -> {
                        val style = if (rowIndex == 0) """ s="1"""" else ""
                        append("""<c r="$ref"$style t="inlineStr"><is><t xml:space="preserve">""")
                        append(escape(value.toString()))
                        append("</t></is></c>")
                    }
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun columnName(index: Int): String {
        var value = index + 1
        val builder = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            builder.insert(0, ('A' + remainder))
            value = (value - 1) / 26
        }
        return builder.toString()
    }

    private fun escape(input: String): String = buildString(input.length) {
        for (char in input) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (char.code in 0x20..0xD7FF || char.code in 0xE000..0xFFFD) append(char)
            }
        }
    }

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
}
