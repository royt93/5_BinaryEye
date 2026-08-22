package com.mckimquyen.binaryeye.database

data class ScanFilter(
    val query: String? = null,
    val formatGroup: FormatGroup = FormatGroup.ALL,
    val dateRange: DateRange = DateRange.ALL,
    // [FEAT F3] 1 tag duy nhat de loc (vd "Work") - scan phai co dung tag nay
    // trong CSV cua no de match, khong phai substring.
    val tag: String? = null,
) {
    val isDefault: Boolean
        get() = query.isNullOrEmpty() &&
            formatGroup == FormatGroup.ALL &&
            dateRange == DateRange.ALL &&
            tag.isNullOrEmpty()

    enum class FormatGroup { ALL, QR, BARCODE_1D, OTHER_2D }
    enum class DateRange { ALL, TODAY, WEEK, MONTH }

    /**
     * Builds the SQL WHERE clause for this filter. Pure function (no Android
     * dependency) so it can be unit-tested on the JVM.
     *
     * Note: [SCANS_DATETIME] is stored in *local* time (see Scan.getDateTime),
     * so the date predicates use SQLite's 'localtime' modifier to keep
     * 'now' in the same timezone — otherwise Today/Week/Month would be off
     * by the UTC offset near day boundaries.
     */
    fun toWhereClause(): String {
        val parts = mutableListOf<String>()
        if (!query.isNullOrEmpty()) {
            parts += "(${Db.SCANS_CONTENT} LIKE ? OR ${Db.SCANS_NAME} LIKE ?)"
        }
        when (formatGroup) {
            FormatGroup.QR ->
                parts += "${Db.SCANS_FORMAT} IN ('QR_CODE','MICRO_QR_CODE','RM_QR_CODE')"
            FormatGroup.BARCODE_1D ->
                parts += "${Db.SCANS_FORMAT} IN ('EAN_8','EAN_13','CODE_39','CODE_93','CODE_128','UPC_A','UPC_E','ITF','CODABAR','DATA_BAR','DATA_BAR_EXPANDED')"
            FormatGroup.OTHER_2D ->
                parts += "${Db.SCANS_FORMAT} IN ('PDF_417','DATA_MATRIX','AZTEC','MAXICODE')"
            FormatGroup.ALL -> {}
        }
        when (dateRange) {
            DateRange.TODAY ->
                parts += "date(${Db.SCANS_DATETIME}) = date('now','localtime')"
            DateRange.WEEK ->
                parts += "${Db.SCANS_DATETIME} >= datetime('now','-7 days','localtime')"
            DateRange.MONTH ->
                parts += "${Db.SCANS_DATETIME} >= datetime('now','-30 days','localtime')"
            DateRange.ALL -> {}
        }
        // [FEAT F3] Bao boc CSV bang dau phay o 2 dau (',tag1,tag2,') roi LIKE
        // '%,tag,%' de match dung 1 tag, tranh "Work" match nham "Workshop".
        if (!tag.isNullOrEmpty()) {
            parts += "(',' || ${Db.SCANS_TAGS} || ',') LIKE ?"
        }
        return if (parts.isEmpty()) "" else "WHERE ${parts.joinToString(" AND ")}"
    }

    /** Bind arguments matching the '?' placeholders in [toWhereClause], in order. */
    fun toWhereArgs(): Array<String>? {
        val args = mutableListOf<String>()
        if (!query.isNullOrEmpty()) {
            val like = "%$query%"
            args += like
            args += like
        }
        if (!tag.isNullOrEmpty()) {
            args += "%,$tag,%"
        }
        return if (args.isEmpty()) null else args.toTypedArray()
    }
}
