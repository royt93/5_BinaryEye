# Task & Feature Roadmap — BinaryEye / Cat Scanner

> ⚠️ **SUPERSEDED 2026-08-21** — nguồn ưu tiên chính nay là **`doc/task/BACKLOG.md`** (audit lại toàn bộ 14 item dưới đây bằng code thật + 3 AI review độc lập, phát hiện 17 bug mới trong đó có 1 P0 mất dữ liệu). File này giữ lại vì phần "Implementation Plans" chi tiết bên dưới vẫn còn giá trị tham chiếu kỹ thuật cho từng item — nhưng **trạng thái/priority ở file này đã lỗi thời**, xem BACKLOG.md trước khi bắt tay code.
>
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

---

# 🧩 Implementation Plans — rã chi tiết (2026-06-27)

> Rã 14 mục (E1 + F3–F10 + E2–E8, **bỏ Monetization M1–M6**) dựa trên khảo sát codebase thực tế.
> Quy ước status mỗi mục: **📋 TODO** → **🔄 IN PROGRESS** → **✅ DONE**. Cập nhật badge ngay khi đổi trạng thái.
> ⚠️ = phát hiện khảo sát làm đổi scope so với roadmap gốc.

## Bảng điều phối

| ID | Feature | Status | Độ lớn | Dependency mới | Ghi chú scope |
|----|---------|--------|--------|----------------|---------------|
| E7 | Zoom memory | 📋 TODO | XS | — | ⚠️ Gần như đã có sẵn — chỉ verify + toggle |
| E3 | Custom QR size | 📋 TODO | S | — | ⚠️ SeekBar đã chỉnh size — chỉ thêm preset |
| E1 | Torch auto-on | 📋 TODO | M | — | Cần `Sensor.TYPE_LIGHT` (không cần permission) |
| E8 | Splash skip gần đây | 📋 TODO | S | — | Pref timestamp |
| E5 | CSV export theo filter | 📋 TODO | S | — | ⚠️ Export đã dùng `scanFilter` — phần lớn đã đúng, cần verify |
| E2 | History date header | 📋 TODO | M | — | ⚠️ ListView+CursorAdapter, KHÔNG phải RecyclerView |
| E4 | Confetti nâng cấp | 📋 TODO | S | `konfetti-android` | Thay custom particle |
| F3 | Scan tags | 📋 TODO | L | — | Bump DB v6→v7 |
| F4 | Geo-tag scans | 📋 TODO | L | `play-services-location` | Bump DB v6→v7 + permission |
| F8 | Biometric lock | 📋 TODO | M | `androidx.biometric` | VIP exclusive |
| F9 | Auto-action config | 📋 TODO | M | — | Mở rộng ActionRegistry |
| F5 | Barcode compare | 📋 TODO | M | — | Greenfield |
| F7 | Scan reminder | 📋 TODO | L | `WorkManager` | Greenfield + notification |
| F10 | OCR → QR | 📋 TODO | L | ML Kit text-recognition | Greenfield |

---

## E7 — Zoom Memory · 📋 TODO
**⚠️ Re-scope:** Zoom **đã được nhớ** rồi — `ActivityCamera.saveZoom()` (onDestroy) ghi `ZOOM_LEVEL`/`ZOOM_MAX` vào SharedPreferences; `restoreZoom()` (trong `initZoomBar()`, chạy onResume) khôi phục. Tính năng cốt lõi xem như **đã có**.
**Việc còn lại:**
1. Verify thủ công: zoom → kill app → mở lại → zoom giữ nguyên (xem test E7).
2. (Tùy chọn) thêm toggle `remember_zoom` trong `preferences.xml` + `Pref.kt`; nếu off thì `restoreZoom()` bỏ qua.
**Test:** đặt zoom ~70% → thoát hẳn app → mở lại Camera → SeekBar đúng vị trí cũ.

## E3 — Custom QR Size · 📋 TODO
**⚠️ Re-scope:** `FEncode` đã có `SeekBar sizeBarView` + `getSize(power)=128*(power+1)` (FEncode.kt:39,430) → size đã tùy chỉnh được liên tục.
**Việc còn lại:**
1. `frm/FEncode.kt`: thêm 4 chip/preset 128/256/512/1024 set thẳng `sizeBarView.progress` tương ứng (128→power0, 256→power1, 512→power3, 1024→power7).
2. Hiển thị label "Npx" cạnh SeekBar (cập nhật trong `updateSize()`).
**Test:** chọn 512 → Encode → bitmap đúng 512px; SeekBar và preset đồng bộ.

## E1 — Torch Auto-On khi tối · 📋 TODO
**Hiện trạng:** `toggleTorchMode()` (ActivityCamera.kt:672) bật/tắt qua `Camera.Parameters.FLASH_MODE_TORCH`; FAB id `flash`; **chưa** dùng cảm biến ánh sáng (grep `TYPE_LIGHT` = rỗng).
**Steps:**
1. `Pref.kt`: thêm `AUTO_TORCH = "auto_torch"` + accessor `autoTorch` (mặc định false) + load trong `update()`.
2. `preferences.xml`: thêm `SwitchPreferenceCompat key="auto_torch"` trong Scan category + string title/summary.
3. `ActivityCamera.kt`: lấy `SensorManager` + `Sensor.TYPE_LIGHT`; đăng ký `SensorEventListener` trong `onResume` (chỉ khi `prefs.autoTorch`), hủy trong `onPause`.
4. Logic ngưỡng + chống nhấp nháy (debounce/hysteresis): lux < ~10 trong >1.5s → bật torch; lux > ~50 → tắt; chỉ tự tác động khi user chưa toggle tay (cờ `userToggledTorch`).
5. Tách helper `setTorch(on: Boolean)` từ `toggleTorchMode()` để cả tay lẫn auto dùng chung.
**Risk:** `TYPE_LIGHT` không có trên mọi máy → null-check sensor, ẩn toggle nếu thiếu. Không cần permission mới.
**Test:** bật setting → che camera vào chỗ tối → torch tự bật; ra sáng → tự tắt; toggle tay vẫn override.

## E8 — Splash Skip nếu mở gần đây · 📋 TODO
**Steps:**
1. `Pref.kt`: `LAST_FOREGROUND_MS = "last_foreground_ms"` (Long) + accessor.
2. `ActivitySplash.kt`: nếu `now - prefs.lastForegroundMs < 30*60_000` → bỏ App Open ad, `goToMain()` ngay.
3. Ghi `prefs.lastForegroundMs = now` khi vào Main (hoặc onPause của Main).
**Test:** mở app → vào Main → back ra → mở lại trong 30' → không thấy App Open ad, vào Main nhanh.

## E5 — History CSV Export theo bộ lọc · 📋 TODO
**⚠️ Re-scope:** `FHistory.askToExportToFile()` đã gọi `db.getScansDetailed(scanFilter)` → export **đã tôn trọng filter** đang áp. Có thể đã đạt yêu cầu.
**Việc còn lại:**
1. Verify: áp filter (Today / QR) → Export CSV → file chỉ chứa scan khớp filter.
2. (Tùy chọn) thêm dòng tiêu đề/ghi chú filter vào đầu CSV; hỏi user "export all vs filtered" nếu đang có filter.
**Test:** filter "This week" → export → đếm dòng khớp số item hiển thị.

## E2 — History Date Group Header · 📋 TODO
**⚠️ Re-scope quan trọng:** `FHistory` dùng **`ListView` + `ScansAdapter : CursorAdapter`** (không phải RecyclerView) → **không dùng được `RecyclerView.ItemDecoration`** như roadmap gốc ghi.
**Hướng A (ít rủi ro, đề xuất):** thêm section header ngay trong `CursorAdapter`:
1. `ScansAdapter`: override `getViewTypeCount()=2`, `getItemViewType()` trả HEADER khi item là ngày đầu nhóm.
2. Tính nhóm (Today/Yesterday/This week/Older) từ `SCANS_DATETIME` của row hiện tại vs row trước; cần map vị trí→ngày (precompute khi `changeCursor`).
3. Layout `roy_v_row_scan_header.xml` cho header.
**Hướng B (lớn):** migrate ListView→RecyclerView rồi mới dùng ItemDecoration (đụng selection/ActionMode hiện tại → tốn).
**Test:** history nhiều ngày → thấy header phân nhóm đúng, scroll mượt, longpress-select vẫn hoạt động.

## E4 — Confetti nâng cấp · 📋 TODO
**Hiện trạng:** `FVipManagement.showConfetti()` (line 767) là custom 50 particle + ObjectAnimator.
**Steps:**
1. `app/build.gradle`: thêm `nl.dionsegijn:konfetti-xml:<ver>`.
2. Thêm `KonfettiView` vào `roy_frm_vip_management.xml` (overlay trên cùng).
3. Thay thân `showConfetti()` bằng burst từ vị trí crown icon (2 nguồn góc trên); xóa code particle thủ công.
4. Giữ haptic `playCelebration()` như cũ.
**Test:** activate VIP bằng key hợp lệ → confetti burst đẹp; navigate back/lại không leak (xem AT-VIP-15).

## F9 — Auto-Action Config (Smart Open) · 📋 TODO
**Hiện trạng:** `ActionRegistry.getAction(data)` trả action đầu match (ActionRegistry.kt:31). Hiện scan xong hiện bottom-sheet (E6) để user chọn.
**Steps:**
1. `Pref.kt`: `AUTO_ACTION = "auto_action"` (Bool, mặc định false) — "tự chạy action mặc định, không hỏi".
2. `preferences.xml`: SwitchPreferenceCompat trong Content category.
3. `ActivityCamera.showScanBottomSheet(...)`: nếu `prefs.autoAction` và action != fallback `OpenOrSearchAction` → gọi thẳng `action.execute()` thay vì hiện sheet.
4. Tôn trọng `prefs.openImmediately`/`copyImmediately` đang có để tránh chồng chéo.
**Test:** bật setting → scan URL → mở trình duyệt ngay; scan text thường → vẫn hiện sheet (vì là fallback).

## F3 — Scan Groups / Tags · 📋 TODO
**Hiện trạng:** DB version **6**, 15 cột, `onUpgrade` incremental (`if oldVersion < N`).
**Steps:**
1. `Db.kt`: bump version `6→7`; thêm `SCANS_TAGS = "tags"` (TEXT, CSV các tag); thêm `addTagsColumn()` + nhánh `if (oldVersion < 7)` trong `onUpgrade`; thêm cột vào `CREATE TABLE`.
2. `Db.kt`: method `setTags(id, tags)`, đưa `tags` vào `getScan`/`getScansDetailed`/`Scan.kt`.
3. `ScanFilter.kt`: thêm `tag: String?` → predicate `tags LIKE '%<tag>%'`.
4. UI: dialog gán tag (multi từ preset Work/Personal/Shopping/Travel + custom) trong `FHistory` ActionMode + chip filter theo tag.
5. VIP gate: free giới hạn số tag; unlimited là VIP (`AdManager.isVipByKeyActive()`).
**Test:** gán 2 tag cho 1 scan → filter theo tag → đúng; nâng cấp DB từ bản cũ không mất dữ liệu (cài đè bản v6).

## F4 — Geo-Tagged Scans (VIP) · 📋 TODO
**Steps:**
1. `Db.kt`: bump `6→7` (gộp chung migration với F3 nếu làm cùng đợt) thêm `SCANS_LAT`/`SCANS_LNG` (REAL).
2. `app/build.gradle`: `com.google.android.gms:play-services-location`.
3. `AndroidManifest.xml`: `ACCESS_COARSE_LOCATION` (+ runtime request).
4. Khi lưu scan (nếu VIP + bật setting): lấy last location, ghi lat/lng.
5. UI: hiển thị vị trí ở scan detail (text + nút mở Maps qua `geo:` intent).
**Test:** bật + cấp quyền → scan → detail có toạ độ + mở Maps; free user không thấy.

## F8 — PIN / Biometric Lock cho History · 📋 TODO (VIP)
**Steps:**
1. `app/build.gradle`: `androidx.biometric:biometric`.
2. `Pref.kt` + `preferences.xml`: toggle `lock_history`.
3. `FHistory.onResume`: nếu bật + VIP → `BiometricPrompt` (API 28+), fallback device-credential (PIN) cho API 24–27; nội dung ẩn (overlay) tới khi auth.
4. Re-auth khi quay lại từ background.
**Test:** bật → rời/quay lại History → yêu cầu vân tay/PIN; hủy auth → không lộ nội dung.

## F5 — Barcode Comparison Tool · 📋 TODO
**Steps:**
1. `FHistory` ActionMode: cho chọn đúng 2 scan → menu "Compare".
2. Tạo `frm/FCompare.kt` + `roy_frm_compare.xml`: 2 cột so sánh content/format/datetime, highlight khác biệt (diff theo ký tự cho content).
3. Dùng `db.getScan(id)` cho cả 2.
**Test:** chọn 2 scan → Compare → thấy bảng so sánh + chỗ khác được tô.

## F7 — Scan Reminder / Scheduled Scan · 📋 TODO
**Steps:**
1. `Db.kt` hoặc bảng mới `reminders` (barcode + time) — hoặc lưu qua `database/Reminder.kt`.
2. `app/build.gradle`: `androidx.work:work-runtime` (WorkManager) — thay cho AlarmManager để bền với Doze.
3. `sv/ReminderWorker.kt`: bắn notification đúng giờ → tap mở `ActivityCamera` với ROI/format đã cấu hình.
4. `AndroidManifest.xml`: `POST_NOTIFICATIONS` (API 33+).
5. UI đặt nhắc nhở (chọn barcode + thời gian).
**Test:** đặt nhắc sau 2' → app background → notification hiện → tap → mở camera đúng cấu hình.

## F10 — OCR → QR Pipeline · 📋 TODO
**Steps:**
1. `app/build.gradle`: ML Kit `com.google.mlkit:text-recognition`.
2. `frm/FOcrEncode.kt`: pick/chụp ảnh → ML Kit `TextRecognition` trích text → đổ vào `FEncode` để tạo QR.
3. Cho sửa text trước khi encode.
**Test:** ảnh có chữ → OCR ra text đúng → tạo QR đúng nội dung.

---

> 📁 Folder `doc/task/{todo,inprogress,done}` để trống — có thể dùng kanban: tách mỗi feature lớn (F3/F4/F7/F10) thành file riêng khi bắt đầu làm, di chuyển giữa 3 thư mục theo status. Hiện tại tất cả 14 mục = **📋 TODO**.
