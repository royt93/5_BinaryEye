package com.mckimquyen.binaryeye.frm

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.EditText
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.ext.app.hasWritePermission
import com.mckimquyen.binaryeye.view.content.copyToClipboard
import com.mckimquyen.binaryeye.view.content.shareFile
import com.mckimquyen.binaryeye.view.content.shareText
import com.mckimquyen.binaryeye.view.graphics.COLOR_BLACK
import com.mckimquyen.binaryeye.view.graphics.COLOR_WHITE
import com.mckimquyen.binaryeye.view.io.addSuffixIfNotGiven
import com.mckimquyen.binaryeye.view.io.toSaveResult
import com.mckimquyen.binaryeye.view.io.writeExternalFile
import com.mckimquyen.binaryeye.view.doOnApplyWindowInsets
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.widget.ConfinedScalingImageView
import com.mckimquyen.binaryeye.view.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.math.min

class FBarcode : Fragment() {
    private enum class FileType {
        PNG, SVG, TXT
    }

    private val parentJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + parentJob)

    private lateinit var barcode: Barcode

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?,
    ): View? {
        val ac = activity ?: return null
        ac.setTitle(R.string.view_barcode)

        val view = inflater.inflate(
            R.layout.roy_f_barcode, container, false
        )

        val bitmap: Bitmap
        try {
            barcode = arguments?.toBarcode() ?: throw IllegalArgumentException(
                "Illegal arguments"
            )
            // Catch exceptions from encoding.
            bitmap = barcode.bitmap()
        } catch (e: Exception) {
            var message = e.message
            if (message.isNullOrEmpty()) {
                message = getString(R.string.error_encoding_barcode)
            }
            message.let {
                ac.toast(message)
            }
            fragmentManager?.popBackStack()
            return null
        }

        val imageView = view.findViewById<ConfinedScalingImageView>(
            R.id.barcode
        )
        imageView.setImageBitmap(bitmap)
        imageView.post {
            // Make sure to invoke this after ScalingImageView.onLayout().
            imageView.minWidth = min(
                imageView.minWidth / 2f, barcode.size.toFloat()
            )
        }

        view.findViewById<View>(R.id.share).setOnClickListener {
            it.context?.apply {
                pickFileType(R.string.share_as) { fileType ->
                    shareAs(fileType)
                }
            }
        }

        (view.findViewById<View>(R.id.insetLayout)).setPaddingFromWindowInsets()
        imageView.doOnApplyWindowInsets { v, insets ->
            (v as ConfinedScalingImageView).insets.set(insets)
        }

        return view
    }

    private fun Bundle.toBarcode() = Barcode(
        getString(CONTENT) ?: throw IllegalArgumentException(
            "content cannot be null"
        ), BarcodeFormat.valueOf(
            getString(FORMAT) ?: throw IllegalArgumentException(
                "format cannot be null"
            )
        ), getInt(SIZE), getInt(EC_LEVEL), Colors.entries[getInt(COLORS)]
    )

    override fun onDestroyView() {
        super.onDestroyView()
        parentJob.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_f_barcode, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.copyToClipboard -> {
                context?.apply {
                    copyToClipboard(barcode.text())
                    toast(R.string.copied_to_clipboard)
                }
                true
            }

            R.id.exportToFile -> {
                context?.pickFileType(R.string.exportAs) {
                    askForFileNameAndSave(it)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun Context.pickFileType(
        title: Int,
        action: (FileType) -> Unit,
    ) {
        val fileTypes = FileType.entries.toTypedArray()
        AlertDialog.Builder(this).setTitle(title)
            .setItems(fileTypes.map { it.name }.toTypedArray()) { _, which ->
                action(fileTypes[which])
            }.show()
    }

    // Dialogs do not have a parent view.
    @SuppressLint("InflateParams")
    private fun askForFileNameAndSave(fileType: FileType) {
        val ac = activity ?: return
        // Write permission is only required before Android Q.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !ac.hasWritePermission {
                askForFileNameAndSave(
                    fileType
                )
            }) {
            return
        }
        val view = ac.layoutInflater.inflate(R.layout.roy_dlg_save_file, null)
        val editText = view.findViewById<EditText>(R.id.fileName)
        editText.setText(
            encodeFileName("${barcode.format}_${barcode.content}")
        )
        AlertDialog.Builder(ac).setView(view).setPositiveButton(android.R.string.ok) { _, _ ->
            val fileName = editText.text.toString()
            when (fileType) {
                FileType.PNG -> saveAs(
                    addSuffixIfNotGiven(fileName, ".png"), MIME_PNG
                ) {
                    barcode.bitmap().saveAsPng(it)
                }

                FileType.SVG -> saveAs(
                    addSuffixIfNotGiven(fileName, ".svg"), MIME_SVG
                ) { outputStream ->
                    outputStream.write(barcode.svg().toByteArray())
                }

                FileType.TXT -> saveAs(
                    addSuffixIfNotGiven(fileName, ".txt"), MIME_TXT
                ) { outputStream ->
                    outputStream.write(barcode.text().toByteArray())
                }
            }
        }.setNegativeButton(android.R.string.cancel) { _, _ ->
        }.show()
    }

    private fun saveAs(
        fileName: String,
        mimeType: String,
        write: (outputStream: OutputStream) -> Unit,
    ) {
        val ac = activity ?: return
        scope.launch(Dispatchers.IO) {
            val message = ac.writeExternalFile(fileName, mimeType, write).toSaveResult()
            withContext(Dispatchers.Main) {
                ac.toast(message)
            }
        }
    }

    private fun Context.shareAs(fileType: FileType) {
        when (fileType) {
            FileType.PNG -> share(barcode.bitmap())
            FileType.SVG -> shareText(barcode.svg(), MIME_SVG)
            FileType.TXT -> shareText(barcode.text())
        }
    }

    private fun Context.share(bitmap: Bitmap) {
        scope.launch(Dispatchers.IO) {
            val file = File(
                externalCacheDir, "shared_barcode.png"
            )
            val success = try {
                FileOutputStream(file).use {
                    bitmap.saveAsPng(it)
                }
                true
            } catch (e: IOException) {
                false
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    shareFile(file, "image/png")
                } else {
                    toast(R.string.error_saving_file)
                }
            }
        }
    }

    companion object {
        private const val CONTENT = "content"
        private const val FORMAT = "format"
        private const val SIZE = "size"
        private const val EC_LEVEL = "ec_level"
        private const val COLORS = "colors"
        private const val MIME_PNG = "image/png"
        private const val MIME_SVG = "image/svg+xmg"
        private const val MIME_TXT = "text/plain"

        fun newInstance(
            content: String,
            format: BarcodeFormat,
            size: Int,
            ecLevel: Int = -1,
            colors: Int = 0,
        ): Fragment {
            val args = Bundle()
            args.putString(CONTENT, content)
            args.putString(FORMAT, format.name)
            args.putInt(SIZE, size)
            args.putInt(EC_LEVEL, ecLevel)
            args.putInt(COLORS, colors)
            val fragment = FBarcode()
            fragment.arguments = args
            return fragment
        }
    }
}

private data class Barcode(
    val content: String,
    val format: BarcodeFormat,
    val size: Int,
    val ecLevel: Int,
    val colors: Colors,
) {
    private var _bitmap: Bitmap? = null
    fun bitmap(): Bitmap {
        val b = _bitmap ?: ZxingCpp.encodeAsBitmap(
            content = content,
            format = format,
            width = size,
            height = size,
            margin = -1,
            ecLevel = ecLevel,
            setColor = colors.foregroundColor(),
            unsetColor = colors.backgroundColor()
        )
        _bitmap = b
        return b
    }

    private var _svg: String? = null
    fun svg(): String {
        val s = _svg ?: ZxingCpp.encodeAsSvg(
            content, format, -1, ecLevel
        )
        _svg = s
        return s
    }

    private var _text: String? = null
    fun text(): String {
        val t = _text ?: ZxingCpp.encodeAsText(
            content = content,
            format = format,
            margin = -1,
            ecLevel = ecLevel,
            inverted = colors == Colors.BLACK_ON_WHITE
        )
        _text = t
        return t
    }
}

private enum class Colors {
    BLACK_ON_WHITE, WHITE_ON_BLACK, BLACK_ON_TRANSPARENT, WHITE_ON_TRANSPARENT;

    fun foregroundColor(): Int = when (this) {
        BLACK_ON_WHITE,
        BLACK_ON_TRANSPARENT,
        -> COLOR_BLACK

        WHITE_ON_BLACK,
        WHITE_ON_TRANSPARENT,
        -> COLOR_WHITE
    }

    fun backgroundColor(): Int = when (this) {
        BLACK_ON_WHITE -> COLOR_WHITE
        WHITE_ON_BLACK -> COLOR_BLACK
        BLACK_ON_TRANSPARENT,
        WHITE_ON_TRANSPARENT,
        -> 0
    }
}

private fun Bitmap.saveAsPng(
    outputStream: OutputStream,
    quality: Int = 90,
) =
    compress(Bitmap.CompressFormat.PNG, quality, outputStream)

private val fileNameCharacters = "[^A-Za-z0-9]".toRegex()
private fun encodeFileName(name: String): String =
    fileNameCharacters.replace(name, "_").take(16).trim('_').lowercase(Locale.getDefault())
