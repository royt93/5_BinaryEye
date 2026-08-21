package com.mckimquyen.binaryeye.view.actions.vietqr

/**
 * [FEAT FEAT-NEW-01] Parser cho VietQR/NAPAS 247 - profile EMVCo Merchant
 * Presented Mode danh rieng cho chuyen khoan ngan hang tai VN. Chi nhan dien
 * QR co GUID NAPAS ("A000000727") trong truong 38 (Merchant Account
 * Information) de tranh nhan nham QR thanh toan EMVCo cua nuoc khac/mang
 * khac ma app khong the tach dung thong tin. Thuan, khong phu thuoc Android
 * framework.
 */
object VietQrParser {

    private const val NAPAS_GUID = "A000000727"
    private const val TAG_PAYLOAD_FORMAT_INDICATOR = "00"
    private const val TAG_MERCHANT_ACCOUNT_INFO = "38"
    private const val TAG_TRANSACTION_AMOUNT = "54"
    private const val TAG_TRANSACTION_CURRENCY = "53"
    private const val TAG_MERCHANT_NAME = "59"
    private const val TAG_ADDITIONAL_DATA = "62"
    private const val SUBTAG_GUID = "00"
    private const val SUBTAG_BANK_ACCOUNT = "01"
    private const val SUBTAG_BANK_BIN = "00"
    private const val SUBTAG_ACCOUNT_NUMBER = "01"
    private const val SUBTAG_PURPOSE_MESSAGE = "08"

    data class Info(
        val bankBin: String?,
        val bankName: String?,
        val accountNumber: String?,
        val amount: String?,
        val currency: String?,
        val merchantName: String?,
        val message: String?,
    )

    /** @return null neu khong phai QR VietQR/NAPAS hop le (thieu GUID NAPAS). */
    fun parse(raw: String): Info? {
        val topFields = EmvQrParser.parseTlv(raw)
        if (topFields.isEmpty()) return null
        val byTag = topFields.associateBy { it.tag }
        if (byTag[TAG_PAYLOAD_FORMAT_INDICATOR]?.value != "01") return null

        val merchantAccountInfo = byTag[TAG_MERCHANT_ACCOUNT_INFO]?.value ?: return null
        val subFields = EmvQrParser.parseTlv(merchantAccountInfo)
        val guid = subFields.firstOrNull { it.tag == SUBTAG_GUID }?.value
        if (guid != NAPAS_GUID) return null

        val bankAccountTlv = subFields.firstOrNull { it.tag == SUBTAG_BANK_ACCOUNT }?.value
        val bankFields = bankAccountTlv?.let { EmvQrParser.parseTlv(it) }.orEmpty()
        val bankBin = bankFields.firstOrNull { it.tag == SUBTAG_BANK_BIN }?.value
        val accountNumber = bankFields.firstOrNull { it.tag == SUBTAG_ACCOUNT_NUMBER }?.value

        val additionalData = byTag[TAG_ADDITIONAL_DATA]?.value?.let { EmvQrParser.parseTlv(it) }
        val message = additionalData?.firstOrNull { it.tag == SUBTAG_PURPOSE_MESSAGE }?.value

        return Info(
            bankBin = bankBin,
            bankName = bankBin?.let { VietBankDirectory.nameForBin(it) },
            accountNumber = accountNumber,
            amount = byTag[TAG_TRANSACTION_AMOUNT]?.value,
            currency = byTag[TAG_TRANSACTION_CURRENCY]?.value,
            merchantName = byTag[TAG_MERCHANT_NAME]?.value,
            message = message,
        )
    }
}
