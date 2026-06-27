# Task & Feature Roadmap — BinaryEye / Cat Scanner

> Cập nhật: 2026-06-27
> Status: ✅ Done | 🟡 In progress | 📋 Picked (chọn làm tiếp) | ⏸️ Deferred | 💭 Ideas
>
> **Đã hoàn thành tới nay:** T1–T4 (tech debt), F1 (filter chips), F2 (QR styling), F6 (batch QR), E6 (scan bottom-sheet). Chi tiết xem `feature.md` / `code_review.md`.

---

## 🔥 Urgent — Bugs & Tech Debt

| # | Status | Task | File | Ghi chú |
|---|--------|------|------|---------|
| T1 | ✅ | Xóa stale AdMob imports | `ActivityCamera.kt` | 4 imports `AdError/AdSize/AdView/LoadAdError` đã xóa |
| T2 | ✅ | Wrap debug Log.d trong BuildConfig.DEBUG | `RApp.kt`, `ActivityCamera.kt` | gaid + 3 ad-state logs đã wrap |
| T3 | ✅ | Verify BluetoothSender + ScanSender CoroutineScope | `BluetoothSender.kt`, `ScanSender.kt` | Đã fix — scope nhận từ caller |
| T4 | ✅ | Rename class `CameraActivity` → `ActivityCamera` | `ActivityCamera.kt`, `ScanTileService.kt`, `AndroidManifest.xml`, `shortcuts.xml` | 5 chỗ đã rename |

---

## 💰 Monetization — In-App Purchase (IAP)

| # | Status | Task | Ghi chú |
|---|--------|------|---------|
| M1 | 📋 | Integrate Google Play Billing Library | Prerequisite cho M2–M5 |
| M2 | ⏸️ | VIP 30 ngày — IAP $0.50 | Enable nút đang disabled trong `FVipManagement` |
| M3 | ⏸️ | VIP 90 ngày — IAP $1.00 | |
| M4 | ⏸️ | VIP 1 năm — IAP $2.00 | |
| M5 | ⏸️ | VIP Lifetime — IAP $3.00 | |
| M6 | ⏸️ | Restore purchase (sau khi uninstall/reinstall) | Required by Play Store policy |

---

## ⭐ Tính Năng Mới Độc Quyền (Exclusive Features)

### F1 — Smart History Search & Filter ✅ DONE 2026-06-22
**Priority:** 🔴 High
**Mô tả:** Filter và tìm kiếm trong lịch sử scan.
- Search by content (full-text search trong SQLite)
- Filter by format (QR, EAN, Code128...)
- Filter by date range (today / this week / this month / custom)
- Sort by: newest, oldest, format, content length
**Files cần sửa:** `FHistory.kt`, `Db.kt` (thêm query), `menu_f_history.xml`
**Độc quyền:** Free user thấy filter nhưng tối đa 7 ngày gần nhất; VIP không giới hạn

---

### F2 — QR Code Styling / Custom Logo ✅ DONE 2026-06-23
**Priority:** 🔴 High
**Mô tả:** Generate QR code có màu và logo tùy chỉnh.
- Chọn foreground color, background color
- Embed logo (từ gallery) vào giữa QR
- Preview real-time trước khi share/save
**Files cần tạo:** `view/graphics/StyledQrGenerator.kt`
**Files cần sửa:** `FEncode.kt`, `FBarcode.kt`
**Lưu ý:** Phải đảm bảo error correction level đủ cao (Q/H) khi có logo

---

### F3 — Scan Groups / Tags
**Priority:** 🟠 Medium
**Mô tả:** Gắn nhãn (tag) cho các scan trong history để phân loại.
- Tag: Work / Personal / Shopping / Travel / Custom
- Multi-tag cho một scan
- Filter history by tag
**Files cần sửa:** `database/Db.kt` (thêm column tags), schema migration, `FHistory.kt`, `FDecode.kt`
**Độc quyền:** Tính năng base free, unlimited tags là VIP

---

### F4 — Geo-Tagged Scans (VIP exclusive)
**Priority:** 🟠 Medium
**Mô tả:** Gắn vị trí GPS vào mỗi scan trong history.
- Hiển thị location name hoặc coordinates trong scan detail
- Filter history theo khu vực
- Map view (optional, dùng Google Maps Intent)
**Files cần sửa:** `database/Scan.kt` (thêm lat/lng), `Db.kt`, `ActivityCamera.kt`
**Permission cần thêm:** `ACCESS_FINE_LOCATION` (optional permission)

---

### F5 — Barcode Comparison Tool
**Priority:** 🟡 Low
**Mô tả:** So sánh nội dung 2 barcode.
- Chọn 2 scan từ history
- Hiển thị diff (giống diff editor): text màu đỏ là khác, xanh là giống
- Hữu ích cho: so sánh serial number, check product batch
**Files cần tạo:** `frm/FBarcodeCompare.kt`, layout `roy_frm_barcode_compare.xml`

---

### F6 — Batch QR Generation ✅ DONE 2026-06-23
**Priority:** 🟠 Medium
**Mô tả:** Generate nhiều QR code cùng lúc từ danh sách text.
- Import từ clipboard (mỗi dòng 1 QR)
- Export batch thành ZIP file chứa PNG
- Hữu ích cho: in nhãn hàng, tạo QR cho danh sách sản phẩm
**Files cần tạo:** `frm/FBatchEncode.kt`
**Độc quyền:** VIP exclusive (free giới hạn 5 QR/lần)

---

### F7 — Scan Reminder / Scheduled Scan
**Priority:** 🟡 Low
**Mô tả:** Nhắc nhở user scan một mã cụ thể vào thời điểm đã đặt.
- Lưu barcode + thời gian nhắc nhở
- Notification hiện đúng giờ → tap → mở CameraActivity với ROI đã config
- Use case: nhắc check-in, nhắc scan thẻ tích điểm
**Files cần tạo:** `sv/ReminderService.kt`, `database/Reminder.kt`

---

### F8 — PIN / Biometric Lock for History
**Priority:** 🟠 Medium
**Mô tả:** Khóa màn hình History bằng PIN hoặc vân tay.
- Toggle trong Settings
- Dùng `BiometricPrompt` (API 28+), fallback PIN trên API 24–27
- History content ẩn khi lock
**Files cần sửa:** `FHistory.kt`, `Pref.kt`, `preferences.xml`
**Độc quyền:** VIP exclusive

---

### F9 — Auto-Action Config (Smart Open)
**Priority:** 🟠 Medium
**Mô tả:** Tự động thực hiện action khi scan loại barcode cụ thể mà không cần confirm.
- Config per format: "Khi scan WiFi QR → tự connect, không hỏi"
- Config per action type: "Khi scan URL → tự mở browser"
- Override manual với long-press để xem kết quả
**Files cần sửa:** `ActionRegistry.kt`, `Pref.kt`, `preferences.xml`

---

### F10 — OCR → QR Pipeline
**Priority:** 🟡 Low
**Mô tả:** Chụp text từ camera (OCR) rồi tự encode thành QR code.
- Dùng ML Kit Text Recognition
- User khoanh vùng text → app extract → auto navigate đến Encode với text đó
- Use case: chụp địa chỉ, số điện thoại từ danh thiếp → tạo QR
**Files cần tạo:** `frm/FOcrEncode.kt`, thêm dependency ML Kit

---

## 🔧 Enhance Tính Năng Hiện Có

### E1 — Torch Auto-On trong điều kiện tối
**Priority:** 🔴 High
**Mô tả:** Tự động bật flash khi ambient light thấp (dùng camera `exposureCompensation` hoặc `Sensor.TYPE_LIGHT`).
**Files cần sửa:** `ActivityCamera.kt`

---

### E2 — History — Date Group Header
**Priority:** 🟠 Medium
**Mô tả:** Phân nhóm history theo ngày (Today / Yesterday / This week / Older).
- Sử dụng `RecyclerView.ItemDecoration` để vẽ header
- Không thay đổi adapter logic phức tạp
**Files cần sửa:** `adapter/ScansAdapter.kt`, `FHistory.kt`

---

### E3 — Encoder — Custom QR Size
**Priority:** 🟡 Low
**Mô tả:** Cho phép chọn kích thước output QR (128 / 256 / 512 / 1024px).
**Files cần sửa:** `FEncode.kt`, `FBarcode.kt` (param `size`)

---

### E4 — VIP Screen — Confetti nâng cấp
**Priority:** 🟡 Low
**Mô tả:** Thay confetti hiện tại bằng lib chuyên nghiệp hơn.
- Gợi ý: `nl.dionsegijn:konfetti-android` hoặc custom particle system
- Particles bay từ 2 góc trên hoặc burst từ crown icon

---

### E5 — History — CSV Export với bộ lọc
**Priority:** 🟠 Medium
**Mô tả:** Export chỉ những scan đã filter (hiện tại export ALL).
- Khi có filter active → export dialog hỏi "Export filtered (N scans) or all?"

---

### E6 — Decode Result — Quick Actions Bottom Sheet ✅ DONE 2026-06-24
**Priority:** 🟠 Medium
**Mô tả:** Sau khi scan thành công, thay vì navigate sang `FDecode`, hiện bottom sheet với quick actions (Copy / Open / Share / Save) để user không mất focus khỏi camera.
**Files cần sửa:** `ActivityCamera.kt`, tạo `roy_bottom_sheet_scan_result.xml`

---

### E7 — Camera — Zoom memory (nhớ level zoom)
**Priority:** 🟡 Low
**Mô tả:** Lưu zoom level cuối cùng vào `Pref`, restore khi mở lại camera.
**Files cần sửa:** `ActivityCamera.kt`, `Pref.kt`

---

### E8 — Splash — Skip nếu đã mở gần đây
**Priority:** 🟡 Low
**Mô tả:** Nếu app mở trong 30 phút gần nhất (cold start lại do hệ thống kill), skip App Open ad và vào Main nhanh hơn.
**Files cần sửa:** `ActivitySplash.kt`, `Pref.kt`

---

## Priority Summary

| Priority | Items | Ghi chú |
|---|---|---|
| ✅ Done | T1–T4, F1, F2, F6, E6 | Tech debt + 4 feature đã merge vào `dev` |
| 🔴 High (còn lại) | E1 | Torch auto-on |
| 🟠 Medium | M1, F3, F4, F8, F9, E2, E5 | IAP + features giữ chân user |
| 🟡 Low | F5, F7, F10, E3, E4, E7, E8 | Nice-to-have |
| ⏸️ Deferred | M2–M6 | Chờ IAP integration (M1) xong |
