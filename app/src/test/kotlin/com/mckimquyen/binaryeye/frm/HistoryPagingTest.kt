package com.mckimquyen.binaryeye.frm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPagingTest {

    @Test
    fun shouldLoadMore_emptyList_isFalse() {
        assertFalse(HistoryPaging.shouldLoadMore(0, 0, 0, 10))
    }

    @Test
    fun shouldLoadMore_farFromBottom_isFalse() {
        assertFalse(HistoryPaging.shouldLoadMore(0, 20, 200, 10))
    }

    @Test
    fun shouldLoadMore_withinThresholdOfBottom_isTrue() {
        // dang xem den dong 195/200, nguong 10 -> con 5 dong la het
        assertTrue(HistoryPaging.shouldLoadMore(175, 20, 200, 10))
    }

    @Test
    fun shouldLoadMore_exactlyAtThresholdBoundary_isTrue() {
        // firstVisible + visible = totalItemCount - threshold dung bang
        assertTrue(HistoryPaging.shouldLoadMore(170, 20, 200, 10))
    }

    @Test
    fun shouldLoadMore_alreadyAtVeryBottom_isTrue() {
        assertTrue(HistoryPaging.shouldLoadMore(180, 20, 200, 10))
    }

    @Test
    fun hasMorePages_returnedLessThanLimit_isFalse() {
        assertFalse(HistoryPaging.hasMorePages(37, 100))
    }

    @Test
    fun hasMorePages_returnedEqualsLimit_isTrue() {
        assertTrue(HistoryPaging.hasMorePages(100, 100))
    }

    @Test
    fun hasMorePages_zeroReturned_isFalse() {
        assertFalse(HistoryPaging.hasMorePages(0, 100))
    }

    @Test
    fun hasMorePages_zeroLimit_edgeCase() {
        // limit=0 thi returnedCount=0 >= 0 -> true theo dinh nghia, khong
        // phai kich ban thuc te (PAGE_SIZE luon > 0) nhung ghi lai hanh vi ro rang
        assertTrue(HistoryPaging.hasMorePages(0, 0))
    }
}
