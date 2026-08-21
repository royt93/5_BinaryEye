package com.mckimquyen.binaryeye.view

import android.content.Context
import android.media.AudioManager
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.media.beepConfirm
import com.mckimquyen.binaryeye.view.media.beepError
import com.mckimquyen.binaryeye.view.os.error
import com.mckimquyen.binaryeye.view.os.getVibrator
import com.mckimquyen.binaryeye.view.os.vibrate

fun Context.scanFeedback() {
    if (prefs.vibrate) {
        getVibrator().vibrate()
    }
    if (prefs.beep && !isSilent()) {
        beepConfirm()
    }
}

fun Context.errorFeedback() {
    if (prefs.vibrate) {
        getVibrator().error()
    }
    if (prefs.beep && !isSilent()) {
        beepError()
    }
}

internal fun Context.isSilent(): Boolean {
    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return when (am.ringerMode) {
        AudioManager.RINGER_MODE_SILENT,
        AudioManager.RINGER_MODE_VIBRATE -> true

        else -> false
    }
}
