package com.mckimquyen.binaryeye.ext.app

import android.content.Context
import android.os.Build
import java.util.*

fun Context.applyLocale(localeName: String) {
    if (localeName.isEmpty()) {
        return
    }
    val localeParts = localeName.split("-")
    val locale = if (localeParts.size == 2) {
        // [FIX BUG-06] localeParts[1] mang tien to Android resource-qualifier
        // "r" (vd "zh-rCN") chu khong phai ma quoc gia ISO 3166 hop le ("CN") -
        // phai bo tien to nay thi Locale moi khop dung values-zh-rCN/...
        Locale(localeParts[0], localeParts[1].removePrefix("r"))
    } else {
        Locale(localeName)
    }
    Locale.setDefault(locale)
    val conf = resources.configuration
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        @Suppress("DEPRECATION")
        conf.locale = locale
    } else {
        conf.setLocale(locale)
    }
    resources.updateConfiguration(conf, resources.displayMetrics)
}
