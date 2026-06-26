package com.mckimquyen.binaryeye.flow

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.act.ActivityMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryFlowEspressoTest {

    private fun launchHistory(): ActivityScenario<ActivityMain> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ActivityMain::class.java
        ).putExtra("history", true)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun filterChipsBar_isDisplayed() {
        launchHistory().use {
            onView(withId(R.id.chipDateAll)).check(matches(isChecked()))
            onView(withId(R.id.chipFormatAll)).check(matches(isChecked()))
        }
    }

    @Test
    fun tappingTodayChip_checksIt() {
        launchHistory().use {
            onView(withId(R.id.chipDateToday)).perform(click())
            onView(withId(R.id.chipDateToday)).check(matches(isChecked()))
        }
    }

    @Test
    fun tappingFormatQrChip_checksIt() {
        // Chip format nằm ngoài màn hình (HorizontalScrollView) → dri/assert qua
        // onActivity để test trực tiếp listener chip→filter, không phụ thuộc cuộn.
        launchHistory().use { scenario ->
            scenario.onActivity { it.findViewById<Chip>(R.id.chipFormatQr).performClick() }
            scenario.onActivity {
                assertEquals(
                    R.id.chipFormatQr,
                    it.findViewById<ChipGroup>(R.id.chipGroupFormat).checkedChipId
                )
            }
        }
    }

    @Test
    fun dateAndFormatChips_selectableIndependently() {
        launchHistory().use { scenario ->
            // Date chip hiển thị → click qua Espresso; format chip → onActivity.
            onView(withId(R.id.chipDateWeek)).perform(click())
            scenario.onActivity { it.findViewById<Chip>(R.id.chipFormat2d).performClick() }
            onView(withId(R.id.chipDateWeek)).check(matches(isChecked()))
            scenario.onActivity {
                assertEquals(
                    R.id.chipFormat2d,
                    it.findViewById<ChipGroup>(R.id.chipGroupFormat).checkedChipId
                )
            }
        }
    }
}
