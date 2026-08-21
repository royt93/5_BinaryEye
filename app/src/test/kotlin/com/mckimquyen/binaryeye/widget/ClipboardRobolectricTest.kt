package com.mckimquyen.binaryeye.widget

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mckimquyen.binaryeye.view.content.copyToClipboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

// [FEAT Incognito] Widget test cho auto-clear clipboard - can Robolectric vi
// dung ClipboardManager that + Handler.postDelayed that.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class ClipboardRobolectricTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val clipboardManager =
        ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun currentClipText(): String? =
        clipboardManager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

    private fun idleFor(seconds: Long) {
        shadowOf(ctx.mainLooper).idleFor(Duration.ofSeconds(seconds))
    }

    @Test
    fun withoutAutoClear_textStaysOnClipboard() {
        ctx.copyToClipboard("hello")
        idleFor(300)
        assertEquals("hello", currentClipText())
    }

    @Test
    fun withAutoClear_textClearedAfterDelay() {
        ctx.copyToClipboard("secret-wifi-password", autoClearAfterMs = 60_000L)
        assertEquals("secret-wifi-password", currentClipText())

        idleFor(59)
        assertEquals("chua het 60s - van con", "secret-wifi-password", currentClipText())

        idleFor(2)
        assertEquals("", currentClipText())
    }

    @Test
    fun withAutoClear_doesNotOverwriteNewerClipboardContent() {
        ctx.copyToClipboard("first", autoClearAfterMs = 60_000L)
        // User copy noi dung khac truoc khi timer cu het han.
        ctx.copyToClipboard("second-copied-by-user")

        idleFor(61)

        // Timer cua "first" khong duoc xoa nham noi dung moi.
        assertEquals("second-copied-by-user", currentClipText())
    }

    @Test
    fun isSensitive_setsClipDescriptionExtra() {
        ctx.copyToClipboard("4111111111111111", isSensitive = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val extraKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                "android.content.extra.IS_SENSITIVE"
            }
            val extras = clipboardManager.primaryClipDescription?.extras
            assertTrue(extras?.getBoolean(extraKey) == true)
        }
    }
}
