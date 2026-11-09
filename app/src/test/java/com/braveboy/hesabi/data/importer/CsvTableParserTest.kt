package com.braveboy.hesabi.data.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTableParserTest {

    @Test
    fun `parses quoted values and semicolons with BOM`() {
        val text = "\uFEFFتاریخ;مبلغ;توضیح\n۱۴۰۵/۰۸/۰۱;۲۵۰٬۰۰۰;\"ناهار، با تیم\"\n"
        val table = CsvTableParser.parse(text)
        assertEquals(listOf("تاریخ", "مبلغ", "توضیح"), table.headers)
        assertEquals(1, table.rows.size)
        assertEquals("ناهار، با تیم", table.rows[0][2])
    }

    @Test
    fun `detects tab delimiter and pads short rows`() {
        val text = "date\tamount\tnote\n1405-08-01\t1000\n"
        val table = CsvTableParser.parse(text)
        assertEquals(3, table.headers.size)
        assertEquals(3, table.rows[0].size)
        assertEquals("", table.rows[0][2])
    }

    @Test
    fun `handles newlines inside quotes`() {
        val text = "a,b\n1,\"line1\nline2\"\n"
        val table = CsvTableParser.parse(text)
        assertEquals("line1\nline2", table.rows[0][1])
    }
}
