# E1 — 🔴 Torch Auto-On khi tối

**Status:** ✅ DONE — 2026-08-21, verify trên Pixel 7 Pro

## Implementation

- `view/act/AutoTorch.kt` (mới) — pure decision logic (`evaluate(lux, nowMs, lowLightSinceMs)`), tách khỏi Android framework để unit-test được. Ngưỡng: lux < 10 → cần tối liên tục ≥1.5s mới bật; lux > 50 → tắt ngay.
- `view/act/ActivityCamera.kt`:
  - `SensorEventListener` mới dùng `AutoTorch.evaluate()`, gọi `setTorch()`.
  - `registerAutoTorch()`/`unregisterAutoTorch()` trong `onResume()`/`onPause()`, chỉ đăng ký khi `prefs.autoTorch` bật và thiết bị có `Sensor.TYPE_LIGHT`.
  - Tách `toggleTorchMode()` (tay) / `setTorch(on)` (dùng chung tay + auto). Cờ `userToggledTorch` để tay luôn override auto trong phiên camera hiện tại.
- `pref/Pref.kt`: `autoTorch` (default false) + key `auto_torch`.
- `res/xml/preferences.xml` + `strings.xml`: `SwitchPreferenceCompat` trong category Scan.

## Test

- Unit: `AutoTorchTest.kt` (6 test) — bright/mid/low-light debounce/flicker-reset, tất cả pass.
- Không cần widget/instrumented test riêng: logic quyết định đã tách pure, phần còn lại (SensorManager wiring) không dễ test qua Robolectric (cần sensor thật) — chấp nhận rủi ro thấp vì logic cốt lõi đã unit-test đầy đủ.
