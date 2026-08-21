package com.mckimquyen.binaryeye.view.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GS = ""

class Gs1ParserTest {

    @Test
    fun nonGs1Content_isNotGs1_allFieldsNull() {
        val result = Gs1Parser.parse("https://example.com/product/123")
        assertFalse(result.isGs1)
        assertNull(result.gtin)
        assertNull(result.lot)
        assertNull(result.expiryDate)
        assertNull(result.serial)
    }

    @Test
    fun emptyContent_isNotGs1() {
        assertFalse(Gs1Parser.parse("").isGs1)
    }

    @Test
    fun bracketedFormat_parsesAllKnownFields() {
        val result = Gs1Parser.parse("(01)09501101530003(17)191231(10)ABC123(21)12345")
        assertTrue(result.isGs1)
        assertEquals("09501101530003", result.gtin)
        assertEquals("2019-12-31", result.expiryDate)
        assertEquals("ABC123", result.lot)
        assertEquals("12345", result.serial)
    }

    @Test
    fun bracketedFormat_unknownAiIsSkipped_knownOnesStillParsed() {
        val result = Gs1Parser.parse("(91)vendor-only(01)09501101530003")
        assertEquals("09501101530003", result.gtin)
        assertEquals(1, result.fields.size)
    }

    @Test
    fun fnc1Format_fixedThenVariableThenLastVariable_parsesCorrectly() {
        // AI 01 (14, fixed) + AI 17 (6, fixed) + AI 10 (variable, needs GS before next
        // field) + AI 21 (variable, last field - no trailing GS needed)
        val raw = "0109501101530003" + "17191231" + "10LOT99" + GS + "21SN42"
        val result = Gs1Parser.parse(raw)
        assertTrue(result.isGs1)
        assertEquals("09501101530003", result.gtin)
        assertEquals("2019-12-31", result.expiryDate)
        assertEquals("LOT99", result.lot)
        assertEquals("SN42", result.serial)
    }

    @Test
    fun fnc1Format_leadingSymbologyIdentifierAndGs_areStripped() {
        val raw = "]C1" + GS + "0109501101530003"
        val result = Gs1Parser.parse(raw)
        assertEquals("09501101530003", result.gtin)
    }

    @Test
    fun fnc1Format_onlySerial_noOtherFields() {
        val result = Gs1Parser.parse("21SN-ONLY-42")
        assertEquals("SN-ONLY-42", result.serial)
        assertNull(result.gtin)
        assertNull(result.lot)
        assertNull(result.expiryDate)
    }

    @Test
    fun fnc1Format_unknownLeadingAi_returnsEmptyNotCrash() {
        val result = Gs1Parser.parse("99UNKNOWN-PREFIX")
        assertFalse(result.isGs1)
    }

    @Test
    fun fnc1Format_truncatedFixedLengthField_doesNotCrash() {
        // AI 01 expects 14 digits but only 5 given - should not throw
        val result = Gs1Parser.parse("0112345")
        assertEquals("12345", result.gtin)
    }

    @Test
    fun expiryDate_yyBoundary_2000to2050_and_1951to1999() {
        assertEquals("2000-01-01", Gs1Parser.parse("(17)000101").expiryDate)
        assertEquals("2050-01-01", Gs1Parser.parse("(17)500101").expiryDate)
        assertEquals("1951-01-01", Gs1Parser.parse("(17)510101").expiryDate)
        assertEquals("1999-01-01", Gs1Parser.parse("(17)990101").expiryDate)
    }

    @Test
    fun expiryDate_malformed_returnsNull() {
        assertNull(Gs1Parser.parse("(17)1231").expiryDate)
        assertNull(Gs1Parser.parse("(17)ABCDEF").expiryDate)
    }
}
