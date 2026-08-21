package com.mckimquyen.binaryeye.view.content

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

fun Context.copyToClipboard(
    text: String,
    isSensitive: Boolean = false,
    // [FEAT Incognito] Neu khac null, tu xoa clipboard sau khoang thoi gian
    // nay - dung cho che do quet an danh voi noi dung nhay cam (mat khau
    // WiFi, the tin dung...). Chi xoa neu clipboard van con dung noi dung
    // da copy (khong ghi de neu user da copy thu khac trong luc cho).
    autoClearAfterMs: Long? = null,
) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(
        ClipData.newPlainText("plain text", text).apply {
            setSensitive(isSensitive)
        }
    )
    if (autoClearAfterMs != null) {
        Handler(Looper.getMainLooper()).postDelayed({
            val current = clipboardManager.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString()
            if (current == text) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }, autoClearAfterMs)
    }
}

private fun ClipData.setSensitive(isSensitive: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        description.extras = PersistableBundle().apply {
            putBoolean(EXTRA_IS_SENSITIVE, isSensitive)
        }
    }
}

private val EXTRA_IS_SENSITIVE = if (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
) {
    ClipDescription.EXTRA_IS_SENSITIVE
} else {
    "android.content.extra.IS_SENSITIVE"
}
