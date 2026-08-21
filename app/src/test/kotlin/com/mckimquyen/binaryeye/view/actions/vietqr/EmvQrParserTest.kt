package com.mckimquyen.binaryeye.view.actions.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmvQrParserTest {

    @Test
    fun parseTlv_singleField() {
        val fields = EmvQrParser.parseTlv("00020101")
        assertEquals(1, fields.size)
        assertEquals("00", fields[0].tag)
        assertEquals("01", fields[0].value)
    }

    @Test
    fun parseTlv_multipleFields_inOrder() {
        val fields = EmvQrParser.parseTlv("000201" + "5802VN" + "5303704")
        assertEquals(
            listOf(
                EmvQrParser.Field("00", "01"),
                EmvQrParser.Field("58", "VN"),
                EmvQrParser.Field("53", "704"),
            ),
            fields
        )
    }

    @Test
    fun parseTlv_emptyString_returnsEmptyList() {
        assertTrue(EmvQrParser.parseTlv("").isEmpty())
    }

    @Test
    fun parseTlv_truncatedField_doesNotCrash_stopsGracefully() {
        // Khai bao do dai 10 nhung chi con 3 ky tu gia tri
        assertTrue(EmvQrParser.parseTlv("0010abc").isEmpty())
    }

    @Test
    fun parseTlv_nonNumericLength_stopsGracefully() {
        assertTrue(EmvQrParser.parseTlv("00XXabc").isEmpty())
    }

    @Test
    fun parseTlv_nestedTlv_canBeReparsed() {
        val outer = EmvQrParser.parseTlv("3810" + "0006970436")
        val nested = EmvQrParser.parseTlv(outer[0].value)
        assertEquals("970436", nested[0].value)
    }
}
