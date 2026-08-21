package com.mckimquyen.binaryeye.pref

import android.content.Context
import android.content.SharedPreferences
import android.media.ToneGenerator
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat

class Pref {
    companion object {
        private const val BARCODE_FORMATS = "formats"
        private const val CROP_HANDLE_X = "crop_handle_x"
        private const val CROP_HANDLE_Y = "crop_handle_y"
        private const val CROP_HANDLE_ORIENTATION = "crop_handle_orientation"
        private const val SHOW_CROP_HANDLE = "show_crop_handle"
        private const val ZOOM_BY_SWIPING = "zoom_by_swiping"
        private const val AUTO_ROTATE = "auto_rotate"
        private const val TRY_HARDER = "try_harder"
        private const val BULK_MODE = "bulk_mode"
        private const val BULK_MODE_DELAY = "bulk_mode_delay"
        private const val SHOW_TOAST_IN_BULK_MODE = "show_toast_in_bulk_mode"
        private const val VIBRATE = "vibrate"
        private const val BEEP = "beep"
        private const val BEEP_TONE_NAME = "beep_tone_name"
        private const val USE_HISTORY = "use_history"
        private const val IGNORE_CONSECUTIVE_DUPLICATES = "ignore_consecutive_duplicates"
        private const val OPEN_IMMEDIATELY = "open_immediately"
        private const val COPY_IMMEDIATELY = "copy_immediately"
        private const val SHOW_META_DATA = "show_meta_data"
        private const val SHOW_HEX_DUMP = "show_hex_dump"
        private const val SHOW_RECREATION = "show_recreation"
        private const val CLOSE_AUTOMATICALLY = "close_automatically"
        private const val DEFAULT_SEARCH_URL = "default_search_url"
        private const val OPEN_WITH_URL = "open_with_url"
        private const val SEND_SCAN_ACTIVE = "send_scan_active"
        private const val SEND_SCAN_URL = "send_scan_url"
        private const val SEND_SCAN_TYPE = "send_scan_type"
        private const val SEND_SCAN_BLUETOOTH = "send_scan_bluetooth"
        private const val SEND_SCAN_BLUETOOTH_HOST = "send_scan_bluetooth_host"
        private const val CUSTOM_LOCALE = "custom_locale"
        private const val HAS_SHOWN_LANGUAGE_DIALOG = "has_shown_language_dialog"
        private const val INDEX_OF_LAST_SELECTED_FORMAT = "index_of_last_selected_format"
        private const val INDEX_OF_LAST_SELECTED_EC_LEVEL = "index_of_last_selected_ec_level"
        private const val FREE_ROTATION = "free_rotation"
        private const val EXPAND_ESCAPE_SEQUENCES = "expand_escape_sequences"
        private const val LAST_FOREGROUND_MS = "last_foreground_ms"
        private const val AUTO_TORCH = "auto_torch"
        private const val LOCK_HISTORY = "lock_history"
        private const val INCOGNITO_MODE = "incognito_mode"
    }

    lateinit var preferences: SharedPreferences

    var barcodeFormats = setOf(
        BarcodeFormat.AZTEC.name,
        BarcodeFormat.CODABAR.name,
        BarcodeFormat.CODE_39.name,
        BarcodeFormat.CODE_93.name,
        BarcodeFormat.CODE_128.name,
        BarcodeFormat.DATA_BAR.name,
        BarcodeFormat.DATA_BAR_EXPANDED.name,
        BarcodeFormat.DATA_MATRIX.name,
        BarcodeFormat.EAN_8.name,
        BarcodeFormat.EAN_13.name,
        BarcodeFormat.ITF.name,
        BarcodeFormat.MAXICODE.name,
        BarcodeFormat.PDF_417.name,
        BarcodeFormat.QR_CODE.name,
        BarcodeFormat.MICRO_QR_CODE.name,
        BarcodeFormat.UPC_A.name,
        BarcodeFormat.UPC_E.name,
    )
        set(value) {
            apply(BARCODE_FORMATS, value)
            field = value
        }
    var cropHandleX = -2 // -2 means set default roi.
        set(value) {
            apply(CROP_HANDLE_X, value)
            field = value
        }
    var cropHandleY = -2
        set(value) {
            apply(CROP_HANDLE_Y, value)
            field = value
        }
    var cropHandleOrientation = 0
        set(value) {
            apply(CROP_HANDLE_ORIENTATION, value)
            field = value
        }
    var showCropHandle = true
        set(value) {
            apply(SHOW_CROP_HANDLE, value)
            field = value
        }
    var zoomBySwiping = true
        set(value) {
            apply(ZOOM_BY_SWIPING, value)
            field = value
        }
    var autoRotate = true
        set(value) {
            apply(AUTO_ROTATE, value)
            field = value
        }
    var tryHarder = false
        set(value) {
            apply(TRY_HARDER, value)
            field = value
        }
    // [FEAT E1] Tu dong bat torch khi anh sang thap
    var autoTorch = false
        set(value) {
            apply(AUTO_TORCH, value)
            field = value
        }
    // [FEAT F8] Khoa man History bang van tay/PIN (VIP-exclusive)
    var lockHistory = false
        set(value) {
            apply(LOCK_HISTORY, value)
            field = value
        }
    // [FEAT Incognito] Khong luu vao history, tu xoa clipboard sau 60s
    var incognitoMode = false
        set(value) {
            apply(INCOGNITO_MODE, value)
            field = value
        }
    var bulkMode = false
        set(value) {
            apply(BULK_MODE, value)
            field = value
        }
    var bulkModeDelay = "500"
        set(value) {
            apply(BULK_MODE_DELAY, value)
            field = value
        }
    var showToastInBulkMode = true
        set(value) {
            apply(SHOW_TOAST_IN_BULK_MODE, value)
            field = value
        }
    var vibrate = true
        set(value) {
            apply(VIBRATE, value)
            field = value
        }
    var beep = false
        set(value) {
            apply(BEEP, value)
            field = value
        }
    private var beepToneName = "tone_prop_beep"
        set(value) {
            apply(BEEP_TONE_NAME, value)
            field = value
        }
    var useHistory = false
        set(value) {
            apply(USE_HISTORY, value)
            field = value
        }
    var ignoreConsecutiveDuplicates = true
        set(value) {
            apply(IGNORE_CONSECUTIVE_DUPLICATES, value)
            field = value
        }
    var copyImmediately = false
        set(value) {
            apply(COPY_IMMEDIATELY, value)
            field = value
        }
    var openImmediately = false
        set(value) {
            apply(OPEN_IMMEDIATELY, value)
            field = value
        }
    var showMetaData = true
        set(value) {
            apply(SHOW_META_DATA, value)
            field = value
        }
    var showHexDump = true
        set(value) {
            apply(SHOW_HEX_DUMP, value)
            field = value
        }
    var showRecreation = true
        set(value) {
            apply(SHOW_RECREATION, value)
            field = value
        }
    var closeAutomatically = false
        set(value) {
            apply(CLOSE_AUTOMATICALLY, value)
            field = value
        }
    var defaultSearchUrl = ""
        set(value) {
            apply(DEFAULT_SEARCH_URL, value)
            field = value
        }
    var openWithUrl: String = ""
        set(value) {
            apply(OPEN_WITH_URL, value)
            field = value
        }
    var sendScanActive = true
        set(value) {
            apply(SEND_SCAN_ACTIVE, value)
            field = value
        }
    var sendScanUrl: String = ""
        set(value) {
            apply(SEND_SCAN_URL, value)
            field = value
        }
    var sendScanType: String = "0"
        set(value) {
            apply(SEND_SCAN_TYPE, value)
            field = value
        }
    var sendScanBluetooth = false
        set(value) {
            apply(SEND_SCAN_BLUETOOTH, value)
            field = value
        }
    var sendScanBluetoothHost: String = ""
        set(value) {
            apply(SEND_SCAN_BLUETOOTH_HOST, value)
            field = value
        }
    var customLocale: String = ""
        set(value) {
            // Make sure this setting is written immediately because
            // the app is about to restart.
            commit(CUSTOM_LOCALE, value)
            field = value
        }
    // Flag: chỉ show language dialog 1 lần duy nhất khi cài app lần đầu
    var hasShownLanguageDialog: Boolean = false
        set(value) {
            commit(HAS_SHOWN_LANGUAGE_DIALOG, value)
            field = value
        }
    var indexOfLastSelectedFormat: Int = 0
        set(value) {
            apply(INDEX_OF_LAST_SELECTED_FORMAT, value)
            field = value
        }
    var indexOfLastSelectedEcLevel: Int = 0
        set(value) {
            apply(INDEX_OF_LAST_SELECTED_EC_LEVEL, value)
            field = value
        }
    var freeRotation = true
        set(value) {
            apply(FREE_ROTATION, value)
            field = value
        }
    var expandEscapeSequences = true
        set(value) {
            apply(EXPAND_ESCAPE_SEQUENCES, value)
            field = value
        }
    // [FEAT E8] Moc thoi gian foreground gan nhat, dung de skip splash ad neu vua mo gan day
    var lastForegroundMs: Long = 0L
        set(value) {
            apply(LAST_FOREGROUND_MS, value)
            field = value
        }

    fun init(context: Context) {
        // [FIX MED-1] PreferenceManager.getDefaultSharedPreferences deprecated tu API 29
        // Dung context.getSharedPreferences() thay the, ket qua tuong duong
        preferences = context.getSharedPreferences(
            "${context.packageName}_preferences",
            android.content.Context.MODE_PRIVATE
        )
        update()
    }

    fun update() {
        preferences.getStringSet(BARCODE_FORMATS, barcodeFormats)?.let {
            barcodeFormats = it
        }
        cropHandleX = preferences.getInt(CROP_HANDLE_X, cropHandleX)
        cropHandleY = preferences.getInt(CROP_HANDLE_Y, cropHandleY)
        cropHandleOrientation = preferences.getInt(
            CROP_HANDLE_ORIENTATION,
            cropHandleOrientation
        )
        showCropHandle = preferences.getBoolean(
            SHOW_CROP_HANDLE,
            showCropHandle
        )
        zoomBySwiping = preferences.getBoolean(ZOOM_BY_SWIPING, zoomBySwiping)
        autoRotate = preferences.getBoolean(AUTO_ROTATE, autoRotate)
        tryHarder = preferences.getBoolean(TRY_HARDER, tryHarder)
        autoTorch = preferences.getBoolean(AUTO_TORCH, autoTorch)
        lockHistory = preferences.getBoolean(LOCK_HISTORY, lockHistory)
        incognitoMode = preferences.getBoolean(INCOGNITO_MODE, incognitoMode)
        bulkMode = preferences.getBoolean(BULK_MODE, bulkMode)
        bulkModeDelay = preferences.getString(
            BULK_MODE_DELAY,
            bulkModeDelay
        ) ?: bulkModeDelay
        showToastInBulkMode = preferences.getBoolean(
            SHOW_TOAST_IN_BULK_MODE,
            showToastInBulkMode
        )
        vibrate = preferences.getBoolean(VIBRATE, vibrate)
        beep = preferences.getBoolean(BEEP, beep)
        preferences.getString(BEEP_TONE_NAME, beepToneName)?.also {
            beepToneName = it
        }
        useHistory = preferences.getBoolean(USE_HISTORY, useHistory)
        ignoreConsecutiveDuplicates = preferences.getBoolean(
            IGNORE_CONSECUTIVE_DUPLICATES,
            ignoreConsecutiveDuplicates
        )
        copyImmediately = preferences.getBoolean(
            COPY_IMMEDIATELY,
            copyImmediately
        )
        openImmediately = preferences.getBoolean(
            OPEN_IMMEDIATELY,
            openImmediately
        )
        showMetaData = preferences.getBoolean(SHOW_META_DATA, showMetaData)
        showHexDump = preferences.getBoolean(SHOW_HEX_DUMP, showHexDump)
        showRecreation = preferences.getBoolean(SHOW_RECREATION, showRecreation)
        closeAutomatically = preferences.getBoolean(
            CLOSE_AUTOMATICALLY,
            closeAutomatically
        )
        preferences.getString(DEFAULT_SEARCH_URL, defaultSearchUrl)?.also {
            defaultSearchUrl = it
        }
        preferences.getString(OPEN_WITH_URL, openWithUrl)?.also {
            openWithUrl = it
        }
        sendScanActive = preferences.getBoolean(
            SEND_SCAN_ACTIVE,
            sendScanActive
        )
        preferences.getString(SEND_SCAN_URL, sendScanUrl)?.also {
            sendScanUrl = it
        }
        preferences.getString(SEND_SCAN_TYPE, sendScanType)?.also {
            sendScanType = it
        }
        sendScanBluetooth = preferences.getBoolean(
            SEND_SCAN_BLUETOOTH,
            sendScanBluetooth
        )
        preferences.getString(
            SEND_SCAN_BLUETOOTH_HOST,
            sendScanBluetoothHost
        )?.also {
            sendScanBluetoothHost = it
        }
        preferences.getString(CUSTOM_LOCALE, customLocale)?.also {
            customLocale = it
        }
        indexOfLastSelectedFormat = preferences.getInt(
            INDEX_OF_LAST_SELECTED_FORMAT,
            indexOfLastSelectedFormat
        )
        indexOfLastSelectedEcLevel = preferences.getInt(
            INDEX_OF_LAST_SELECTED_EC_LEVEL,
            indexOfLastSelectedEcLevel
        )
        freeRotation = preferences.getBoolean(FREE_ROTATION, freeRotation)
        expandEscapeSequences = preferences.getBoolean(
            EXPAND_ESCAPE_SEQUENCES,
            expandEscapeSequences
        )
        hasShownLanguageDialog = preferences.getBoolean(
            HAS_SHOWN_LANGUAGE_DIALOG,
            hasShownLanguageDialog
        )
        lastForegroundMs = preferences.getLong(LAST_FOREGROUND_MS, lastForegroundMs)
    }

    fun beepTone() = when (beepToneName) {
        "tone_cdma_confirm" -> ToneGenerator.TONE_CDMA_CONFIRM
        "tone_sup_radio_ack" -> ToneGenerator.TONE_SUP_RADIO_ACK
        "tone_prop_ack" -> ToneGenerator.TONE_PROP_ACK
        "tone_prop_beep" -> ToneGenerator.TONE_PROP_BEEP
        "tone_prop_beep2" -> ToneGenerator.TONE_PROP_BEEP2
        else -> ToneGenerator.TONE_PROP_BEEP
    }

    private fun put(label: String, value: Boolean) =
        preferences.edit().putBoolean(label, value)

    private fun put(label: String, value: String) =
        preferences.edit().putString(label, value)

    private fun apply(label: String, value: Boolean) {
        put(label, value).apply()
    }

    private fun apply(label: String, value: String) {
        preferences.edit().putString(label, value).apply()
    }

    private fun apply(label: String, value: Int) {
        preferences.edit().putInt(label, value).apply()
    }

    private fun apply(label: String, value: Long) {
        preferences.edit().putLong(label, value).apply()
    }

    private fun commit(label: String, value: Boolean) {
        preferences.edit().putBoolean(label, value).commit()
    }

    private fun commit(label: String, value: String) {
        preferences.edit().putString(label, value).commit()
    }

    private fun apply(label: String, value: Set<String>) {
        preferences.edit().putStringSet(label, value).apply()
    }
}
