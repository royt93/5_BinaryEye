package com.mckimquyen.binaryeye.view.actions.vietqr

/**
 * [FEAT FEAT-NEW-01] BIN (Bank Identification Number) NAPAS -> ten ngan hang,
 * chi gom 10 ngan hang lon nhat/pho bien nhat de giam rui ro sai lech du lieu
 * - BIN khong nam trong danh sach van hien thi duoc (raw BIN), khong bao gio
 * chan luong parse chinh. Thuan, khong phu thuoc Android framework.
 */
object VietBankDirectory {

    private val BIN_TO_NAME = mapOf(
        "970436" to "Vietcombank",
        "970415" to "VietinBank",
        "970418" to "BIDV",
        "970405" to "Agribank",
        "970407" to "Techcombank",
        "970416" to "ACB",
        "970422" to "MB Bank",
        "970432" to "VPBank",
        "970403" to "Sacombank",
        "970423" to "TPBank",
    )

    fun nameForBin(bin: String): String? = BIN_TO_NAME[bin]
}
