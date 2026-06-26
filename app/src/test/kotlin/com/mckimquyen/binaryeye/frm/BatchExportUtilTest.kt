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
        assertTrue(!zipEntryName("a\\b\\c", 0).contains("\\"))
    }

    @Test
    fun exactly24Chars_kept() {
        val name = "a".repeat(24)
        assertEquals("${"a".repeat(24)}_1.png", zipEntryName(name, 0))
    }

    @Test
    fun truncationHappensBeforeSanitize() {
        // 24 ký tự đầu là "aaaa..../" — '/' ở vị trí 24 bị cắt trước khi sanitize.
        val name = "a".repeat(24) + "/evil"
        assertEquals("${"a".repeat(24)}_1.png", zipEntryName(name, 0))
    }

    @Test
    fun unicodeOnly_fallsBackToQr() {
        // Ký tự ngoài [A-Za-z0-9._-] đều bị thay → trim → rỗng → "qr".
        assertEquals("qr_5.png", zipEntryName("日本語コード", 4))
    }

    @Test
    fun dotsAndDashAndUnderscore_arePreserved() {
        assertEquals("v1.2-3_x_1.png", zipEntryName("v1.2-3_x", 0))
    }

    @Test
    fun largeIndex_formattedAsIs() {
        assertEquals("item_200.png", zipEntryName("item", 199))
    }

    @Test
    fun everyName_endsWithPngAndHasIndex() {
        for (i in 0 until 50) {
            val n = zipEntryName("x$i", i)
            assertTrue(n.endsWith("_${i + 1}.png"))
        }
    }
}
