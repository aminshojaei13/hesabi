package com.braveboy.hesabi.core.export

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class XlsxWriterTest {

    @Test
    fun `writes a valid package with rtl sheets and escaped text`() {
        val output = ByteArrayOutputStream()
        XlsxWriter.write(
            output,
            listOf(
                XlsxWriter.Sheet(
                    name = "تراکنش‌ها",
                    rows = listOf(
                        listOf("تاریخ", "مبلغ & توضیح"),
                        listOf("۱۴۰۵/۰۸/۰۱", 250_000L),
                        listOf("<ناهار>", 1.5),
                    ),
                ),
            ),
        )
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        val sheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("rightToLeft=\"1\""))
        assertTrue(sheet.contains("&amp;"))
        assertTrue(sheet.contains("&lt;ناهار&gt;"))
        assertTrue(sheet.contains("<v>250000</v>"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("تراکنش‌ها"))
    }

    @Test
    fun `sanitizes invalid sheet names`() {
        val output = ByteArrayOutputStream()
        XlsxWriter.write(output, listOf(XlsxWriter.Sheet("a/b:c*", listOf(listOf("x")))))
        val text = String(output.toByteArray(), Charsets.ISO_8859_1)
        assertTrue(text.contains("xl/worksheets/sheet1.xml"))
    }
}
