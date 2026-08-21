package com.mckimquyen.binaryeye.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.mckimquyen.binaryeye.R

class UrlPref(
    context: Context,
    attrs: AttributeSet?,
) : DialogPreference(context, attrs) {
    private var url: String? = null

    init {
        dialogLayoutResource = R.layout.roy_dlg_url
    }

    fun getUrl() = url

    fun setUrl(url: String) {
        this.url = url
        persistString(url)
    }

    @Deprecated("Deprecated in Java")
    override fun onSetInitialValue(
        restorePersistedValue: Boolean,
        defaultValue: Any?,
    ) {
        setUrl(
            if (restorePersistedValue) {
                getPersistedString(url)
            } else {
                defaultValue as String
            }
        )
    }
}
