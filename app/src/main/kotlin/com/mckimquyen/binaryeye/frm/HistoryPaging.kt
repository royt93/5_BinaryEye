package com.mckimquyen.binaryeye.frm

/**
 * [FEAT-NEW-05] Logic thuan cho phan trang History - History truoc day load
 * het toan bo scan trong 1 lan query, anh huong hieu nang khi lich su lon.
 * Khong phu thuoc Android framework nen unit-test duoc tren JVM thuan.
 */
object HistoryPaging {

    /** Cuon gan den cuoi danh sach (trong pham vi [threshold] dong) thi nen tai them trang ke tiep. */
    fun shouldLoadMore(
        firstVisibleItem: Int,
        visibleItemCount: Int,
        totalItemCount: Int,
        threshold: Int,
    ): Boolean = totalItemCount > 0 &&
        firstVisibleItem + visibleItemCount >= totalItemCount - threshold

    /**
     * Neu so dong tra ve dung bang so dong da xin (LIMIT), co the con du lieu
     * o trang sau - chua the biet chac 100% ma khong query them 1 dong thua,
     * nhung day la du de tranh 1 lan fetch thua khi da het du lieu.
     */
    fun hasMorePages(returnedCount: Int, requestedLimit: Int): Boolean =
        returnedCount >= requestedLimit
}
