package com.mckimquyen.binaryeye.view.actions.vietqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Payload EMVCo VietQR/NAPAS mau: Vietcombank (BIN 970436), tai khoan
// 0123456789, 500000 VND, noi dung "Thanh toan don hang"
private const val SAMPLE_VIETQR =
    "00020101021238540010A00000072701240006970436011001234567890208QRIBFTTA" +
        "52040000530370454065000005802VN5912NGUYEN VAN A62230819Thanh toan don hang"

class VietQrParserTest {

    @Test
    fun parse_fullSample_extractsAllFields() {
        val info = VietQrParser.parse(SAMPLE_VIETQR)
        assertEquals("970436", info?.bankBin)
        assertEquals("Vietcombank", info?.bankName)
        assertEquals("0123456789", info?.accountNumber)
        assertEquals("500000", info?.amount)
        assertEquals("704", info?.currency)
        assertEquals("NGUYEN VAN A", info?.merchantName)
        assertEquals("Thanh toan don hang", info?.message)
    }

    @Test
    fun parse_nonEmvContent_returnsNull() {
        assertNull(VietQrParser.parse("https://example.com"))
    }

    @Test
    fun parse_emvButNotNapas_returnsNull() {
        // GUID khac NAPAS (vd mang thanh toan quoc te khac) - khong nhan dien duoc
        val nonNapas = "00020138130009D00000000"
        assertNull(VietQrParser.parse(nonNapas))
    }

    @Test
    fun parse_missingWrongPayloadFormatIndicator_returnsNull() {
        assertNull(VietQrParser.parse("000202" + "0102"))
    }

    @Test
    fun parse_unknownBin_bankNameNullButBinStillExposed() {
        val unknownBinPayload =
            "00020101021238540010A00000072701240006999999011001234567890208QRIBFTTA"
        val info = VietQrParser.parse(unknownBinPayload)
        assertEquals("999999", info?.bankBin)
        assertNull(info?.bankName)
    }
}
