package com.mckimquyen.binaryeye.database

import com.mckimquyen.binaryeye.database.ScanFilter.DateRange
import com.mckimquyen.binaryeye.database.ScanFilter.FormatGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFilterTest {

    @Test
    fun defaultFilter_isDefault_andEmptySql() {
        val f = ScanFilter()
        assertTrue(f.isDefault)
        assertEquals("", f.toWhereClause())
        assertNull(f.toWhereArgs())
    }

    @Test
    fun anyNonDefaultField_makesNotDefault() {
        assertFalse(ScanFilter(query = "x").isDefault)
        assertFalse(ScanFilter(formatGroup = FormatGroup.QR).isDefault)
        assertFalse(ScanFilter(dateRange = DateRange.TODAY).isDefault)
        // Blank query is treated as no query.
        assertTrue(ScanFilter(query = "").isDefault)
    }

    @Test
    fun query_buildsLikeClauseWithTwoArgs() {
        val f = ScanFilter(query = "abc")
        assertEquals("WHERE (content LIKE ? OR name LIKE ?)", f.toWhereClause())
        assertArrayEquals2(arrayOf("%abc%", "%abc%"), f.toWhereArgs())
    }

    @Test
    fun formatGroups_mapToExpectedInClause() {
        assertTrue(
            ScanFilter(formatGroup = FormatGroup.QR).toWhereClause()
                .contains("format IN ('QR_CODE','MICRO_QR_CODE','RM_QR_CODE')")
        )
        assertTrue(
            ScanFilter(formatGroup = FormatGroup.BARCODE_1D).toWhereClause()
                .contains("EAN_13")
        )
        assertTrue(
            ScanFilter(formatGroup = FormatGroup.OTHER_2D).toWhereClause()
                .contains("format IN ('PDF_417','DATA_MATRIX','AZTEC','MAXICODE')")
        )
    }

    @Test
    fun dateRanges_useLocaltimeModifier() {
        // Regression: stored datetime is local, so 'now' must be localtime too.
        assertEquals(
            "WHERE date(_datetime) = date('now','localtime')",
            ScanFilter(dateRange = DateRange.TODAY).toWhereClause()
        )
        assertEquals(
            "WHERE _datetime >= datetime('now','-7 days','localtime')",
            ScanFilter(dateRange = DateRange.WEEK).toWhereClause()
        )
        assertEquals(
            "WHERE _datetime >= datetime('now','-30 days','localtime')",
            ScanFilter(dateRange = DateRange.MONTH).toWhereClause()
        )
    }

    @Test
    fun combinedFilters_joinedWithAnd() {
        val f = ScanFilter(
            query = "q",
            formatGroup = FormatGroup.QR,
            dateRange = DateRange.TODAY
        )
        assertEquals(
            "WHERE (content LIKE ? OR name LIKE ?) AND " +
                "format IN ('QR_CODE','MICRO_QR_CODE','RM_QR_CODE') AND " +
                "date(_datetime) = date('now','localtime')",
            f.toWhereClause()
        )
        assertArrayEquals2(arrayOf("%q%", "%q%"), f.toWhereArgs())
    }

    @Test
    fun args_nullWhenNoQuery_evenWithOtherFilters() {
        assertNull(ScanFilter(formatGroup = FormatGroup.QR).toWhereArgs())
        assertNull(ScanFilter(dateRange = DateRange.MONTH).toWhereArgs())
    }

    @Test
    fun barcode1d_listContainsAllExpectedFormats() {
        val sql = ScanFilter(formatGroup = FormatGroup.BARCODE_1D).toWhereClause()
        listOf("EAN_8", "EAN_13", "CODE_39", "CODE_93", "CODE_128",
            "UPC_A", "UPC_E", "ITF", "CODABAR", "DATA_BAR", "DATA_BAR_EXPANDED")
            .forEach { assertTrue("thiếu $it", sql.contains("'$it'")) }
    }

    @Test
    fun queryLikeArgs_wrapWithPercent() {
        assertArrayEquals2(arrayOf("%a b%", "%a b%"), ScanFilter(query = "a b").toWhereArgs())
        // Ký tự đặc biệt được giữ nguyên trong arg (đã parameterized nên an toàn injection).
        assertArrayEquals2(arrayOf("%a'b%", "%a'b%"), ScanFilter(query = "a'b").toWhereArgs())
    }

    @Test
    fun whitespaceQuery_isTreatedAsAQuery() {
        // " " không rỗng → vẫn tạo mệnh đề LIKE (tài liệu hoá hành vi hiện tại).
        val f = ScanFilter(query = " ")
        assertTrue(f.toWhereClause().contains("LIKE ?"))
        assertFalse(f.isDefault)
    }

    @Test
    fun clauseUsesParameterizedPlaceholders_noInlineQueryValue() {
        // Giá trị query KHÔNG được nội suy vào câu SQL (chống injection).
        val sql = ScanFilter(query = "DROP TABLE scans").toWhereClause()
        assertFalse(sql.contains("DROP TABLE"))
        assertTrue(sql.contains("LIKE ?"))
    }

    @Test
    fun queryAndDate_noFormat_joinedWithAnd() {
        val sql = ScanFilter(query = "q", dateRange = DateRange.WEEK).toWhereClause()
        assertEquals(
            "WHERE (content LIKE ? OR name LIKE ?) AND " +
                "_datetime >= datetime('now','-7 days','localtime')",
            sql
        )
    }

    private fun assertArrayEquals2(expected: Array<String>, actual: Array<String>?) {
        assertEquals(expected.toList(), actual?.toList())
    }
}
