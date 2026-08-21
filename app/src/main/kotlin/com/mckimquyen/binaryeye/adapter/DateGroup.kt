package com.mckimquyen.binaryeye.adapter

import com.mckimquyen.binaryeye.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * [FEAT E2] Nhom ngay cho section header trong History - pure logic, khong
 * phu thuoc Android framework nen unit-test duoc tren JVM thuan.
 */
internal enum class DateGroup(val labelResId: Int) {
    TODAY(R.string.history_group_today),
    YESTERDAY(R.string.history_group_yesterday),
    THIS_WEEK(R.string.history_group_this_week),
    OLDER(R.string.history_group_older),
}

private val SCAN_DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

/**
 * @param datetimeText gia tri tho tu cot [com.mckimquyen.binaryeye.database.Db.SCANS_DATETIME]
 *   (gio dia phuong, dinh dang "yyyy-MM-dd HH:mm:ss").
 * @param nowMs moc thoi gian "hien tai" - truyen vao thay vi doc System.currentTimeMillis()
 *   ben trong de ham nay thuan (pure) va de-test.
 */
internal fun classifyDateGroup(datetimeText: String, nowMs: Long): DateGroup {
    val parsedMs = try {
        SCAN_DATETIME_FORMAT.parse(datetimeText)?.time
    } catch (e: ParseException) {
        null
    } ?: return DateGroup.OLDER

    val dayDiff = dayDifference(parsedMs, nowMs)
    return when {
        dayDiff <= 0 -> DateGroup.TODAY
        dayDiff == 1L -> DateGroup.YESTERDAY
        dayDiff in 2..6 -> DateGroup.THIS_WEEK
        else -> DateGroup.OLDER
    }
}

private fun dayDifference(thenMs: Long, nowMs: Long): Long {
    val thenDay = startOfDay(thenMs)
    val nowDay = startOfDay(nowMs)
    return (nowDay - thenDay) / (24 * 60 * 60 * 1000L)
}

private fun startOfDay(ms: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
