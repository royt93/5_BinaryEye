package com.mckimquyen.binaryeye.database

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {

    @Test
    fun plainContent_unchanged() {
        assertEquals("hello world", "hello world".escapeFormulaInjection())
        assertEquals("", "".escapeFormulaInjection())
    }

    @Test
    fun formulaPrefixes_getQuotePrefixed() {
        assertEquals("'=cmd|' /C calc'!A0", "=cmd|' /C calc'!A0".escapeFormulaInjection())
        assertEquals("'+1+1", "+1+1".escapeFormulaInjection())
        assertEquals("'-1-1", "-1-1".escapeFormulaInjection())
        assertEquals("'@SUM(A1:A2)", "@SUM(A1:A2)".escapeFormulaInjection())
    }

    @Test
    fun nonFormulaLeadingChar_unchanged() {
        // Dau '-' o giua chuoi (khong phai ky tu dau) khong bi anh huong.
        assertEquals("a-b", "a-b".escapeFormulaInjection())
        assertEquals("https://example.com", "https://example.com".escapeFormulaInjection())
    }
}
