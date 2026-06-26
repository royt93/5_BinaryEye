package com.mckimquyen.binaryeye.widget

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.act.ActivityMain
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Widget test: bottom sheet kết quả scan (E6) phải inflate được dưới theme app.
 * Đây chính là test bắt lỗi class "Material widget cần Theme.MaterialComponents"
 * (bug A7 từng làm crash) — nếu theme sai, inflate sẽ ném ngay.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class ScanResultBottomSheetRobolectricTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        prefs.init(ctx)
    }

    @Test
    fun bottomSheet_inflatesUnderAppTheme_withAllActionViews() {
        // Dùng ActivityMain để có theme thật của app (Material Bridge).
        val activity = Robolectric.buildActivity(ActivityMain::class.java).setup().get()
        val view = activity.layoutInflater.inflate(R.layout.roy_bottom_sheet_scan_result, null)

        assertNotNull(view.findViewById<Chip>(R.id.chipFormat))
        assertNotNull(view.findViewById<TextView>(R.id.tvScanContent))
        assertNotNull(view.findViewById<MaterialButton>(R.id.btnPrimaryAction))
        assertNotNull(view.findViewById<MaterialButton>(R.id.btnCopy))
        assertNotNull(view.findViewById<MaterialButton>(R.id.btnShare))
        assertNotNull(view.findViewById<MaterialButton>(R.id.btnSave))
        assertNotNull(view.findViewById<MaterialButton>(R.id.btnDetails))
    }

    @Test
    fun batchEncodeLayout_inflates() {
        val activity = Robolectric.buildActivity(ActivityMain::class.java).setup().get()
        val view = activity.layoutInflater.inflate(R.layout.roy_frm_batch_encode, null)
        assertNotNull(view.findViewById<View>(R.id.etBatchInput))
        assertNotNull(view.findViewById<View>(R.id.btnBatchGenerate))
        assertNotNull(view.findViewById<View>(R.id.btnBatchExportZip))
    }
}
