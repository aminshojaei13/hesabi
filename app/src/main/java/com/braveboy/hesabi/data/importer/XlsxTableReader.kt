package com.braveboy.hesabi.data.importer

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Reads the first worksheet of an .xlsx file without Apache POI.
 *
 * Only what an import wizard needs is supported: shared strings, inline strings,
 * numbers and simple formulas (the cached value is used). Styling is ignored.
 */
object XlsxTableReader {

    fun read(input: InputStream): CsvTableParser.Table {
        val zipBytes = input.readBytes()
        var sharedStrings: List<String> = emptyList()
        var sheetXml: String? = null

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xl/sharedStrings.xml") {
                    sharedStrings = parseSharedStrings(zip)
                } else if (name == "xl/worksheets/sheet1.xml" || (sheetXml == null && name.startsWith("xl/worksheets/sheet"))) {
                    sheetXml = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val xml = sheetXml ?: return CsvTableParser.Table(emptyList(), emptyList())
        return parseSheet(xml, sharedStrings)
    }

    private fun parseSharedStrings(input: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var inText = false
        val current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") current.clear()
                    if (parser.name == "t") inText = true
                }
                XmlPullParser.TEXT -> if (inText) current.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") inText = false
                    if (parser.name == "si") strings.add(current.toString())
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseSheet(xml: String, sharedStrings: List<String>): CsvTableParser.Table {
        val rows = mutableListOf<MutableMap<Int, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        var currentRow: MutableMap<Int, String>? = null
        var cellType: String? = null
        var cellColumn = 0
        var cellValue = StringBuilder()
        var inValue = false
        var inInlineText = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableMapOf()
                    "c" -> {
                        cellType = parser.getAttributeValue(null, "t")
                        cellColumn = columnIndex(parser.getAttributeValue(null, "r"))
                        cellValue = StringBuilder()
                    }
                    "v" -> inValue = true
                    "t" -> if (cellType == "inlineStr") inInlineText = true
                }
                XmlPullParser.TEXT -> {
                    if (inValue || inInlineText) cellValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inValue = false
                    "t" -> inInlineText = false
                    "c" -> {
                        val raw = cellValue.toString()
                        val value = when (cellType) {
                            "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                            else -> raw
                        }
                        currentRow?.put(cellColumn, value)
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                    }
                }
            }
            event = parser.next()
        }

        if (rows.isEmpty()) return CsvTableParser.Table(emptyList(), emptyList())
        val width = rows.maxOf { row -> (row.keys.maxOrNull() ?: 0) + 1 }
        val asLists = rows.map { row -> List(width) { index -> row[index] ?: "" } }
        val headers = asLists.first()
        return CsvTableParser.Table(headers, asLists.drop(1).filter { row -> row.any { it.isNotBlank() } })
    }

    /** "AB12" -> 27 (zero-based column index). */
    private fun columnIndex(reference: String?): Int {
        if (reference.isNullOrBlank()) return 0
        var value = 0
        for (char in reference) {
            if (char.isLetter()) {
                value = value * 26 + (char.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (value - 1).coerceAtLeast(0)
    }
}
