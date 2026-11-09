package com.braveboy.hesabi.data.importer

/**
 * RFC-4180-ish CSV reader that also accepts the semicolon and tab variants used by
 * Iranian banking and competitor exports. Handles quoted fields, newlines inside
 * quotes and a UTF-8 BOM.
 */
object CsvTableParser {

    data class Table(val headers: List<String>, val rows: List<List<String>>)

    fun parse(text: String): Table {
        val cleaned = text.removePrefix("\uFEFF").trimEnd('\r', '\n')
        if (cleaned.isBlank()) return Table(emptyList(), emptyList())
        val delimiter = detectDelimiter(cleaned)
        val records = readRecords(cleaned, delimiter)
        if (records.isEmpty()) return Table(emptyList(), emptyList())
        val headers = records.first().map { it.trim() }
        val width = headers.size
        val rows = records.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row -> pad(row, width) }
        return Table(headers, rows)
    }

    fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val candidates = listOf(',', ';', '\t', '|')
        return candidates.maxByOrNull { firstLine.count { char -> char == it } } ?: ','
    }

    private fun pad(row: List<String>, width: Int): List<String> =
        if (row.size >= width) row.take(width) else row + List(width - row.size) { "" }

    private fun readRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes -> {
                    if (char == '"') {
                        if (index + 1 < text.length && text[index + 1] == '"') {
                            cell.append('"')
                            index++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        cell.append(char)
                    }
                }
                char == '"' -> inQuotes = true
                char == delimiter -> {
                    current.add(cell.toString())
                    cell.clear()
                }
                char == '\n' -> {
                    current.add(cell.toString())
                    cell.clear()
                    records.add(current)
                    current = mutableListOf()
                }
                char == '\r' -> Unit
                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || current.isNotEmpty()) {
            current.add(cell.toString())
            records.add(current)
        }
        return records
    }
}
