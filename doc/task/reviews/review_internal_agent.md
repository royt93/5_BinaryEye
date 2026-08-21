# Internal Audit — general-purpose agent (in-process, 2026-08-21)

> Nguồn: agent nội bộ (Claude Code sub-agent, cùng phiên), quyền Read/Grep/Glob/Bash — không sửa file.
> Nhiệm vụ: verify thực tế 14 item TODO trong doc/task.md bằng grep code, tìm bug/tech-debt mới, tìm lỗ hổng test, đề xuất idea VIP mới.

# BinaryEye / Cat Scanner — Code Audit Report

## 1. Verify 14 TODO items (doc/task.md, claimed 2026-06-27)

| # | Item | Status thực tế | Evidence |
|---|------|------|----------|
| E1 | Torch auto-on (light sensor) | **CHƯA làm** | `grep TYPE_LIGHT\|AUTO_TORCH` → 0 kết quả toàn repo. `ActivityCamera.kt:667-678` chỉ có `toggleTorchMode()` thủ công qua nút bấm, không `SensorManager`/`SensorEventListener` nào tồn tại. |
| E2 | History date group header | **CHƯA làm** | `adapter/ScansAdapter.kt:18-19` là `CursorAdapter` thuần (`class ScansAdapter(...) : CursorAdapter(...)`), `FHistory.kt:49` dùng `ListView` — không có `ItemDecoration`, không group-by-date logic nào. Chip filter `chipDateToday/Week/Month` (`FHistory.kt:310-313`) chỉ *lọc*, không *group* danh sách. |
| E3 | Custom QR size preset chip | **Đã làm một phần** | `FEncode.kt:430` `getSize(power) = 128 * (power+1)`, SeekBar `android:max="7"` (`roy_f_encode.xml:182`) → cho ra 128/256/384/512/640/768/896/1024, liên tục theo từng nấc 128px chứ không phải 4 preset chip rời rạc 128/256/512/1024 như yêu cầu. Không tìm thấy `Chip`/preset button nào cho size trong `FEncode.kt`. |
| E4 | Confetti nâng cấp | **Đã làm — bằng giải pháp thay thế** | Không có `konfetti` trong `app/build.gradle` (grep rỗng), nhưng `FVipManagement.kt:768-811` đã tự viết custom particle system (50 dot, `ObjectAnimator` fall/rotate/fade) — đúng với gợi ý thay thế "custom particle system" trong chính doc. Coi như xong về mặt UX, nhưng có bug mới (xem B1). |
| E5 | CSV export theo filter | **Đã làm phần lõi, thiếu prompt UI** | `FHistory.kt:492,521`: `db.getScansDetailed(scanFilter)` — export/share đã dùng `scanFilter` hiện tại. Không có dialog hỏi "Export filtered (N) or all?". |
| E7 | Camera zoom memory | **Đã xong hẳn** | `ActivityCamera.kt:621-630` có `saveZoom()`/`restoreZoom()` đầy đủ, dùng `Pref` key `ZOOM_LEVEL` (`:742`), gọi ở `onPause`/`onDestroy` (`:201,504`). Doc liệt kê sai là TODO. |
| E8 | Splash skip nếu mở gần đây | **CHƯA làm** | `grep LAST_FOREGROUND\|lastForeground` → 0 kết quả. |
| F3 | Scan tags | **CHƯA làm** | `grep SCANS_TAGS\|tags` trong `Db.kt` → 0. Schema version vẫn = **6** (`Db.kt:202`). |
| F4 | Geo-tag scans | **CHƯA làm** | `grep SCANS_LAT\|SCANS_LNG` → 0. `play-services-location` không có trong `build.gradle`. |
| F5 | Barcode compare tool | **CHƯA làm** | Không có file nào tên `*compare*`. |
| F7 | Scan reminder | **CHƯA làm** | Không có file `*reminder*`; `androidx.work` không có trong `build.gradle`. |
| F8 | Biometric lock history | **CHƯA làm** | `androidx.biometric` không có trong `build.gradle`. |
| F9 | Auto-action config | **CHƯA làm** | `grep AUTO_ACTION\|autoAction` → 0. `ActionRegistry.kt:31-33` vẫn `find { canExecuteOn }` tĩnh. |
| F10 | OCR → QR | **CHƯA làm** | Không có `mlkit`/`text-recognition`, không có `FOcrEncode.kt`. |

**compileSdk/targetSdk 37 impact:** Predictive-back đã xử lý đúng (`ActivityCamera.kt:176-183`, `[FIX BUG-6]`, dùng `OnBackPressedDispatcher`). Edge-to-edge: không dùng `enableEdgeToEdge()` chuẩn nhưng có mitigation thủ công `setPaddingFromWindowInsets()` (`view/WindowInsets.kt`) áp cho hầu hết root layout. Camera dùng `android.hardware.Camera` (deprecated) — intentional theo CLAUDE.md.

## 2. Bug / tech-debt MỚI

- **B1** — `FVipManagement.kt:783-809` confetti dots không huỷ animator khi `onDestroyView()` sớm, chỉ `runCatching{removeView}` sau `postDelayed` tối đa ~3s. Leak View/Animator nhẹ, nên cancel trong `onDestroyView()`.
- **B2** — `ActivitySplash.kt:59-61` CoroutineScope ẩn danh mỗi lần `runSplashAdFlow()`, không `SupervisorJob`, khác pattern chuẩn nội bộ — nhưng có cancel đúng ở `onDestroy()`, chỉ là nit style, không phải bug thật.
- **B3** — `POST_NOTIFICATIONS` permission khai báo trong manifest nhưng không có `NotificationManagerCompat`/runtime-request nào trong app code — thừa hoặc pre-declare cho F7 tương lai.
- **B4** — `view/graphics/Transparency.kt:15` `config!!` — an toàn về logic (đứng sau null-check ở dòng 9-10) nhưng nên đổi `?: return this` cho rõ ràng.

Không tìm thấy println/TODO/FIXME sót lại; Log.* còn lại đều hợp lý cho production.

## 3. Lỗ hổng test coverage

File lớn/nhiều nhánh chưa có test nào: `FVipManagement.kt` (1015 dòng), `ActivityCamera.kt` (921 dòng), `FDecode.kt` (610), `DetectorView.kt` (441), `ext/Activity.kt` (421), `FBarcode.kt` (401, `overlayLogo` là pure-logic dễ test), `WifiConnector.kt`/`WifiAction.kt` (379, parsing pure-logic dễ test), `Pref.kt` (393), `Scan.kt` (217). Ưu tiên: `WifiConnector` parsing và `FBarcode.overlayLogo` corner-radius calc dễ tách unit test JVM nhất theo convention CLAUDE.md.

## 4. Đề xuất 5 tính năng VIP mới

1. **Scan Streak / Daily Check-in Counter** — dùng `SCANS_DATETIME` có sẵn, query `COUNT(DISTINCT date(...))` liên tiếp. Free 7 ngày gần nhất, VIP full + mốc thưởng. Effort **S**.
2. **Smart Duplicate-Scan Warning** — trước khi lưu scan mới, query nội dung trùng trong N ngày gần nhất (đã có `Db.getScans(ScanFilter(query=...))`). VIP: cảnh báo không giới hạn; free 24h. Effort **S**.
3. **VIP Format Filter mở rộng** — `ScanFilter.FormatGroup` hiện chỉ 4 nhóm thô (`ScanFilter.kt:11`), thêm filter theo `BarcodeFormat` cụ thể cho VIP, tận dụng `toWhereClause()`/`toWhereArgs()` đã pure-function hoá. Effort **M**.
4. **Export Template / Scheduled Auto-Backup** — mở rộng `askToExportToFile()` đã dùng scanFilter, lưu "export profile" + `WorkManager` chạy định kỳ (cộng hưởng với F7 vì cùng cần `androidx.work`). Effort **M**.
5. **Batch QR từ CSV nhiều cột (Contact/WiFi/Event)** — nâng cấp `FBatchEncode.kt` (đã có `MAX_BATCH=200`), tái dùng `VCardAction`/`WifiAction` builder có sẵn để build QR có cấu trúc hàng loạt. Effort **L**.
