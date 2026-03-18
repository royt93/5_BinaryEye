package com.mckimquyen.binaryeye.frm

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.adapter.prettifyFormatName
import com.mckimquyen.binaryeye.ext.app.addFragment
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.text.unescape
import com.mckimquyen.binaryeye.view.hideSoftKeyboard
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat

class FEncode : Fragment() {
    private lateinit var formatView: Spinner
    private lateinit var ecLabel: TextView
    private lateinit var ecSpinner: Spinner
    private lateinit var colorsLabel: TextView
    private lateinit var colorsSpinner: Spinner
    private lateinit var sizeView: TextView
    private lateinit var sizeBarView: SeekBar
    private lateinit var contentView: EditText
    private lateinit var unescapeCheckBox: CheckBox

    private val formats = arrayListOf(
        BarcodeFormat.AZTEC,
        BarcodeFormat.CODABAR,
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_128,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.EAN_8,
        BarcodeFormat.EAN_13,
        BarcodeFormat.ITF,
        BarcodeFormat.PDF_417,
        BarcodeFormat.QR_CODE,
        BarcodeFormat.UPC_A
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?,
    ): View? {
        val ac = activity ?: return null
        ac.setTitle(R.string.compose_barcode)

        val view = inflater.inflate(
            /* resource = */ R.layout.roy_f_encode,
            /* root = */ container,
            /* attachToRoot = */ false
        )

        formatView = view.findViewById(R.id.format)
        val formatAdapter = ArrayAdapter(
            /* context = */ ac,
            /* resource = */ android.R.layout.simple_spinner_item,
            /* objects = */ formats.map { prettifyFormatName(it.name) }
        )
        formatAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        formatView.adapter = formatAdapter
        formatView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long,
            ) {
                val format = formats[position]
                val arrayId = when (format) {
                    BarcodeFormat.AZTEC -> R.array.aztecErrorCorrectionLevels
                    BarcodeFormat.QR_CODE -> R.array.qrErrorCorrectionLevels
                    BarcodeFormat.PDF_417 -> R.array.pdf417ErrorCorrectionLevels
                    else -> 0
                }
                if (arrayId > 0) {
                    ecSpinner.setEntries(arrayId)
                    val idx = format.unpackEcLevel(
                        prefs.indexOfLastSelectedEcLevel
                    )
                    if (idx < ecSpinner.adapter.count) {
                        ecSpinner.setSelection(idx)
                    }
                    true
                } else {
                    false
                }.setVisibility(ecLabel, ecSpinner)
                format.canBeInverted().setVisibility(
                    colorsLabel, colorsSpinner
                )
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        ecLabel = view.findViewById(R.id.errorCorrectionLabel)
        ecSpinner = view.findViewById(R.id.errorCorrectionLevel)
        ecSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long,
            ) {
                val format = formats[formatView.selectedItemPosition]
                prefs.indexOfLastSelectedEcLevel = format.packEcLevel(
                    prefs.indexOfLastSelectedEcLevel,
                    position
                )
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        colorsLabel = view.findViewById(R.id.colorsLabel)
        colorsSpinner = view.findViewById(R.id.colors)

        sizeView = view.findViewById(R.id.sizeDisplay)
        sizeBarView = view.findViewById(R.id.sizeBar)
        initSizeBar()

        contentView = view.findViewById(R.id.content)
        unescapeCheckBox = view.findViewById(R.id.unescape)
        unescapeCheckBox.isChecked = prefs.expandEscapeSequences

        val args = arguments
        args?.getString(CONTENT)?.let {
            contentView.setText(it)
        }

        val barcodeFormat = args?.getString(FORMAT)
        if (barcodeFormat != null) {
            formatView.setSelection(
                formats.indexOf(barcodeFormat.toFormat())
            )
        } else if (state == null) {
            formatView.post {
                formatView.setSelection(prefs.indexOfLastSelectedFormat)
            }
        }

        view.findViewById<View>(R.id.encode).setOnClickListener {
            it.context.encode()
        }

        // [Feature 3] Paste from clipboard
        view.findViewById<android.widget.ImageButton>(R.id.btnPaste).setOnClickListener {
            val clipboard = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(it.context)?.toString() ?: ""
            if (text.isNotEmpty()) {
                contentView.setText(text)
                contentView.setSelection(text.length)
            } else {
                it.context.toast(R.string.clipboard_empty)
            }
        }

        (view.findViewById<View>(R.id.insetLayout)).setPaddingFromWindowInsets()
        (view.findViewById<View>(R.id.scrollView)).setPaddingFromWindowInsets()

        return view
    }

    override fun onPause() {
        super.onPause()
        prefs.indexOfLastSelectedFormat = formatView.selectedItemPosition
        prefs.expandEscapeSequences = unescapeCheckBox.isChecked
    }

    private fun Context.encode() {
        var content = contentView.text.toString()
        if (unescapeCheckBox.isChecked) {
            try {
                content = content.unescape()
            } catch (e: IllegalArgumentException) {
                toast(e.message ?: "Invalid escape sequence")
                return
            }
        }
        if (content.isEmpty()) {
            toast(R.string.error_no_content)
            return
        }
        hideSoftKeyboard(contentView)
        val format = formats[formatView.selectedItemPosition]
        fragmentManager?.addFragment(
            FBarcode.newInstance(
                content = content,
                format = format,
                size = getSize(sizeBarView.progress),
                ecLevel = format.getErrorCorrectionLevel(ecSpinner.selectedItemPosition),
                colors = if (format.canBeInverted()) {
                    colorsSpinner.selectedItemPosition
                } else 0
            )
        )
    }

    private fun initSizeBar() {
        updateSize(sizeBarView.progress)
        sizeBarView.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progressValue: Int,
                    fromUser: Boolean,
                ) {
                    updateSize(progressValue)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
    }

    private fun updateSize(power: Int) {
        val size = getSize(power)
        sizeView.text = getString(R.string.width_by_height, size, size)
    }

    companion object {
        private const val CONTENT = "content"
        private const val FORMAT = "format"

        fun newInstance(
            content: String? = null,
            format: String? = null,
        ): Fragment {
            val args = Bundle()
            content?.let {
                args.putString(CONTENT, content)
            }
            format?.let {
                args.putString(FORMAT, it)
            }
            val fragment = FEncode()
            fragment.arguments = args
            return fragment
        }
    }
}

private fun Boolean.setVisibility(vararg views: View) {
    val visibility = if (this) View.VISIBLE else View.GONE
    for (view in views) {
        view.visibility = visibility
    }
}

private fun BarcodeFormat.packEcLevel(packed: Int, level: Int): Int {
    val s = ecLevelShift()
    return (level shl s) or (packed and (15 shl s).inv())
}

private fun BarcodeFormat.unpackEcLevel(packed: Int) =
    (packed shr ecLevelShift()) and 15

private fun BarcodeFormat.ecLevelShift() = when (this) {
    BarcodeFormat.AZTEC -> 0
    BarcodeFormat.QR_CODE -> 4
    BarcodeFormat.PDF_417 -> 8
    else -> throw IllegalArgumentException("$this does not have error levels")
}

private fun BarcodeFormat.canBeInverted() = when (this) {
    BarcodeFormat.AZTEC,
    BarcodeFormat.DATA_MATRIX,
    BarcodeFormat.QR_CODE,
    -> true

    else -> false
}

private fun String.toFormat(default: BarcodeFormat = BarcodeFormat.QR_CODE): BarcodeFormat = try {
    BarcodeFormat.valueOf(this)
} catch (_: IllegalArgumentException) {
    default
}

private fun BarcodeFormat.getErrorCorrectionLevel(position: Int) = when (this) {
    BarcodeFormat.AZTEC -> position
    BarcodeFormat.QR_CODE -> (position + 1) * 2
    BarcodeFormat.PDF_417 -> position
    else -> 0
}.coerceIn(0, 8)

private fun Spinner.setEntries(resId: Int) = ArrayAdapter.createFromResource(
    this.context,
    resId,
    android.R.layout.simple_spinner_item
).also { aa ->
    aa.setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item
    )
    adapter = aa
}

private fun getSize(power: Int) = 128 * (power + 1)
