# VIP-01 — 👑 Secure Vault (scope vừa phải) — F8 + SEC-03 + Incognito mode

**Status:** ✅ DONE (code + unit/widget test) — 2026-08-21. Xem "Hạn chế test" bên dưới trước khi release.

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

## ⚠️ Hạn chế test — CẦN VERIFY THÊM TRƯỚC KHI COI LÀ HOÀN TẤT

- **F8 biometric prompt CHƯA được live-test trên thiết bị thật** (không kích hoạt được VIP qua nhập key do IME tiếng Việt trên máy test làm hỏng ký tự đặc biệt của key nhiều lần liên tiếp, và không muốn tiếp tục bật/tắt mạng để xem quảng cáo reward theo phản hồi của user giữa chừng). Code dùng đúng API chuẩn `androidx.biometric.BiometricPrompt` theo tài liệu chính thức, compile sạch, nhưng **chưa xác nhận bằng mắt** rằng prompt hiện đúng, xác thực thành công đúng ẩn `lockOverlay`, hoặc hành vi khi huỷ/thất bại.
- **Incognito "không lưu history" và "tự xoá clipboard" khi quét thật CHƯA verify qua camera thật** (không có mã vạch vật lý để quét trong phiên test này) — chỉ verify được UI toggle. Logic auto-clear clipboard đã verify kỹ qua unit test (4 test), nhưng phần "bỏ qua `db.insertScan()`" trong `showResult()` chỉ được review code, chưa chạy qua camera thật.

**Khuyến nghị:** trước khi release, tự kích hoạt VIP qua "Xem quảng cáo" (nút free, không cần gõ tay) rồi bật Lock History, thoát History và quay lại để xác nhận prompt hiện đúng; quét thử 1 mã QR thật khi Incognito bật để xác nhận không xuất hiện trong History.
