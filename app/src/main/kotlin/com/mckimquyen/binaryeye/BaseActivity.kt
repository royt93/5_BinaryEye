package com.mckimquyen.binaryeye

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mckimquyen.binaryeye.ext.app.applyLocale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(context: Context) {
        context.applyLocale(prefs.customLocale)
        val override = Configuration(context.resources.configuration)
        override.fontScale = 1.0f
        applyOverrideConfiguration(override)
        super.attachBaseContext(context)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableAdaptiveRefreshRate()
        }
    }

    private fun enableAdaptiveRefreshRate() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display // Sử dụng API mới
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay // Fallback cho API thấp hơn
        }

        if (display != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val supportedModes = display.supportedModes
                val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = highestRefreshRateMode.modeId
                    }
                    println("Adaptive refresh rate applied: ${highestRefreshRateMode.refreshRate} Hz")
                }
            }
        }
    }
}