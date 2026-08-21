package com.mckimquyen.binaryeye.adapter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

private val FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

// Moc "now" co dinh: 2026-08-21 13:00:00 - tranh phu thuoc dong ho he thong.
private val NOW_MS = FORMAT.parse("2026-08-21 13:00:00")!!.time

class DateGroupTest {

    @Test
    fun sameCalendarDay_isToday() {
        assertEquals(DateGroup.TODAY, classifyDateGroup("2026-08-21 00:00:01", NOW_MS))
        assertEquals(DateGroup.TODAY, classifyDateGroup("2026-08-21 23:59:59", NOW_MS))
        assertEquals(DateGroup.TODAY, classifyDateGroup("2026-08-21 13:00:00", NOW_MS))
    }

    @Test
    fun oneDayBefore_isYesterday() {
        assertEquals(DateGroup.YESTERDAY, classifyDateGroup("2026-08-20 23:59:59", NOW_MS))
        assertEquals(DateGroup.YESTERDAY, classifyDateGroup("2026-08-20 00:00:00", NOW_MS))
    }

    @Test
    fun twoToSixDaysBefore_isThisWeek() {
        assertEquals(DateGroup.THIS_WEEK, classifyDateGroup("2026-08-19 12:00:00", NOW_MS))
        assertEquals(DateGroup.THIS_WEEK, classifyDateGroup("2026-08-15 12:00:00", NOW_MS))
    }

    @Test
    fun sevenOrMoreDaysBefore_isOlder() {
        assertEquals(DateGroup.OLDER, classifyDateGroup("2026-08-14 12:00:00", NOW_MS))
        assertEquals(DateGroup.OLDER, classifyDateGroup("2020-01-01 00:00:00", NOW_MS))
    }

    @Test
    fun unparsableDatetime_fallsBackToOlder() {
        assertEquals(DateGroup.OLDER, classifyDateGroup("garbage", NOW_MS))
        assertEquals(DateGroup.OLDER, classifyDateGroup("", NOW_MS))
    }

    @Test
    fun futureDatetime_treatedAsToday() {
        // Khong nen xay ra trong thuc te (clock skew) nhung phai xu ly an toan,
        // khong duoc crash hay roi vao nhom sai.
        assertEquals(DateGroup.TODAY, classifyDateGroup("2026-08-22 08:00:00", NOW_MS))
    }
}
