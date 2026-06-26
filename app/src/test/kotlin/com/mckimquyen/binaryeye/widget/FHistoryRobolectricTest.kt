package com.mckimquyen.binaryeye.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class FHistoryRobolectricTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        prefs.init(ctx)
        db.open(ctx)
    }

    private fun launchHistory() = Robolectric.buildActivity(
        ActivityMain::class.java,
        Intent(ctx, ActivityMain::class.java).putExtra("history", true)
    ).setup().get()

    @Test
    fun chipGroups_existWithDefaultSelection() {
        val activity = launchHistory()
        val dateGroup = activity.findViewById<ChipGroup>(R.id.chipGroupDate)
        val formatGroup = activity.findViewById<ChipGroup>(R.id.chipGroupFormat)
        assertTrue(dateGroup != null && formatGroup != null)
        assertEquals("Mặc định date = All", R.id.chipDateAll, dateGroup.checkedChipId)
        assertEquals("Mặc định format = All", R.id.chipFormatAll, formatGroup.checkedChipId)
    }

    @Test
    fun selectingDateChip_updatesSelection_noCrash() {
        val activity = launchHistory()
        val today = activity.findViewById<Chip>(R.id.chipDateToday)
        today.performClick()
        shadowOf(ctx.mainLooper).idle()
        val dateGroup = activity.findViewById<ChipGroup>(R.id.chipGroupDate)
        assertEquals(R.id.chipDateToday, dateGroup.checkedChipId)
    }

    @Test
    fun selectingFormatChip_updatesSelection_noCrash() {
        val activity = launchHistory()
        val qr = activity.findViewById<Chip>(R.id.chipFormatQr)
        qr.performClick()
        shadowOf(ctx.mainLooper).idle()
        val formatGroup = activity.findViewById<ChipGroup>(R.id.chipGroupFormat)
        assertEquals(R.id.chipFormatQr, formatGroup.checkedChipId)
    }

    @Test
    fun singleSelection_dateGroup_onlyOneChecked() {
        val activity = launchHistory()
        val dateGroup = activity.findViewById<ChipGroup>(R.id.chipGroupDate)
        activity.findViewById<Chip>(R.id.chipDateWeek).performClick()
        shadowOf(ctx.mainLooper).idle()
        activity.findViewById<Chip>(R.id.chipDateMonth).performClick()
        shadowOf(ctx.mainLooper).idle()
        assertEquals("singleSelection: chỉ Month được chọn", R.id.chipDateMonth, dateGroup.checkedChipId)
    }
}
