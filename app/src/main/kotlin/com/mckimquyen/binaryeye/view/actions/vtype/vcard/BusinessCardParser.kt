package com.mckimquyen.binaryeye.view.actions.vtype.vcard

/**
 * [FEAT F10] OCR -> VCARD. Nhan biet cac truong co cau truc (ten/SDT/email/
 * cong ty/chuc danh) tu danh sach dong text tho lay tu OCR (ML Kit Text
 * Recognition). Heuristic don gian, khong hoan hao - nhung du dung cho MVP
 * va nguoi dung van sua tay duoc truoc khi luu. Thuan, khong phu thuoc
 * Android/ML Kit nen unit-test duoc tren JVM.
 */
object BusinessCardParser {

    data class ParsedCard(
        val name: String = "",
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val company: String = "",
        val title: String = "",
    )

    private val EMAIL_REGEX = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
    private val PHONE_REGEX = Regex("""\+?[\d][\d\s().-]{5,}\d""")

    private val COMPANY_KEYWORDS = listOf(
        "ltd", "inc", "corp", "jsc", "company", "group",
        "cong ty", "công ty", "tnhh", "co.,"
    )
    private val TITLE_KEYWORDS = listOf(
        "manager", "director", "ceo", "cto", "cfo", "founder", "president", "chairman",
        "giam doc", "giám đốc", "truong phong", "trưởng phòng", "nhan vien", "nhân viên"
    )

    fun parse(lines: List<String>): ParsedCard {
        val trimmed = lines.map { it.trim() }.filter { it.isNotEmpty() }
        val emails = mutableListOf<String>()
        val phones = mutableListOf<String>()
        var company: String? = null
        var title: String? = null
        val remaining = mutableListOf<String>()

        for (line in trimmed) {
            val email = EMAIL_REGEX.find(line)?.value
            val phone = PHONE_REGEX.find(line)?.value
            val digitCount = line.count { it.isDigit() }
            val lower = line.lowercase()
            when {
                email != null -> emails.add(email)
                phone != null && digitCount >= 7 -> phones.add(phone.trim())
                company == null && COMPANY_KEYWORDS.any { lower.contains(it) } -> company = line
                title == null && TITLE_KEYWORDS.any { lower.contains(it) } -> title = line
                else -> remaining.add(line)
            }
        }

        return ParsedCard(
            name = remaining.firstOrNull().orEmpty(),
            phones = phones,
            emails = emails,
            company = company.orEmpty(),
            title = title.orEmpty(),
        )
    }

    /** Chuyen ParsedCard thanh vCard 3.0 text - dung dinh dang ma VTypeParser/VCardAction da parse duoc. */
    fun toVCard(card: ParsedCard): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        if (card.name.isNotBlank()) appendLine("FN:${card.name}")
        card.phones.forEach { appendLine("TEL:$it") }
        card.emails.forEach { appendLine("EMAIL:$it") }
        if (card.company.isNotBlank()) appendLine("ORG:${card.company}")
        if (card.title.isNotBlank()) appendLine("TITLE:${card.title}")
        appendLine("END:VCARD")
    }
}
