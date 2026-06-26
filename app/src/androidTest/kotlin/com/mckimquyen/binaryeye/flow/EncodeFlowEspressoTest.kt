package com.mckimquyen.binaryeye.flow

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.act.ActivityMain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncodeFlowEspressoTest {

    private fun launchEncode(): ActivityScenario<ActivityMain> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ActivityMain::class.java
        ).putExtra("encode", "espresso-test")
        return ActivityScenario.launch(intent)
    }

    @Test
    fun batchQrButton_isDisplayed() {
        launchEncode().use {
            onView(withId(R.id.batchQr)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun formatSpinner_isDisplayed() {
        // Lưu ý: không assert visibility của qrStylingSection ở đây vì nó phụ
        // thuộc format đang chọn (FEncode khôi phục format từ prefs) — logic
        // toggle theo format đã được Robolectric test kiểm soát trạng thái.
        launchEncode().use {
            onView(withId(R.id.format)).check(matches(isDisplayed()))
        }
    }
}
