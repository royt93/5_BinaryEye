package com.mckimquyen.binaryeye.frm

import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mckimquyen.binaryeye.view.graphics.COLOR_BLACK
import com.mckimquyen.binaryeye.view.graphics.COLOR_WHITE
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


    // F2: QR Styling state
    private var fgColor: Int = COLOR_BLACK
    private var bgColor: Int = COLOR_WHITE
    private var logoUri: Uri? = null

    private lateinit var qrStylingSection: LinearLayout
    private lateinit var swatchFg: View
    private lateinit var swatchBg: View
    private lateinit var ivLogoThumb: ImageView
    private lateinit var tvLogoName: android.widget.TextView
    private lateinit var btnPickLogo: ImageButton
    private lateinit var btnClearLogo: ImageButton
    private lateinit var logoPlaceholder: View

    private val logoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            logoUri = uri
            updateLogoUI()
            // Force EC level to H (index 3 for QR_CODE) to keep scannable
            val format = formats[formatView.selectedItemPosition]
            if (format == BarcodeFormat.QR_CODE && ecSpinner.adapter.count > 3) {
                ecSpinner.setSelection(3)
                activity?.toast(R.string.qr_logo_ec_forced)
            }
        }
    }

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
                (format == BarcodeFormat.QR_CODE).setVisibility(qrStylingSection)
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

        view.findViewById<View>(R.id.batchQr).setOnClickListener {
            fragmentManager?.addFragment(FBatchEncode())
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


        // F2: QR Styling views
        qrStylingSection = view.findViewById(R.id.qrStylingSection)
        swatchFg = view.findViewById(R.id.swatchFg)
        swatchBg = view.findViewById(R.id.swatchBg)
        ivLogoThumb = view.findViewById(R.id.ivLogoThumb)
        tvLogoName = view.findViewById(R.id.tvLogoName)
        btnPickLogo = view.findViewById(R.id.btnPickLogo)
        btnClearLogo = view.findViewById(R.id.btnClearLogo)
        logoPlaceholder = view.findViewById(R.id.logoPlaceholder)

        updateSwatches()
        swatchFg.setOnClickListener { showColorPicker(fgColor) { c -> fgColor = c; updateSwatches() } }
        swatchBg.setOnClickListener { showColorPicker(bgColor) { c -> bgColor = c; updateSwatches() } }
        btnPickLogo.setOnClickListener { logoPickerLauncher.launch("image/*") }
        btnClearLogo.setOnClickListener { logoUri = null; updateLogoUI() }

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
                colors = if (format.canBeInverted()) colorsSpinner.selectedItemPosition else 0,
                fgColor = if (format == BarcodeFormat.QR_CODE) fgColor else COLOR_BLACK,
                bgColor = if (format == BarcodeFormat.QR_CODE) bgColor else COLOR_WHITE,
                logoUri = if (format == BarcodeFormat.QR_CODE) logoUri?.toString() else null
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


    private fun updateSwatches() {
        swatchFg.background = colorCircle(fgColor, needsBorder = Color.alpha(fgColor) > 200 && Color.luminance(fgColor) > 0.9f)
        swatchBg.background = colorCircle(bgColor, needsBorder = Color.alpha(bgColor) > 200 && Color.luminance(bgColor) > 0.9f)
    }

    private fun colorCircle(color: Int, needsBorder: Boolean = false) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (needsBorder) setStroke(2, 0xFFCCCCCC.toInt())
    }

    private fun updateLogoUI() {
        val hasLogo = logoUri != null
        ivLogoThumb.visibility = if (hasLogo) View.VISIBLE else View.GONE
        logoPlaceholder.visibility = if (hasLogo) View.GONE else View.VISIBLE
        btnClearLogo.visibility = if (hasLogo) View.VISIBLE else View.GONE
        if (hasLogo) {
            ivLogoThumb.setImageURI(logoUri)
            tvLogoName.text = logoUri?.lastPathSegment ?: getString(R.string.qr_pick_logo)
        } else {
            tvLogoName.text = getString(R.string.qr_no_logo)
        }
    }

    private fun showColorPicker(current: Int, onPick: (Int) -> Unit) {
        val presets = intArrayOf(
            0xFF000000.toInt(), 0xFF424242.toInt(), 0xFF9E9E9E.toInt(), 0xFFFFFFFF.toInt(),
            0xFFF44336.toInt(), 0xFFFF5722.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(),
            0xFF4CAF50.toInt(), 0xFF009688.toInt(), 0xFF2196F3.toInt(), 0xFF9C27B0.toInt()
        )
        val dp = resources.displayMetrics.density
        val size = (44 * dp).toInt()
        val margin = (6 * dp).toInt()
        val grid = GridLayout(requireContext()).apply { columnCount = 4 }
        var dialog: AlertDialog? = null
        presets.forEach { color ->
            val v = View(requireContext())
            val lp = GridLayout.LayoutParams().apply {
                width = size; height = size
                setMargins(margin, margin, margin, margin)
            }
            v.layoutParams = lp
            v.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                val stroke = if (color == current) 0xFF1565C0.toInt() else 0xFFBDBDBD.toInt()
                val strokeW = if (color == current) (3 * dp).toInt() else (1 * dp).toInt()
                setStroke(strokeW, stroke)
            }
            v.setOnClickListener { onPick(color); dialog?.dismiss() }
            grid.addView(v)
        }
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pick_color)
            .setView(grid)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
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
