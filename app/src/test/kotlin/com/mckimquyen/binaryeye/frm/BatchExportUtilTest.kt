package com.mckimquyen.binaryeye.frm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchExportUtilTest {

    @Test
    fun plainName_keptAndSuffixedWith1BasedIndex() {
        assertEquals("product-001_1.png", zipEntryName("product-001", 0))
        assertEquals("product-001_3.png", zipEntryName("product-001", 2))
    }

    @Test
    fun unsafeChars_replacedWithUnderscore() {
        // Each unsafe char (':', '/', '/') maps to one '_'.
        assertEquals("https___example.com_1.png", zipEntryName("https://example.com", 0))
    }

    @Test
    fun emptyOrAllUnsafe_fallsBackToQr() {
        assertEquals("qr_1.png", zipEntryName("", 0))
        assertEquals("qr_2.png", zipEntryName("///", 1))
    }

    @Test
    fun longName_truncatedTo24Chars() {
        val name = "a".repeat(100)
        val result = zipEntryName(name, 0)
        assertEquals("${"a".repeat(24)}_1.png", result)
    }

    @Test
    fun leadingTrailingUnsafe_trimmed() {
        assertEquals("abc_1.png", zipEntryName("__abc__", 0))
    }

    @Test
    fun result_neverContainsPathSeparators() {
        val tricky = "../../etc/passwd"
        assertTrue(!zipEntryName(tricky, 0).contains("/"))
    }
}
