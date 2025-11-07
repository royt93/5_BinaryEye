package com.mckimquyen.binaryeye.frm

import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import android.view.View
import android.widget.TextView
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.database.Scan
import com.mckimquyen.binaryeye.view.net.sendAsync
import com.mckimquyen.binaryeye.pref.UrlPref

class FUrlDialog : PreferenceDialogFragmentCompat() {
    private var urlView: TextView? = null
    private var testButton: TextView? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        urlView = view.findViewById(R.id.url)
        testButton = view.findViewById(R.id.testUrl)
        testButton?.setOnClickListener {
            testUrl(testButton)
        }
        urlView?.text = urlPreference().getUrl()
        urlView?.hint = getString(
            when (prefs.sendScanType) {
                "0" -> R.string.urlHintAddContent
                else -> R.string.urlHint
            }
        )
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            urlPreference().setUrl(getUrl())
        }
    }

    private fun getUrl() = completeUrl(urlView?.text.toString())

    private fun urlPreference() = preference as UrlPref

    private fun testUrl(textView: TextView?) {
        val url = getUrl()
        if (url.isEmpty()) {
            return
        }
        textView ?: return
        textView.text = "…"
        Scan(
            content = "test",
            raw = null,
            format = "none"
        ).sendAsync(url, prefs.sendScanType) { code, body ->
            textView.text = when {
                code != null -> "$code"
                body != null -> body
                else -> getString(R.string.background_request_failed)
            }
        }
    }

    companion object {
        fun newInstance(key: String): FUrlDialog {
            val args = Bundle()
            args.putString(ARG_KEY, key)
            val fragment = FUrlDialog()
            fragment.arguments = args
            return fragment
        }
    }
}

private fun completeUrl(template: String): String {
    var s = template.trim()
    if (s.isEmpty()) {
        return ""
    }
    if (!s.startsWith("http")) {
        s = "http://${s}"
    }
    if (prefs.sendScanType == "0" &&
        !s.matches(".*/[a-zA-Z._-]*\\?[a-zA-Z0-9&=_-]+=$".toRegex())
    ) {
        s = "${s}?content="
    }
    return s
}
