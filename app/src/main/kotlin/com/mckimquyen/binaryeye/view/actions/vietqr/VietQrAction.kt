package com.mckimquyen.binaryeye.view.actions.vietqr

import android.content.Context
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.actions.IAction
import com.mckimquyen.binaryeye.view.content.copyToClipboard
import com.mckimquyen.binaryeye.view.widget.toast

/**
 * [FEAT FEAT-NEW-01] Nhan dien QR chuyen khoan ngan hang VietQR/NAPAS 247,
 * tach San/So tai khoan/So tien de hien thi de doc va copy nhanh. Mo thang
 * app ngan hang cu the (deep link rieng tung ngan hang) nam ngoai scope MVP -
 * khong co chuan chung, do tin cay thap, xem doc/task/done.
 */
object VietQrAction : IAction {
    override val iconResId = R.drawable.ic_action_bank
    override val titleResId = R.string.vietqr_copy_account

    override fun canExecuteOn(data: ByteArray): Boolean =
        VietQrParser.parse(String(data)) != null

    override fun displayText(context: Context, data: ByteArray): String? {
        val info = VietQrParser.parse(String(data)) ?: return null
        return buildString {
            val bankLabel = info.bankName ?: info.bankBin
            if (bankLabel != null) {
                append(context.getString(R.string.vietqr_bank_label))
                append(": ")
                append(bankLabel)
                append("\n")
            }
            info.accountNumber?.let {
                append(context.getString(R.string.vietqr_account_label))
                append(": ")
                append(it)
                append("\n")
            }
            info.amount?.let { amount ->
                append(context.getString(R.string.vietqr_amount_label))
                append(": ")
                append(amount)
                if (info.currency == "704") append(" VND")
                append("\n")
            }
            info.message?.let {
                append(context.getString(R.string.vietqr_message_label))
                append(": ")
                append(it)
            }
        }.trim()
    }

    override suspend fun execute(context: Context, data: ByteArray) {
        val account = VietQrParser.parse(String(data))?.accountNumber
        if (account == null) {
            context.toast(R.string.vietqr_no_account)
            return
        }
        context.copyToClipboard(account)
        context.toast(context.getString(R.string.vietqr_copied_toast, account))
    }
}
