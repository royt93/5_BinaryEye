package com.mckimquyen.binaryeye.view.act

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoTorchTest {

    @Test
    fun brightLight_turnsTorchOff() {
        val result = AutoTorch.evaluate(lux = 100f, nowMs = 0L, lowLightSinceMs = 999L)
        assertEquals(false, result.torchOn)
        assertEquals(0L, result.lowLightSinceMs)
    }

    @Test
    fun midRangeLight_noChange() {
        val result = AutoTorch.evaluate(lux = 25f, nowMs = 0L, lowLightSinceMs = 999L)
        assertNull(result.torchOn)
        assertEquals(0L, result.lowLightSinceMs)
    }

    @Test
    fun lowLight_justStarted_doesNotTurnOnYet() {
        val result = AutoTorch.evaluate(lux = 5f, nowMs = 1_000L, lowLightSinceMs = 0L)
        assertNull(result.torchOn)
        assertEquals(1_000L, result.lowLightSinceMs)
    }

    @Test
    fun lowLight_belowDebounceWindow_doesNotTurnOnYet() {
        val result = AutoTorch.evaluate(lux = 5f, nowMs = 2_000L, lowLightSinceMs = 1_000L)
        assertNull(result.torchOn)
        // lowLightSinceMs khong doi vi van con trong giai doan toi lien tuc
        assertEquals(1_000L, result.lowLightSinceMs)
    }

    @Test
    fun lowLight_pastDebounceWindow_turnsTorchOn() {
        val result = AutoTorch.evaluate(lux = 5f, nowMs = 2_600L, lowLightSinceMs = 1_000L)
        assertEquals(true, result.torchOn)
        assertEquals(1_000L, result.lowLightSinceMs)
    }

    @Test
    fun flickeringAroundThreshold_debounceResetsOnBrightFrame() {
        // Toi -> bat dau dem debounce
        var state = AutoTorch.evaluate(lux = 5f, nowMs = 0L, lowLightSinceMs = 0L)
        assertNull(state.torchOn)
        // Nhap nhay sang tuong doi sang (nhung khong qua HIGH_LUX) truoc khi het debounce
        state = AutoTorch.evaluate(lux = 25f, nowMs = 500L, lowLightSinceMs = state.lowLightSinceMs)
        assertNull(state.torchOn)
        assertEquals(0L, state.lowLightSinceMs) // debounce bi reset
        // Toi lai - phai dem lai debounce tu dau, chua bat torch ngay
        state = AutoTorch.evaluate(lux = 5f, nowMs = 600L, lowLightSinceMs = state.lowLightSinceMs)
        assertNull(state.torchOn)
    }
}
