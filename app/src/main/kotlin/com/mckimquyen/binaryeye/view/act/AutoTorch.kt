package com.mckimquyen.binaryeye.view.act

/**
 * [FEAT E1] Pure decision logic cho torch auto-on/off theo cam bien anh sang -
 * tach khoi ActivityCamera/SensorEventListener de unit-test duoc tren JVM
 * thuan, khong phu thuoc Android framework.
 */
internal object AutoTorch {
    const val LOW_LUX = 10f
    const val HIGH_LUX = 50f
    const val DEBOUNCE_MS = 1_500L

    /**
     * @param lowLightSinceMs 0L neu hien khong o trong giai doan toi lien tuc,
     *   nguoc lai la thoi diem bat dau toi lien tuc (epoch ms).
     * @return [lowLightSinceMs] moi + [torchOn] (null = khong doi trang thai
     *   torch trong lan doc nay).
     */
    fun evaluate(lux: Float, nowMs: Long, lowLightSinceMs: Long): Result {
        return when {
            lux < LOW_LUX -> {
                val since = if (lowLightSinceMs == 0L) nowMs else lowLightSinceMs
                val turnOn = (nowMs - since) >= DEBOUNCE_MS
                Result(lowLightSinceMs = since, torchOn = if (turnOn) true else null)
            }

            lux > HIGH_LUX -> Result(lowLightSinceMs = 0L, torchOn = false)
            else -> Result(lowLightSinceMs = 0L, torchOn = null)
        }
    }

    data class Result(val lowLightSinceMs: Long, val torchOn: Boolean?)
}
