package com.mckimquyen.binaryeye.frm

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.adapter.prettifyFormatName
import com.mckimquyen.binaryeye.database.Recreation
import com.mckimquyen.binaryeye.database.Scan
import com.mckimquyen.binaryeye.database.toRecreation
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.ext.app.addFragment
import com.mckimquyen.binaryeye.ext.app.hasLocationPermission
import com.mckimquyen.binaryeye.ext.app.hasWritePermission
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.act.ActivityMain
import com.mckimquyen.binaryeye.view.actions.ActionRegistry
import com.mckimquyen.binaryeye.view.actions.IAction
import com.mckimquyen.binaryeye.view.actions.wifi.WifiAction
import com.mckimquyen.binaryeye.view.actions.wifi.WifiConnector
import com.mckimquyen.binaryeye.view.content.copyToClipboard
import com.mckimquyen.binaryeye.view.content.shareText
import com.mckimquyen.binaryeye.view.io.askForFileName
import com.mckimquyen.binaryeye.view.io.toSaveResult
import com.mckimquyen.binaryeye.view.io.writeExternalFile
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.widget.toast
import kotlin.math.roundToInt
import kotlinx.coroutines.*

class FDecode : Fragment() {
    private lateinit var contentView: EditText
    private lateinit var formatView: TextView
    private lateinit var dataView: TableLayout
    private lateinit var metaView: TableLayout
    private lateinit var hexView: TextView
    private lateinit var stampView: TextView
    private lateinit var recreationView: ImageView
    private lateinit var fab: FloatingActionButton
    private lateinit var format: String

    private val parentJob = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
    private val dp: Float
        get() = resources.displayMetrics.density
    private val content: String
        get() = contentView.text.toString()

    private var closeAutomatically = false
    private var action = ActionRegistry.DEFAULT_ACTION
    private var isBinary = false
    private var originalBytes: ByteArray = ByteArray(0)
    private var id = 0L
    private var recreation: Recreation? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?,
    ): View {
        activity?.setTitle(R.string.content)

        val view = inflater.inflate(
            /* resource = */ R.layout.roy_f_decode,
            /* root = */ container,
            /* attachToRoot = */ false
        )

        closeAutomatically = prefs.closeAutomatically &&
                activity?.intent?.hasExtra(ActivityMain.DECODED) == true

        val scan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(SCAN, Scan::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(SCAN) as Scan?
        } ?: throw IllegalArgumentException("DecodeFragment needs a Scan")

        val originalContent = scan.content
        isBinary = scan.raw != null
        originalBytes = scan.raw ?: originalContent.toByteArray()
        format = scan.format
        id = scan.id

        contentView = view.findViewById(R.id.content)
        formatView = view.findViewById(R.id.format)
        dataView = view.findViewById(R.id.data)
        metaView = view.findViewById(R.id.meta)
        hexView = view.findViewById(R.id.hex)
        stampView = view.findViewById(R.id.stamp)
        recreationView = view.findViewById(R.id.recreation)
        fab = view.findViewById(R.id.open)

        initContentAndFab(originalContent)

        if (prefs.showMetaData) {
            metaView.fillMetaView(scan)
        } else {
            metaView.visibility = View.GONE
        }
        if (prefs.showRecreation) {
            recreation = scan.toRecreation(
                (200f * dp).roundToInt()
            )
        }

        updateViewsAndFab(originalContent, originalBytes)

        (view.findViewById<View>(R.id.insetLayout)).setPaddingFromWindowInsets()
        (view.findViewById<View>(R.id.scrollView)).setPaddingFromWindowInsets()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        parentJob.cancel()
    }

    private fun initContentAndFab(originalContent: String) {
        if (isBinary) {
            contentView.setText(String(originalBytes).foldNonAlNum())
            contentView.isEnabled = false
            fab.setImageResource(R.drawable.ic_action_save)
            fab.setOnClickListener {
                askForFileNameAndSave(originalBytes)
            }
        } else {
            contentView.setText(originalContent)
            contentView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    updateViewsAndFab(s?.toString() ?: "")
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit
            })
            fab.setOnClickListener {
                executeAction(content)
            }
            if (prefs.openImmediately) {
                executeAction(content)
            }
        }
    }

    private fun updateViewsAndFab(text: String, bytes: ByteArray? = null) {
        val b = bytes ?: text.toByteArray()
        resolveActionAndUpdateFab(b)
        updateViews(text, b)
    }

    private fun resolveActionAndUpdateFab(bytes: ByteArray) {
        val prevAction = action
        if (!prevAction.canExecuteOn(bytes)) {
            action = ActionRegistry.getAction(bytes)
        }
        if (prevAction !== action) {
            updateFab(action)
        }
    }

    private fun updateFab(action: IAction) {
        fab.setImageResource(action.iconResId)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            fab.setOnLongClickListener { v ->
                v.context.toast(action.titleResId)
                true
            }
        } else {
            fab.tooltipText = getString(action.titleResId)
        }
    }

    private fun updateViews(text: String, bytes: ByteArray) {
        formatView.text = resources.getQuantityString(
            R.plurals.barcode_info,
            bytes.size,
            prettifyFormatName(format),
            bytes.size
        )
        hexView.showIf(prefs.showHexDump) { v ->
            v.text = hexDump(bytes)
        }
        recreationView.showIf(prefs.showRecreation) { v ->
            val r = recreation ?: return@showIf
            v.setImageBitmap(r.encode(text))
            v.setOnClickListener {
                fragmentManager?.addFragment(
                    FBarcode.newInstance(
                        content = text,
                        format = r.format,
                        size = r.size,
                        ecLevel = r.ecLevel
                    )
                )
            }
        }
        dataView.fillDataView(text)
        stampView.setTrackingLink(bytes, format)
    }

    private fun TableLayout.fillDataView(text: String) {
        val items = LinkedHashMap<Int, String?>()
        if (action is WifiAction) {
            WifiConnector.parseMap(text)?.let { wifiData ->
                items.putAll(
                    linkedMapOf(
                        R.string.entry_type to getString(R.string.wifiNetwork),
                        R.string.wifi_ssid to wifiData["S"],
                        R.string.wifi_password to wifiData["P"],
                        R.string.wifi_type to wifiData["T"],
                        R.string.wifi_hidden to wifiData["H"],
                        R.string.wifi_eap to wifiData["E"],
                        R.string.wifiIdentity to wifiData["I"],
                        R.string.wifi_anonymous_identity to wifiData["A"],
                        R.string.wifi_phase2 to wifiData["PH2"]
                    )
                )
            }
        }
        fill(items)
    }

    private fun TableLayout.fillMetaView(scan: Scan) {
        val items = linkedMapOf(
            R.string.errorCorrectionLevel to scan.errorCorrectionLevel,
            R.string.sequence_size to scan.sequenceSize.positiveToString(),
            R.string.sequence_index to scan.sequenceIndex.positiveToString(),
            R.string.sequence_id to scan.sequenceId,
            R.string.gtin_country to scan.country,
            R.string.gtin_add_on to scan.addOn,
            R.string.gtin_price to scan.price,
            R.string.gtin_issue_number to scan.issueNumber,
        )
        if (!scan.version.isNullOrBlank()) {
            val versionString = if (scan.format == "QR_CODE") {
                getString(
                    R.string.qr_version_and_modules,
                    scan.version,
                    (scan.version.toIntOrNull() ?: 0) * 4 + 17
                )
            } else {
                scan.version
            }
            items.putAll(
                linkedMapOf(
                    R.string.barcode_version_number to versionString
                )
            )
        }
        fill(items)
    }

    private fun TableLayout.fill(
        items: LinkedHashMap<Int, String?>,
    ) {
        removeAllViews()
        visibility = if (items.isEmpty()) View.GONE else {
            val ctx = context
            val spaceBetween = (16f * dp).roundToInt()
            items.forEach { item ->
                val text = item.value
                if (!text.isNullOrBlank()) {
                    val tr = TableRow(ctx)
                    val keyView = TextView(ctx)
                    keyView.setText(item.key)
                    val valueView = TextView(ctx)
                    valueView.setPadding(
                        /* left = */ spaceBetween,
                        /* top = */ 0,
                        /* right = */ 0,
                        /* bottom = */ 0
                    )
                    valueView.text = text
                    tr.addView(keyView)
                    tr.addView(valueView)
                    addView(tr)
                }
            }
            View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_f_decode, menu)
        if (isBinary) {
            menu.findItem(R.id.copyToClipboard).isVisible = false
            menu.findItem(R.id.create).isVisible = false
        }
        if (id > 0L) {
            menu.findItem(R.id.remove).isVisible = true
        }
        if (action is WifiAction) {
            menu.findItem(R.id.copyPassword).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.copyPassword -> {
                copyPasswordToClipboard()
                maybeBackOrFinish()
                true
            }

            R.id.copyToClipboard -> {
                copyToClipboard(textOrHex())
                maybeBackOrFinish()
                true
            }

            R.id.share -> {
                context?.apply {
                    shareText(textOrHex())
                    maybeBackOrFinish()
                }
                true
            }

            R.id.create -> {
                fragmentManager?.addFragment(
                    FEncode.newInstance(content, format)
                )
                true
            }

            R.id.remove -> {
                db.removeScan(id)
                backOrFinish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun textOrHex() = if (isBinary) {
        originalBytes.toHexString()
    } else {
        content
    }

    private fun copyPasswordToClipboard() {
        val ac = action
        if (ac is WifiAction) {
            ac.password?.let { password ->
                activity?.apply {
                    copyToClipboard(password, isSensitive = true)
                    toast(R.string.copied_password_to_clipboard)
                }
            }
        }
    }

    private fun copyToClipboard(
        text: String,
        isSensitive: Boolean = false,
    ) {
        activity?.apply {
            this.copyToClipboard(text, isSensitive)
            toast(R.string.copied_to_clipboard)
        }
    }

    private fun maybeBackOrFinish() {
        if (closeAutomatically) {
            backOrFinish()
        }
    }

    private fun backOrFinish() {
        val fm = fragmentManager
        if (fm != null && fm.backStackEntryCount > 0) {
            fm.popBackStack()
        } else {
            activity?.finish()
        }
    }

    private fun executeAction(content: String) {
        val ac = activity ?: return
        if (content.isNotEmpty()) {
            if (action is WifiAction &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                !ac.hasLocationPermission { executeAction(content) }
            ) {
                return
            }
            scope.launch {
                action.execute(ac, content.toByteArray())
                maybeBackOrFinish()
            }
        }
    }

    private fun askForFileNameAndSave(raw: ByteArray) {
        val ac = activity ?: return
        // Write permission is only required before Android Q.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            !ac.hasWritePermission { askForFileNameAndSave(raw) }
        ) {
            return
        }
        scope.launch(Dispatchers.Main) {
            val name = ac.askForFileName() ?: return@launch
            withContext(Dispatchers.IO) {
                val message = ac.writeExternalFile(
                    name,
                    "application/octet-stream"
                ) {
                    it.write(raw)
                }.toSaveResult()
                withContext(Dispatchers.Main) {
                    ac.toast(message)
                }
            }
        }
    }

    companion object {
        private const val SCAN = "scan"

        fun newInstance(scan: Scan): Fragment {
            val args = Bundle()
            args.putParcelable(SCAN, scan)
            val fragment = FDecode()
            fragment.arguments = args
            return fragment
        }
    }
}

private inline fun <T : View> T.showIf(
    visible: Boolean,
    block: (T) -> Unit,
) {
    visibility = if (visible) {
        block.invoke(this)
        View.VISIBLE
    } else {
        View.GONE
    }
}

private val nonAlNum = "[^a-zA-Z0-9]".toRegex()
private val multipleDots = "[…]+".toRegex()
private fun String.foldNonAlNum() = replace(nonAlNum, "…")
    .replace(multipleDots, "…")

private fun hexDump(
    bytes: ByteArray,
    charsPerLine: Int = 33,
): String {
    if (charsPerLine < 4 || bytes.isEmpty()) {
        return ""
    }
    val dump = StringBuilder()
    val hex = StringBuilder()
    val ascii = StringBuilder()
    val itemsPerLine = (charsPerLine - 1) / 4
    val len = bytes.size
    var i = 0
    while (true) {
        val ord = bytes[i]
        hex.append(String.format("%02X ", ord))
        ascii.append(if (ord > 31) ord.toInt().toChar() else " ")
        ++i
        val posInLine = i % itemsPerLine
        val atEnd = i >= len
        var atLineEnd = posInLine == 0
        if (atEnd && !atLineEnd) {
            for (j in posInLine until itemsPerLine) {
                hex.append("   ")
            }
            atLineEnd = true
        }
        if (atLineEnd) {
            dump.append(hex.toString())
            dump.append(" ")
            dump.append(ascii.toString())
            dump.append("\n")
            hex.setLength(0)
            ascii.setLength(0)
        }
        if (atEnd) {
            break
        }
    }
    return dump.toString()
}

private fun Int.positiveToString() = if (this > -1) this.toString() else ""

private fun TextView.setTrackingLink(
    bytes: ByteArray,
    format: String,
) {
    val trackingLink = generateDpTrackingLink(bytes, format)
    if (trackingLink != null) {
        text = trackingLink.fromHtml()
        isClickable = true
        movementMethod = LinkMovementMethod.getInstance()
    } else {
        visibility = View.GONE
    }
}

private fun generateDpTrackingLink(
    bytes: ByteArray,
    format: String,
): String? {
    // Check for Deutsche Post Matrixcode stamp.
    var isStamp = false
    var rawData = bytes
    if (format == "DATA_MATRIX" &&
        bytes.toString(Charsets.ISO_8859_1).startsWith("DEA5")
    ) {
        if (bytes.size == 47) {
            isStamp = true
        } else if (bytes.size > 47) {
            // Transform back to original data.
            rawData = bytes.toString(Charsets.UTF_8).toByteArray(
                Charsets.ISO_8859_1
            )
            if (rawData.size == 47) {
                isStamp = true
            }
        }
    }

    if (!isStamp) {
        return null
    }

    val hex = StringBuilder()
    hex.append(String.format("%02X", rawData[9]))
    hex.append(String.format("%02X", rawData[10]))
    hex.append(String.format("%02X", rawData[11]))
    hex.append(String.format("%02X", rawData[12]))
    hex.append(String.format("%02X", rawData[13]))
    hex.append(String.format("%X", (rawData[4].toInt() and 0x0f).toByte()))
    hex.append(String.format("%02X", rawData[5]))
    hex.append(String.format("%02X", rawData[6]))
    hex.append(String.format("%02X", rawData[7]))
    hex.append(String.format("%02X", rawData[8]))
    val hexString = hex.toString()
    val trackingNumber = hexString + String.format(
        "%X",
        crc4(hexString.toByteArray(Charsets.ISO_8859_1))
    )
    return "<a href=\"https://www.deutschepost.de/de/s/sendungsverfolgung/verfolgen.html?piececode=$trackingNumber\">Deutsche Post: $trackingNumber</a>"
}

// CRC-4 with polynomial x^4 + x + 1.
private fun crc4(input: ByteArray): Int {
    var crc = 0
    var i = 0
    while (i < input.size) {
        val c = input[i].toInt()
        var j = 0x80
        while (j != 0) {
            var bit = crc and 0x8
            crc = crc shl 1
            if (c and j != 0) {
                bit = bit xor 0x8
            }
            if (bit != 0) {
                crc = crc xor 0x3
            }
            j = j ushr 1
        }
        ++i
    }
    crc = crc and 0xF
    return crc
}

private fun String.fromHtml() = if (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
) {
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
} else {
    @Suppress("DEPRECATION")
    Html.fromHtml(this)
}
