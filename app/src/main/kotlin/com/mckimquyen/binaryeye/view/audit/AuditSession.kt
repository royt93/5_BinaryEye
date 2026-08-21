package com.mckimquyen.binaryeye.view.audit

import com.mckimquyen.binaryeye.database.quoteAndEscape

/**
 * [FEAT VIP-02] Logic thuan cho 1 phien kiem ke (Batch Audit) - dem so lan
 * quet moi ma, doi chieu voi danh sach ky vong (neu co). Khong phu thuoc
 * Android framework nen unit-test duoc tren JVM thuan.
 */
class AuditSession(expectedCodes: Collection<String> = emptyList()) {

    enum class Outcome { NEW_EXPECTED, NEW_UNEXPECTED, DUPLICATE }

    data class Row(
        val content: String,
        val format: String,
        val count: Int,
        val status: String,
        val gtin: String? = null,
        val lot: String? = null,
        val expiryDate: String? = null,
        val serial: String? = null,
    )

    val expectedCodesList: List<String> = expectedCodes.toList()
    private val expectedCodes: Set<String> = expectedCodes.toSet()
    private val entries = LinkedHashMap<String, Entry>()

    private data class Entry(val format: String, var count: Int)

    val hasExpectedList: Boolean get() = expectedCodes.isNotEmpty()
    val totalScans: Int get() = entries.values.sumOf { it.count }
    val uniqueCount: Int get() = entries.size
    val unexpectedCount: Int get() = entries.keys.count { it !in expectedCodes && hasExpectedList }
    val duplicateScanCount: Int get() = entries.values.sumOf { (it.count - 1).coerceAtLeast(0) }

    /** Ma trong expectedCodes nhung chua tung duoc quet trong phien nay. */
    val missingCodes: List<String>
        get() = expectedCodes.filter { it !in entries.keys }.sorted()

    /**
     * Ghi nhan 1 lan quet. @return outcome de UI phat am thanh phan hoi
     * tuong ung (NEW_EXPECTED/NEW_UNEXPECTED dung tone khac DUPLICATE).
     */
    fun recordScan(content: String, format: String): Outcome {
        val existing = entries[content]
        if (existing != null) {
            existing.count++
            return Outcome.DUPLICATE
        }
        entries[content] = Entry(format, 1)
        return if (!hasExpectedList || content in expectedCodes) {
            Outcome.NEW_EXPECTED
        } else {
            Outcome.NEW_UNEXPECTED
        }
    }

    /** Danh sach dong cho bao cao, theo dung thu tu quet (insertion order). */
    fun rows(): List<Row> = entries.map { (content, entry) ->
        val status = when {
            !hasExpectedList -> if (entry.count > 1) "DUPLICATE" else "OK"
            content !in expectedCodes -> "UNEXPECTED"
            entry.count > 1 -> "DUPLICATE"
            else -> "OK"
        }
        val gs1 = Gs1Parser.parse(content)
        Row(content, entry.format, entry.count, status, gs1.gtin, gs1.lot, gs1.expiryDate, gs1.serial)
    }

    /** Cac ma ky vong nhung chua quet, xuat kem vao bao cao voi count=0. */
    fun missingRows(): List<Row> = missingCodes.map { code ->
        val gs1 = Gs1Parser.parse(code)
        Row(code, "", 0, "MISSING", gs1.gtin, gs1.lot, gs1.expiryDate, gs1.serial)
    }

    /** Snapshot cac dong da quet, dung de khoi phuc phien sau khi Activity bi tao lai (vd xoay man hinh). */
    fun snapshotEntries(): List<Triple<String, String, Int>> =
        entries.map { (content, entry) -> Triple(content, entry.format, entry.count) }

    /** Khoi phuc cac dong da quet tu snapshot. Chi goi ngay sau khi tao AuditSession moi. */
    fun restoreEntries(raw: List<Triple<String, String, Int>>) {
        raw.forEach { (content, format, count) -> entries[content] = Entry(format, count) }
    }

    /** Bao cao CSV day du (scanned rows + missing rows), sap xep de doc. */
    fun toCsv(delimiter: String = ","): String {
        val header = listOf(
            "content", "format", "count", "status", "gtin", "lot", "expiry", "serial"
        ).joinToString(delimiter)
        val allRows = rows() + missingRows()
        val body = allRows.joinToString("\n") { row ->
            listOf(
                row.content.quoteAndEscape(),
                row.format.quoteAndEscape(),
                row.count.toString(),
                row.status.quoteAndEscape(),
                (row.gtin ?: "").quoteAndEscape(),
                (row.lot ?: "").quoteAndEscape(),
                (row.expiryDate ?: "").quoteAndEscape(),
                (row.serial ?: "").quoteAndEscape(),
            ).joinToString(delimiter)
        }
        return if (body.isEmpty()) "$header\n" else "$header\n$body\n"
    }
}
