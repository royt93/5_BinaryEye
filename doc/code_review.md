# Code Review Report

> Lần review gốc: 2026-03-18 | Audit cập nhật: 2026-06-22
> Files scanned: 71 Kotlin files

---

## 🔴 HIGH — Bugs / Crash Risk

### H1 · `ActivityMain.kt` line 88 — Non-null assertion `!!` có thể crash
**Status:** ✅ **FIXED** (audit 2026-06-22)

```kotlin
// Trước (gây NPE nếu extra null):
// intent.getParcelableExtra(DECODED)!!

// Sau (đã fix):
intent.getParcelableExtra(DECODED) ?: return FPreferences()
```

---

### H2 · `BluetoothSender.kt` line 20 — Unmanaged `CoroutineScope`
**Status:** 🔍 **CẦN VERIFY** — Xem `memory_leak.md` H1/H2 để biết pattern đã áp dụng.

```kotlin
CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) { ... }
```
**Vấn đề:** Scope không cancel được nếu caller bị destroy.
**Fix đề xuất:** Truyền scope từ caller hoặc dùng `GlobalScope` (fire-and-forget).

---

### H3 · `ScanSender.kt` line 22 — Unmanaged `CoroutineScope`
**Status:** 🔍 **CẦN VERIFY** — Tương tự H2.

---

## 🟠 MEDIUM — Memory Leaks

### M1 · `BluetoothSender.kt` lines 55–61 — File-level global mutable state
**Status:** 🔍 **CẦN VERIFY**

`socket` và `writer` là file-level globals, không thread-safe, có thể leak nếu coroutine chưa xong mà Activity bị destroy.

---

### M2 · `Beeps.kt` lines 7–8 — File-level ToneGenerator globals
**Status:** ✅ **OK** — `releaseToneGenerators()` được gọi trong `CameraActivity.onDestroy()`. An toàn.

---

## 🟡 LOW — Warnings / Code Quality

### W1 · `BaseActivity.kt` — `println()` debug log trong production
**Status:** ✅ **FIXED** — Đã xóa `println`, comment `// W1: Removed debug println` tại line 37.

---

### W2 · `BaseActivity.kt` — Dead `else` branch không thể đạt được
**Status:** 🔍 **CẦN VERIFY** — Còn tồn tại hay đã cleanup cùng W1.

---

### W3 · Nhiều nơi — Dead code `attachBaseContext` bị comment
**Status:** 🔍 **CẦN VERIFY**

Tìm kiếm:
```
grep -rn "attachBaseContext" app/src/main/kotlin/
```

---

### W4 · `ActivityPick.kt` line 243 — Bug logic trong `rotateClockwise()`
**Status:** ✅ **FIXED** (audit 2026-06-22)

```kotlin
// Trước (sai operator precedence):
// cropImageView.imageRotation += 90 % 360

// Sau (đã fix):
cropImageView.imageRotation = (cropImageView.imageRotation + 90) % 360
```

---

### W5 · `FBarcode.kt` — Redundant `let` với biến không-null
**Status:** 🔍 **CẦN VERIFY**

```kotlin
// Nên đơn giản hóa thành:
ac.toast(message ?: getString(R.string.error_encoding_barcode))
```

---

### W6 · `ActivityCamera.kt` — Stale imports từ Google AdMob SDK
**Status:** 🆕 **MỚI PHÁT HIỆN** (audit 2026-06-22) — Cần fix

File đang import các class Google AdMob dù đã migrate sang `AdManager` wrapper:
```kotlin
// Stale imports cần xóa:
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
```
Code thực tế đã dùng `AdManager.loadBanner()` từ wrapper. Các import này là orphan.

---

### W7 · `RApp.kt` — Log debug trong production
**Status:** 🆕 **MỚI PHÁT HIỆN** (audit 2026-06-22)

```kotlin
Log.d("roy93~", "AdManager init success=$success, gaid=$gaid")
```
Nên wrap trong `if (BuildConfig.DEBUG)`.

---

### W8 · `ActivityCamera.kt` — Log debug lộ thông tin trong production
**Status:** 🆕 **MỚI PHÁT HIỆN** (audit 2026-06-22)

3 chỗ tại lines ~322, 334, 374:
```kotlin
Log.d("roy93~", "Ad đã hiển thị và đóng thành công")
Log.d("roy93~", "Ad không hiển thị được hoặc có lỗi")
```
Nên wrap trong `if (BuildConfig.DEBUG)` hoặc dùng `SafeLogger` từ AdManager wrapper.

---

## 📋 Tổng hợp cập nhật 2026-06-22

| ID | File | Severity | Status | Ghi chú |
|----|------|----------|--------|---------|
| H1 | `ActivityMain.kt:88` | 🔴 HIGH | ✅ FIXED | `?: return FPreferences()` |
| H2 | `BluetoothSender.kt:20` | 🔴 HIGH | 🔍 Verify | |
| H3 | `ScanSender.kt:22` | 🔴 HIGH | 🔍 Verify | |
| M1 | `BluetoothSender.kt:55-61` | 🟠 MEDIUM | 🔍 Verify | |
| M2 | `Beeps.kt:7-8` | 🟠 MEDIUM | ✅ OK | release() trong CameraActivity.onDestroy |
| W1 | `BaseActivity.kt:37` | 🟡 LOW | ✅ FIXED | Đã xóa println |
| W2 | `BaseActivity.kt` | 🟡 LOW | 🔍 Verify | |
| W3 | Nhiều file | 🟡 LOW | 🔍 Verify | |
| W4 | `ActivityPick.kt:243` | 🟡 LOW | ✅ FIXED | `(+ 90) % 360` |
| W5 | `FBarcode.kt` | 🟡 LOW | 🔍 Verify | |
| W6 | `ActivityCamera.kt:25-28` | 🟡 LOW | ✅ FIXED | Đã xóa stale AdMob imports (audit 2026-06-24) |
| W7 | `RApp.kt` | 🟡 LOW | ✅ FIXED | Bọc `if (BuildConfig.DEBUG)` (audit 2026-06-24) |
| W8 | `ActivityCamera.kt:322,334,374` | 🟡 LOW | ✅ FIXED | Bọc `if (BuildConfig.DEBUG)` (audit 2026-06-24) |

---

## 🟢 Audit 2026-06-24 — Feature changeset (F1/F2/F6/E6 + rename)

Review diff trên branch `dev`: filter chips, QR styling, batch QR, scan bottom sheet, rename `CameraActivity`→`ActivityCamera`. **Điểm: 8 → 9/10** sau khi fix.

| # | Vấn đề | File | Status |
|---|--------|------|--------|
| A1 | Thụt lề sai `getScans`/`getScansDetailed` | `Db.kt` | ✅ FIXED |
| A2 | Tên fully-qualified inline + import trùng `COLOR_*` | `FBarcode.kt`, `FEncode.kt` | ✅ FIXED |
| A3 | `MainScope()` fallback chết/leak (3 chỗ: bottom sheet + 2 legacy `showResult`) | `ActivityCamera.kt` | ✅ FIXED — chuyển hết sang `(this as LifecycleOwner).lifecycleScope` |
| A4 | Batch QR không cap → nguy cơ OOM | `FBatchEncode.kt` | ✅ FIXED — cap `MAX_BATCH=200` + toast `batch_qr_capped` |
| A5 | Bo góc logo cố định `8f` | `FBarcode.overlayLogo` | ✅ FIXED — `corner = logoSize * 0.12f` |
| A6 | Color picker chỉ preset (12 màu), chưa custom | `FEncode.showColorPicker` | ⏸️ Chấp nhận cho v1 |

**Verify:** `./gradlew compileDevDebugKotlin` → BUILD SUCCESSFUL.

### 🔴 A7 (HIGH) — Crash khi mở Encode/History/bottom-sheet — PHÁT HIỆN KHI TEST RUNTIME
Compile pass nhưng **app crash lúc inflate layout**:
```
IllegalArgumentException: The style on this component requires your app theme
to be Theme.MaterialComponents (or a descendant).
  at com.google.android.material.button.MaterialButton.<init>
```
**Nguyên nhân:** `AppTheme` (cả `values/` lẫn `values-v14/styles.xml`) parent = `Theme.AppCompat.NoActionBar`. Changeset thêm `MaterialButton`/`Chip`/`MaterialAlertDialogBuilder` vào `roy_f_encode.xml`, `roy_f_history.xml`, `roy_frm_batch_encode.xml`, `roy_bottom_sheet_scan_result.xml` — các widget này yêu cầu theme MaterialComponents.
**Fix:** đổi parent → `Theme.MaterialComponents.NoActionBar.Bridge` (giữ nguyên styling AppCompat, chỉ thoả mãn yêu cầu Material). Status: ✅ FIXED + verified trên device.

**Kiểm thử runtime trên Pixel 7 Pro (VIP active để tắt ad):**
- F2 QR styling: ✅ color picker (preset highlight current), swatch cập nhật, QR sinh ra đúng màu foreground tùy chỉnh.
- F6 Batch QR: ✅ live count "3 items", Generate → 3 thumbnail, Export ZIP → share sheet.
- F1 filter chips: ✅ render (All/Today/Week/Month). 🟡 **Cosmetic:** thanh chip đè lên status bar (thiếu top window-inset).
- E6 bottom sheet: ✅ verified trên S24 Ultra (qua image-pick decode).

### A9 (HIGH) — Quét từ ảnh (ActivityPick) mất kết quả — FIXED
`ActivityPick.showResult()` gọi `showResult(r)` (tạo BottomSheetDialog) rồi `finish()` **ngay** → dialog chết theo activity, kết quả mất (về launcher). Regression do E6 thay `startActivity(getDecodeIntent)` bằng bottom sheet dùng chung cho cả camera lẫn image-pick; camera sống nên OK, ActivityPick finish ngay nên hỏng.
**Fix:** thêm cờ `finishOnDismiss` vào `showResult`/`showScanBottomSheet`; ActivityPick truyền `true` và bỏ `finish()` trực tiếp → host chỉ finish khi sheet đóng (`setOnDismissListener`). Verified: sheet hiện đúng từ image-pick trên S24.

### A10 — Bottom sheet UI: tương phản + edge-to-edge + nút vỡ chữ — FIXED
Theme `.Bridge` → BottomSheet nền trắng mặc định, app dark (chữ sáng) → mất tương phản; nút wrap ("SHA RE"); sát nav bar.
**Fix:** nền tối bo góc (`roy_bg_bottom_sheet`) + container trong suốt, chữ trắng cho nội dung/chip/nút, divider sáng; nút secondary `iconGravity=top` + `maxLines=1` (gọn 1 dòng); listener window-inset chừa navigation bar. Verified trên S24.

**Logcat (S24, suốt phiên test):** không FATAL/crash, không exception từ code app. Chỉ có `libpenguin.so` (native AppLovin, mạng tắt) + `SurfaceFlinger alpha` (animation hệ thống) — đều vô hại.

### A8 (MEDIUM) — F1 date filter lệch múi giờ — FIXED + có test
`SCANS_DATETIME` lưu giờ địa phương (`DateFormat.format(...,currentTimeMillis())`) nhưng filter dùng `date('now')`/`datetime('now')` (UTC) → sai biên (vd VN +7, scan sáng sớm bị "Today" bỏ sót).
**Fix:** thêm `'localtime'` vào các predicate ngày. Đồng thời **refactor** logic build WHERE/args thành hàm thuần `ScanFilter.toWhereClause()`/`toWhereArgs()` (bỏ `Db.buildWhereClause/buildWhereArgs`) để unit-test được trên JVM.

### Unit tests (mới) — `./gradlew testDevDebugUnitTest`
- `database/ScanFilterTest` (7): isDefault, từng nhóm format, **date predicate có `localtime`**, combine AND, args.
- `frm/BatchExportUtilTest` (6): `zipEntryName` sanitize/truncate/fallback/không chứa path separator.
- Kết quả: **13/13 pass**. Thêm `testImplementation 'junit:junit:4.13.2'`.

**Hạn chế còn lại (không phải bug):** color picker preset-only; batch QR giữ toàn bộ bitmap trong RAM (đã cap 200); chưa có test suite (toàn project chưa có).

**Ghi chú tên class:** `ActivityCamera.kt` khai báo class là `CameraActivity` (không phải `ActivityCamera`). Đây là inconsistency với naming convention — file name theo convention prefix `Activity*` nhưng class name bị viết ngược.
