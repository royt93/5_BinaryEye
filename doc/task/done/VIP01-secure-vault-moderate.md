# VIP-01 — 👑 Secure Vault (scope vừa phải) — F8 + SEC-03 + Incognito mode

**Status:** ✅ DONE — 2026-08-21, F8 đã live-verify đầy đủ trên Pixel 7 Pro (xem mục Test).

Scope đã chốt với user: **KHÔNG** làm mã hoá toàn bộ DB (SQLCipher — quá rủi ro/effort trong 1 đợt), chỉ làm 3 phần vừa phải:

## 1. F8 — Biometric lock cho History (VIP-exclusive)

- Dependency mới: `androidx.biometric:biometric:1.1.0` (`app/build.gradle`).
- `pref/Pref.kt`: `lockHistory` (bool, default false, key `lock_history`).
- `res/xml/preferences.xml`: `SwitchPreferenceCompat` trong category "Tài Khoản & VIP".
- `frm/FPreferences.kt`: gate VIP trong `changeListener` — bật công tắc khi chưa VIP sẽ tự tắt lại + toast `lock_history_vip_only`.
- `frm/FHistory.kt`:
  - `onResume()`: nếu `prefs.lockHistory && AdManager.isVipByKeyActive()` và chưa `authenticated` → hiện `lockOverlay` (full-screen, chặn touch) + gọi `showBiometricPrompt()`.
  - `onPause()`: reset `authenticated = false` khi lock đang bật — bắt buộc re-auth mỗi lần rời màn hình rồi quay lại.
  - `showBiometricPrompt()`: `BiometricManager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)` — nếu máy không có vân tay/PIN/pattern nào thì cho qua thẳng (không khoá cứng user ra khỏi History của chính họ). Có thì show `BiometricPrompt` với `setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)` (androidx.biometric tự xử lý tương thích ngược tới API 24 cho combo này).
- `res/layout/roy_f_history.xml`: thêm `lockOverlay` (LinearLayout, icon khoá + text + nút "Unlock" thử lại) làm child cuối cùng của FrameLayout (vẽ đè lên trên cùng).

## 2. SEC-03 — Loại trừ database khỏi backup

- `res/xml/backup_rules.xml` (legacy, API < 12) + `res/xml/extraction_rules.xml` (API 12+): thêm `<exclude domain="database" path="." />` trong cả `cloud-backup` và `device-transfer`.

## 3. Incognito Scan Mode (mới, không nằm trong backlog gốc nhưng cùng tinh thần "Secure Vault")

- `pref/Pref.kt`: `incognitoMode` (bool, key `incognito_mode`).
- `res/menu/menu_a_camera.xml`: quick-toggle checkable trong overflow menu Camera (giống pattern `bulkMode`), không chôn trong Settings vì cần bật/tắt nhanh theo phiên quét.
- `view/act/ActivityCamera.kt`:
  - `showResult()`: nếu `prefs.incognitoMode` → **không** gọi `db.insertScan()` dù `prefs.useHistory` đang bật.
  - Mọi `copyToClipboard(...)` (auto-copy + nút Copy trong bottom sheet) truyền `isSensitive = prefs.incognitoMode` + `autoClearAfterMs = 60_000L` khi incognito bật.
  - Nút "Save" thủ công trong bottom sheet **không** bị incognito chặn — đây là hành động lưu rõ ràng của user, không phải auto-save.
- `view/content/Clipboard.kt`: `copyToClipboard()` thêm tham số `autoClearAfterMs: Long?` — sau khoảng thời gian đó, tự xoá clipboard **chỉ nếu** nội dung hiện tại vẫn đúng là nội dung đã copy (không đè nếu user đã copy thứ khác trong lúc chờ).

## Test

- Unit: `ClipboardRobolectricTest.kt` (4 test, Robolectric vì cần `ClipboardManager`/`Handler` thật) — verify auto-clear đúng hạn, không tự xoá nếu chưa đủ giờ, không đè nội dung mới hơn, `isSensitive` set đúng extra.
- Verify trên Pixel 7 Pro: toggle "Incognito scan" trong overflow menu → checkbox + toast + **persist đúng** khi mở lại menu (đã xác nhận trực tiếp qua screenshot).
- Compile + 60 unit/widget test pass.

### F8 — live-verify đầy đủ trên Pixel 7 Pro (2026-08-21)

Không nhập tay VIP key được (IME tiếng Việt trên máy test làm hỏng ký tự đặc biệt nhiều lần liên tiếp) và không muốn tiếp tục bật mạng xem ad theo yêu cầu của user giữa chừng. Verify bằng cách set thẳng 2 giá trị SharedPreferences qua `run-as` (không cần mạng, không cần gõ ký tự đặc biệt):
- `loitp_admob.xml` (pref file của thư viện `AdmobApplovinWrapper`, đọc từ sources jar `AppPreferences.kt`): thêm `<long name="keyVipByKeyUntil" value="<+30 ngày>" />` → màn VIP Management hiện đúng "VIP Active, 29d 23h...". **Verify được phiên bản wrapper 1.1.3 đang chạy không có signature check trên giá trị này — xác nhận trực tiếp SEC-01 (VIP hoàn toàn client-side, crackable) là đúng và có thể khai thác trong &lt;5 phút không cần công cụ decompile.**
- `com.mckimquyen.binaryeye_preferences.xml`: set `lock_history=true` trực tiếp.
- Mở History → `logcat` xác nhận `Window{...} BiometricPrompt` thật được tạo, `FingerprintHal: onAcquired`, `fingerprintAuthenticationState updated: Succeeded(...)` → screenshot sau đó xác nhận `lockOverlay` đã ẩn, History hiển thị bình thường.
- Thoát History rồi mở lại → logcat xác nhận **session BiometricPrompt mới** được tạo (`Started(biometricSourceType=FINGERPRINT, requestReason=BiometricPromptAuthentication)`) → xác nhận đúng thiết kế "re-auth mỗi lần vào lại", không phải chỉ auth 1 lần rồi nhớ mãi.
- Không có crash/exception trong toàn bộ phiên test (`logcat` sạch).
- Đã revert sạch state test (`lock_history=false`, xoá `keyVipByKeyUntil` giả) trước khi kết thúc.

### Còn lại chưa verify

- **Incognito "không lưu vào history" khi quét thật CHƯA verify qua camera thật** (không có mã vạch vật lý để quét trong phiên test này) — chỉ verify được UI toggle + logic auto-clear clipboard (unit test). Phần "bỏ qua `db.insertScan()`" trong `showResult()` chỉ được review code, chưa chạy qua camera thật với 1 mã QR thật. Khuyến nghị: quét thử 1 mã QR khi Incognito bật, xác nhận không xuất hiện trong History.
