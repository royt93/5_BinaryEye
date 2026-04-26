package com.mckimquyen.binaryeye.view.media

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.mckimquyen.binaryeye.prefs

private const val TAG = "roy93~Beeps"

// [FIX ML-5] Boc ToneGenerator vao object de quan ly lifecycle ro rang
// Tranh native AudioTrack handle bi leak neu Activity crash truoc onDestroy
private object BeepManager {
    private var confirmGenerator: ToneGenerator? = null
    private var errorGenerator: ToneGenerator? = null

    fun beepConfirm() {
        val tg = confirmGenerator ?: tryCreate(AudioManager.STREAM_NOTIFICATION).also {
            confirmGenerator = it
        } ?: return
        tg.startTone(prefs.beepTone())
    }

    fun beepError() {
        val tg = errorGenerator ?: tryCreate(AudioManager.STREAM_ALARM).also {
            errorGenerator = it
        } ?: return
        tg.startTone(ToneGenerator.TONE_SUP_ERROR)
    }

    fun release() {
        try { confirmGenerator?.release() } catch (e: Exception) {
            Log.w(TAG, "release confirmGenerator error", e)
        }
        confirmGenerator = null
        try { errorGenerator?.release() } catch (e: Exception) {
            Log.w(TAG, "release errorGenerator error", e)
        }
        errorGenerator = null
    }

    private fun tryCreate(streamType: Int): ToneGenerator? = try {
        ToneGenerator(streamType, ToneGenerator.MAX_VOLUME)
    } catch (e: Exception) {
        // ToneGenerator co the throw RuntimeException neu audio service khong san sang
        Log.w(TAG, "tryCreate ToneGenerator failed for streamType=$streamType", e)
        null
    }
}

fun beepConfirm() = BeepManager.beepConfirm()

fun beepError() = BeepManager.beepError()

fun releaseToneGenerators() = BeepManager.release()
