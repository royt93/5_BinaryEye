package com.mckimquyen.binaryeye.view.audit

/**
 * [FEAT VIP-02] Parser thuan cho barcode dang GS1-128/GS1 DataMatrix (Application
 * Identifier). Ho tro tap AI pho bien nhat cho B2B audit: GTIN/lot/expiry/serial.
 * Khong phu thuoc Android framework nen unit-test duoc tren JVM thuan.
 *
 * Chi ho tro AI 2 chu so (du cho MVP) - khong ho tro AI 3-4 chu so (vd 310x can
 * nang) hay full GS1 spec (vd "00" last-day-of-month cho ngay het han).
 */
object Gs1Parser {

    private const val GROUP_SEPARATOR = '' // FNC1, tach cac truong do dai bien doi

    data class Field(val ai: String, val label: String, val value: String)

    data class ParseResult(
        val gtin: String? = null,
        val lot: String? = null,
        val expiryDate: String? = null,
        val serial: String? = null,
        val fields: List<Field> = emptyList(),
    ) {
        val isGs1: Boolean get() = fields.isNotEmpty()
    }

    private data class AiDef(val length: Int?, val label: String)

    private val KNOWN_AIS = mapOf(
        "00" to AiDef(18, "SSCC"),
        "01" to AiDef(14, "GTIN"),
        "10" to AiDef(null, "BATCH_LOT"),
        "11" to AiDef(6, "PROD_DATE"),
        "13" to AiDef(6, "PACK_DATE"),
        "15" to AiDef(6, "BEST_BEFORE"),
        "17" to AiDef(6, "EXPIRY"),
        "21" to AiDef(null, "SERIAL"),
        "30" to AiDef(null, "COUNT"),
    )

    fun parse(raw: String): ParseResult {
        if (raw.isEmpty()) return ParseResult()
        val fields = if (raw.startsWith("(")) parseBracketed(raw) else parseFnc1(raw)
        if (fields.isEmpty()) return ParseResult()
        val byAi = fields.associateBy { it.ai }
        return ParseResult(
            gtin = byAi["01"]?.value,
            lot = byAi["10"]?.value,
            expiryDate = byAi["17"]?.value?.let { formatYyMmDd(it) },
            serial = byAi["21"]?.value,
            fields = fields,
        )
    }

    // Dang doc-duoc "(01)09501101530003(17)191231(10)ABC123(21)12345"
    private fun parseBracketed(raw: String): List<Field> {
        val regex = Regex("""\((\d{2})\)([^()]*)""")
        return regex.findAll(raw).mapNotNull { m ->
            val ai = m.groupValues[1]
            val def = KNOWN_AIS[ai] ?: return@mapNotNull null
            Field(ai, def.label, m.groupValues[2])
        }.toList()
    }

    // Dang tho tu camera: AI 2 so noi truc tiep vao gia tri, cac truong do dai
    // bien doi (khong co length co dinh) can GROUP_SEPARATOR de ket thuc - tru
    // khi la truong cuoi cung cua chuoi.
    private fun parseFnc1(raw: String): List<Field> {
        val s = raw.removePrefix("]C1").removePrefix("]e0").trimStart(GROUP_SEPARATOR)
        val result = mutableListOf<Field>()
        var i = 0
        while (i + 2 <= s.length) {
            val ai = s.substring(i, i + 2)
            val def = KNOWN_AIS[ai] ?: break
            val start = i + 2
            if (start > s.length) break
            val end = if (def.length != null) {
                (start + def.length).coerceAtMost(s.length)
            } else {
                val sep = s.indexOf(GROUP_SEPARATOR, start)
                if (sep >= 0) sep else s.length
            }
            result.add(Field(ai, def.label, s.substring(start, end)))
            i = if (end < s.length && s[end] == GROUP_SEPARATOR) end + 1 else end
        }
        return result
    }

    // GS1 spec: YYMMDD, yy 00-50 -> 2000-2050, 51-99 -> 1951-1999. dd="00" nghia
    // la "ngay cuoi cung cua thang" theo spec day du - MVP nay giu nguyen dang
    // "00" thay vi resolve ra ngay that, danh dau la gioi han da biet.
    private fun formatYyMmDd(yyMmDd: String): String? {
        if (yyMmDd.length != 6 || !yyMmDd.all { it.isDigit() }) return null
        val yy = yyMmDd.substring(0, 2).toInt()
        val mm = yyMmDd.substring(2, 4)
        val dd = yyMmDd.substring(4, 6)
        val year = if (yy <= 50) 2000 + yy else 1900 + yy
        return "$year-$mm-$dd"
    }
}
