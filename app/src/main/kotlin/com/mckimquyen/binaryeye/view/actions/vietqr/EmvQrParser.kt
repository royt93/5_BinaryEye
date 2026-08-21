package com.mckimquyen.binaryeye.view.actions.vietqr

/**
 * [FEAT FEAT-NEW-01] Parser TLV (Tag-Length-Value) thuan cho EMVCo QR Code
 * Specification for Payment Systems (Merchant Presented Mode) - chuan quoc te
 * ma VietQR/NAPAS 247 tuan theo. Moi truong: 2 chu so tag + 2 chu so do dai +
 * gia tri (dung ky tu). Khong phu thuoc Android framework nen unit-test duoc
 * tren JVM thuan.
 */
object EmvQrParser {

    data class Field(val tag: String, val value: String)

    /** Parse 1 tang TLV. Dung de goi de quy cho cac truong long nhau (vd tag 38, 62). */
    fun parseTlv(raw: String): List<Field> {
        val result = mutableListOf<Field>()
        var i = 0
        while (i + 4 <= raw.length) {
            val tag = raw.substring(i, i + 2)
            val length = raw.substring(i + 2, i + 4).toIntOrNull() ?: break
            val valueStart = i + 4
            val valueEnd = valueStart + length
            if (valueEnd > raw.length) break
            result.add(Field(tag, raw.substring(valueStart, valueEnd)))
            i = valueEnd
        }
        return result
    }
}
