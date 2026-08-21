package com.mckimquyen.binaryeye.widget

import android.app.Application
import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.adapter.ScansAdapter
import com.mckimquyen.binaryeye.database.Db
import com.mckimquyen.binaryeye.database.Scan
import com.mckimquyen.binaryeye.database.ScanFilter
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.prefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

// [FEAT E2] Widget test cho ScansAdapter voi header ngay - dung Robolectric
// vi can View/TextView/ViewGroup that (khong the mo phong bang JUnit thuan).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class ScansAdapterRobolectricTest {

    private lateinit var ctx: Context
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Before
    fun setup() {
        val appContext: Context = ApplicationProvider.getApplicationContext()
        // AppTheme can bo attr (colorAccent...) ma header layout dung -
        // Application context tran khong co theme nen inflate se throw.
        ctx = androidx.appcompat.view.ContextThemeWrapper(appContext, R.style.AppTheme)
        prefs.init(ctx)
        db.open(ctx)
        prefs.ignoreConsecutiveDuplicates = false
    }

    @After
    fun tearDown() {
        db.getScans(ScanFilter())?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    db.removeScan(cursor.getLong(cursor.getColumnIndex(Db.SCANS_ID)))
                } while (cursor.moveToNext())
            }
        }
    }

    private fun insertScanAt(content: String, daysAgo: Int) {
        val ms = System.currentTimeMillis() - daysAgo * 24 * 60 * 60 * 1000L
        db.insertScan(
            Scan(
                content = content,
                raw = null,
                format = "QR_CODE",
                dateTime = format.format(ms),
            )
        )
    }

    @Test
    fun headersInserted_beforeEachDateGroup() {
        insertScanAt("today-1", daysAgo = 0)
        insertScanAt("today-2", daysAgo = 0)
        insertScanAt("yesterday-1", daysAgo = 1)
        insertScanAt("older-1", daysAgo = 30)

        val cursor = db.getScans(ScanFilter())!!
        val adapter = ScansAdapter(ctx, cursor)

        // 4 dong that + 3 header (Today, Yesterday, Older) = 7
        assertEquals(7, adapter.count)
        assertEquals(2, adapter.viewTypeCount)
    }

    @Test
    fun headerRows_areDisabled_itemRows_areEnabled() {
        insertScanAt("today-1", daysAgo = 0)
        insertScanAt("older-1", daysAgo = 30)

        val cursor = db.getScans(ScanFilter())!!
        val adapter = ScansAdapter(ctx, cursor)

        assertFalse(adapter.areAllItemsEnabled())
        // position 0 = header "Today", position 1 = item today-1,
        // position 2 = header "Older", position 3 = item older-1
        assertFalse("position 0 la header, phai disabled", adapter.isEnabled(0))
        assertTrue("position 1 la item, phai enabled", adapter.isEnabled(1))
        assertFalse("position 2 la header, phai disabled", adapter.isEnabled(2))
        assertTrue("position 3 la item, phai enabled", adapter.isEnabled(3))
    }

    @Test
    fun headerView_showsGroupLabel_itemView_showsContent() {
        insertScanAt("today-content", daysAgo = 0)

        val cursor = db.getScans(ScanFilter())!!
        val adapter = ScansAdapter(ctx, cursor)
        val parent = android.widget.LinearLayout(ctx)

        val headerView = adapter.getView(0, null, parent)
        assertEquals(
            ctx.getString(R.string.history_group_today),
            (headerView as TextView).text.toString()
        )

        val itemView = adapter.getView(1, null, parent)
        val contentView = itemView.findViewById<TextView>(R.id.content)
        assertEquals("today-content", contentView.text.toString())
    }

    @Test
    fun getItem_onHeaderPosition_returnsNull_onItemPosition_returnsCursor() {
        insertScanAt("today-content", daysAgo = 0)

        val cursor = db.getScans(ScanFilter())!!
        val adapter = ScansAdapter(ctx, cursor)

        assertEquals(null, adapter.getItem(0))
        assertTrue(adapter.getItem(1) is android.database.Cursor)
    }

    @Test
    fun getContent_indexedByAdapterPosition_matchesInsertedScan() {
        insertScanAt("alpha", daysAgo = 0)
        insertScanAt("beta", daysAgo = 1)

        val cursor = db.getScans(ScanFilter())!!
        val adapter = ScansAdapter(ctx, cursor)

        // position 0 = header Today, 1 = alpha, 2 = header Yesterday, 3 = beta
        assertEquals("alpha", adapter.getContent(1))
        assertEquals("beta", adapter.getContent(3))
    }
}
