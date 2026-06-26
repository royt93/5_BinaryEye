package com.mckimquyen.binaryeye.frm

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.content.shareFile
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FBatchEncode : Fragment() {

    private lateinit var etInput: EditText
    private lateinit var tvCount: TextView
    private lateinit var btnGenerate: MaterialButton
    private lateinit var btnExportZip: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var thumbnailScrollView: View
    private lateinit var thumbnailRow: LinearLayout

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var generatedBitmaps: List<Pair<String, Bitmap>> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val ac = activity ?: return null
        ac.setTitle(R.string.batch_qr_title)

        val view = inflater.inflate(R.layout.roy_frm_batch_encode, container, false)

        etInput = view.findViewById(R.id.etBatchInput)
        tvCount = view.findViewById(R.id.tvBatchCount)
        btnGenerate = view.findViewById(R.id.btnBatchGenerate)
        btnExportZip = view.findViewById(R.id.btnBatchExportZip)
        progressBar = view.findViewById(R.id.batchProgress)
        thumbnailScrollView = view.findViewById(R.id.thumbnailScrollView)
        thumbnailRow = view.findViewById(R.id.thumbnailRow)

        etInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val count = s.toString().lines().count { it.isNotBlank() }
                tvCount.text = if (count == 0) getString(R.string.batch_qr_count_zero)
                               else getString(R.string.batch_qr_count, count)
            }
        })

        btnGenerate.setOnClickListener { generateQRs() }
        btnExportZip.setOnClickListener { exportZip() }

        view.findViewById<View>(R.id.insetLayout).setPaddingFromWindowInsets()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        recycleAll()
    }

    private fun generateQRs() {
        val allLines = etInput.text.toString().lines().filter { it.isNotBlank() }
        if (allLines.isEmpty()) {
            activity?.toast(R.string.batch_qr_empty)
            return
        }
        // Gioi han so QR giu trong RAM cung luc de tranh OOM voi input qua lon.
        val lines = allLines.take(MAX_BATCH)
        if (allLines.size > MAX_BATCH) {
            activity?.toast(getString(R.string.batch_qr_capped, MAX_BATCH))
        }
        setLoading(true)
        scope.launch {
            val results = lines.mapNotNull { line ->
                try {
                    val bmp = ZxingCpp.encodeAsBitmap(
                        line, BarcodeFormat.QR_CODE, 256, 256, -1, -1
                    )
                    line to bmp
                } catch (_: Exception) { null }
            }
            withContext(Dispatchers.Main) {
                recycleAll()
                generatedBitmaps = results
                showThumbnails(results)
                btnExportZip.isEnabled = results.isNotEmpty()
                setLoading(false)
            }
        }
    }

    private fun showThumbnails(items: List<Pair<String, Bitmap>>) {
        thumbnailRow.removeAllViews()
        if (items.isEmpty()) {
            thumbnailScrollView.visibility = View.GONE
            return
        }
        val dp = resources.displayMetrics.density
        val size = (88 * dp).toInt()
        val margin = (4 * dp).toInt()
        items.forEach { (_, bmp) ->
            val iv = ImageView(requireContext())
            iv.setImageBitmap(bmp)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(margin, margin, margin, margin)
            iv.layoutParams = lp
            thumbnailRow.addView(iv)
        }
        thumbnailScrollView.visibility = View.VISIBLE
    }

    private fun exportZip() {
        if (generatedBitmaps.isEmpty()) return
        val snapshot = generatedBitmaps.toList()
        setLoading(true)
        scope.launch {
            val ac = activity ?: return@launch
            val file = File(ac.externalCacheDir, "batch_qr.zip")
            ZipOutputStream(FileOutputStream(file)).use { zos ->
                snapshot.forEachIndexed { i, (name, bmp) ->
                    zos.putNextEntry(ZipEntry(zipEntryName(name, i)))
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                }
            }
            withContext(Dispatchers.Main) {
                setLoading(false)
                ac.shareFile(file, "application/zip")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnGenerate.isEnabled = !loading
    }

    private fun recycleAll() {
        generatedBitmaps.forEach { it.second.recycle() }
        generatedBitmaps = emptyList()
    }

    companion object {
        private const val MAX_BATCH = 200
    }
}
