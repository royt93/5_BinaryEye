package com.mckimquyen.binaryeye.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Spinner
import androidx.test.core.app.ApplicationProvider
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.act.ActivityMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Widget test (Robolectric) cho FEncode — chạy trên JVM, không cần device.
 * Dùng `application = Application::class` để bỏ qua RApp.onCreate (tránh khởi
 * tạo AdManager), và init thủ công db/prefs trong @Before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class FEncodeRobolectricTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        prefs.init(ctx)
        db.open(ctx)
    }

    private fun launchEncode() = Robolectric.buildActivity(
        ActivityMain::class.java,
        Intent(ctx, ActivityMain::class.java).putExtra("encode", "robo-test")
    ).setup()

    @Test
    fun qrStylingSection_isGoneByDefault_forAztec() {
        val activity = launchEncode().get()
        val section = activity.findViewById<View>(R.id.qrStylingSection)
        assertEquals(
            "QR styling phải ẩn khi format mặc định (AZTEC)",
            View.GONE, section.visibility
        )
    }

    @Test
    fun qrStylingSection_becomesVisible_whenQrSelected() {
        val activity = launchEncode().get()
        val spinner = activity.findViewById<Spinner>(R.id.format)

        var qrIndex = -1
        for (i in 0 until spinner.adapter.count) {
            if (spinner.adapter.getItem(i).toString().contains("QR", ignoreCase = true)) {
                qrIndex = i; break
            }
        }
        assertTrue("Không tìm thấy format QR trong spinner", qrIndex >= 0)

        spinner.setSelection(qrIndex)
        shadowOf(ctx.mainLooper).idle()

        val section = activity.findViewById<View>(R.id.qrStylingSection)
        assertEquals(
            "QR styling phải hiện khi chọn QR_CODE",
            View.VISIBLE, section.visibility
        )
    }

    @Test
    fun batchQrButton_isPresent() {
        val activity = launchEncode().get()
        assertTrue(activity.findViewById<View>(R.id.batchQr) != null)
    }
}
