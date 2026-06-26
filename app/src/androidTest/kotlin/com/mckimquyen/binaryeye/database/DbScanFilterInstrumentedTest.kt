package com.mckimquyen.binaryeye.database

import android.database.Cursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mckimquyen.binaryeye.db
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test: chạy trên device với SQLite thật. Kiểm tra
 * Db.getScans(ScanFilter) end-to-end — gồm cả predicate ngày 'localtime'
 * và lọc theo nhóm format. Dọn sạch theo id trong @After để KHÔNG đụng
 * lịch sử thật của người dùng.
 */
@RunWith(AndroidJUnit4::class)
class DbScanFilterInstrumentedTest {

    private val insertedIds = mutableListOf<Long>()
    private val marker = "INSTRTEST_${System.nanoTime()}"

    @Before
    fun setup() {
        // RApp đã open db; đảm bảo chắc chắn đã mở.
        db.open(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After
    fun tearDown() {
        insertedIds.forEach { db.removeScan(it) }
        insertedIds.clear()
    }

    private fun insert(content: String, format: String, dateTime: String? = null): Long {
        val scan = if (dateTime != null) {
            Scan(content = content, raw = null, format = format, dateTime = dateTime)
        } else {
            Scan(content = content, raw = null, format = format)
        }
        val id = db.insertScan(scan)
        insertedIds += id
        return id
    }

    private fun idsFor(filter: ScanFilter): Set<Long> {
        val ids = mutableSetOf<Long>()
        db.getScans(filter)?.use { c: Cursor ->
            val idx = c.getColumnIndexOrThrow(Db.SCANS_ID)
            while (c.moveToNext()) ids += c.getLong(idx)
        }
        return ids
    }

    @Test
    fun dateFilter_today_includesNow_excludesOld() {
        val todayId = insert("$marker-today", "QR_CODE")
        val oldId = insert("$marker-old", "QR_CODE", dateTime = "2020-01-01 08:00:00")

        val today = idsFor(ScanFilter(dateRange = ScanFilter.DateRange.TODAY))
        assertTrue("scan hôm nay phải nằm trong filter Today", todayId in today)
        assertFalse("scan 2020 không được nằm trong Today", oldId in today)
    }

    @Test
    fun dateFilter_weekAndMonth_excludeOld() {
        val todayId = insert("$marker-recent", "QR_CODE")
        val oldId = insert("$marker-ancient", "QR_CODE", dateTime = "2020-01-01 08:00:00")

        idsFor(ScanFilter(dateRange = ScanFilter.DateRange.WEEK)).let {
            assertTrue(todayId in it); assertFalse(oldId in it)
        }
        idsFor(ScanFilter(dateRange = ScanFilter.DateRange.MONTH)).let {
            assertTrue(todayId in it); assertFalse(oldId in it)
        }
    }

    @Test
    fun formatFilter_qrVsBarcode1d_partitionsCorrectly() {
        val qrId = insert("$marker-qr", "QR_CODE")
        val eanId = insert("$marker-ean", "EAN_13")

        idsFor(ScanFilter(formatGroup = ScanFilter.FormatGroup.QR)).let {
            assertTrue(qrId in it); assertFalse(eanId in it)
        }
        idsFor(ScanFilter(formatGroup = ScanFilter.FormatGroup.BARCODE_1D)).let {
            assertTrue(eanId in it); assertFalse(qrId in it)
        }
    }

    @Test
    fun formatFilter_aztecIsOther2d_notQr() {
        val aztecId = insert("$marker-aztec", "AZTEC")
        idsFor(ScanFilter(formatGroup = ScanFilter.FormatGroup.OTHER_2D)).let {
            assertTrue(aztecId in it)
        }
        idsFor(ScanFilter(formatGroup = ScanFilter.FormatGroup.QR)).let {
            assertFalse(aztecId in it)
        }
    }

    @Test
    fun queryFilter_matchesContent() {
        val matchId = insert("$marker-needle", "QR_CODE")
        val otherId = insert("$marker-haystack", "QR_CODE")
        val found = idsFor(ScanFilter(query = "$marker-needle"))
        assertTrue(matchId in found)
        assertFalse(otherId in found)
    }

    @Test
    fun combinedFilter_queryAndFormatAndDate() {
        val hit = insert("$marker-combo", "QR_CODE")
        val wrongFormat = insert("$marker-combo", "EAN_13")
        val found = idsFor(
            ScanFilter(
                query = "$marker-combo",
                formatGroup = ScanFilter.FormatGroup.QR,
                dateRange = ScanFilter.DateRange.TODAY
            )
        )
        assertTrue(hit in found)
        assertFalse(wrongFormat in found)
    }
}
