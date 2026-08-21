# VIP-02 — 👑 B2B Batch Audit & Inventory Suite (MVP)

**Status:** ✅ DONE (MVP) — 2026-08-21, live-verify đầy đủ trên Samsung Galaxy A50s (trừ luồng quét camera thật).

## Scope MVP đã làm

Đồng thuận 4/4 nguồn AI đề xuất biến thể của "batch audit/inventory suite" — MVP tập trung vào phần lõi ai cũng nhắc tới: quét liên tục đếm số lượng + so khớp expected-list + cảnh báo bằng âm thanh + xuất báo cáo. **Không** làm GS1 AI parser (lot/expiry/serial) hay cảnh báo trùng serial *xuyên nhiều phiên* trong đợt này — đó là 2 nhánh mở rộng riêng, effort lớn hơn nhiều so với phần lõi.

## Implementation

- `view/audit/AuditSession.kt` (mới) — pure logic, không phụ thuộc Android framework:
  - `recordScan(content, format)` → `Outcome` (NEW_EXPECTED/NEW_UNEXPECTED/DUPLICATE).
  - `rows()`/`missingRows()`/`toCsv()` cho báo cáo.
  - Nếu không có expected-list → chế độ "chỉ đếm" (mọi mã lần đầu = OK, quét lại = DUPLICATE, không có khái niệm UNEXPECTED).
- `view/media/Beeps.kt` — thêm `beepDuplicate()` (tone `TONE_PROP_BEEP2`, rõ ràng khác `beepConfirm()`/`beepError()`).
- `view/act/ActivityCamera.kt`:
  - Field `auditSession: AuditSession?` — null nghĩa là không đang audit.
  - `postResult()`: nếu `auditSession != null` → chặn hoàn toàn luồng action-dispatch/history bình thường, gọi `handleAuditScan()` thay thế (đếm + tone + tiếp tục quét ngay, KHÔNG lưu history, KHÔNG mở bottom sheet).
  - `R.id.auditMode` trong overflow menu (checkable) — VIP-gate qua `AdManager.isVipByKeyActive()`, chưa VIP thì toast + không cho bật.
  - `showAuditStartDialog()` — dialog nhập expected-list nhiều dòng (tuỳ chọn, để trống = chế độ chỉ đếm).
  - `auditBadge` (Chip, anchor dưới toolbar) — live-update "N scanned · M unexpected", tap mở summary sheet.
  - `showAuditSummarySheet()` — bottom sheet: thống kê đầy đủ (kể cả số mã ky vọng chưa quét), danh sách dòng, nút Export/End Session.
  - `exportAuditReport()` — tái dùng `askForFileName`/`writeExternalFile` pattern có sẵn từ History export.
  - `confirmEndAuditSession()` — hỏi xác nhận trước khi xoá phiên (tránh mất dữ liệu chưa export).
- `database/CsvExport.kt` — bump `quoteAndEscape()` lên `internal` để `AuditSession.toCsv()` tái dùng, tránh trùng lặp logic escape.
- `res/layout/roy_a_camera.xml` — thêm `auditBadge` (Chip, anchor vào toolbar).
- `res/layout/roy_bottom_sheet_audit_summary.xml` (mới) — theo đúng style bottom sheet có sẵn (`roy_bg_bottom_sheet`, drag handle, chữ trắng).
- `res/menu/menu_a_camera.xml` — thêm `auditMode` checkable.
- `strings.xml` — toàn bộ string audit_* mới.

## Test

- Unit: `AuditSessionTest.kt` (12 test) — mọi nhánh outcome (NEW_EXPECTED/NEW_UNEXPECTED/DUPLICATE), missing-codes, CSV format, thứ tự insertion. Logic cốt lõi hoàn toàn pure nên độ tin cậy cao.
- Compile + 72 unit/widget test pass (không regression).

### Live-verify trên Samsung Galaxy A50s (2026-08-21)

Pixel 7 Pro mất kết nối USB giữa chừng phiên làm việc — chuyển sang Galaxy A50s (Android 11) đang cắm sẵn, theo xác nhận của user. Kích hoạt VIP giả bằng kỹ thuật đã dùng cho F8 (set `keyVipByKeyUntil` trực tiếp vào `shared_prefs/loitp_admob.xml` qua `run-as`, không cần mạng/gõ tay).

Đã verify **toàn bộ luồng UI end-to-end, không crash**:
1. Menu overflow hiện đúng "Batch audit (VIP)" checkable.
2. Chưa VIP → tap → toast "Batch audit is a VIP-exclusive feature" đúng, không cho bật.
3. Có VIP → tap → dialog "Start batch audit" hiện đúng title/message/hint, nhập 3 dòng "SKU001/SKU002/SKU003" — EditText nhiều dòng hoạt động chính xác.
4. Tap START → session tạo thành công, `auditBadge` xuất hiện đúng vị trí (anchor dưới toolbar) với text "0 scanned · 0 unexpected".
5. Tap badge → bottom sheet "Audit session" mở đúng, thống kê chính xác **"0 scanned · 0 unexpected · 3 expected code(s) not yet scanned"** — khớp 100% với 3 mã vừa nhập chưa quét lần nào.
6. Tap "EXPORT REPORT" → dialog "Save as in Downloads file?" (dùng lại đúng pattern export của History) → nhập tên file → OK → toast "Saved in downloads".
7. **Kéo file thật về và verify nội dung CSV khớp 100% kỳ vọng:**
   ```
   content,format,count,status
   "SKU001","",0,"MISSING"
   "SKU002","",0,"MISSING"
   "SKU003","",0,"MISSING"
   ```
8. Tap "END SESSION" → dialog xác nhận "End this audit session? Unexported data will be lost." hiện đúng → xác nhận → session xoá, badge biến mất, quay lại camera bình thường.
9. Logcat sạch suốt phiên test, không FATAL/AndroidRuntime exception.
10. Đã dọn sạch state test (xoá file CSV test, revert VIP giả) trước khi kết thúc.

### Còn lại chưa verify

- **`handleAuditScan()` (nhánh xử lý khi quét camera thật) CHƯA được kích hoạt qua 1 lần quét thật** — không có mã vạch vật lý trong phiên test. Mọi thứ TRƯỚC và SAU bước quét (tạo session, badge, summary, export, end session) đã verify đầy đủ; chỉ riêng đường đi cụ thể "camera decode → `postResult()` → `handleAuditScan()` → tone/badge update" là suy luận từ code review + unit test của `AuditSession.recordScan()` (đã test kỹ), chưa tận mắt xác nhận tone phát đúng và badge tăng đúng số khi quét vật lý.
- Không test trên thiết bị Pixel 7 Pro (mất kết nối giữa chừng) — chỉ verify trên Galaxy A50s (Android 11, API 30). Các API dùng (Chip, BottomSheetDialog, MaterialButton) đều là AndroidX/Material chuẩn, rủi ro khác biệt giữa 2 máy thấp.

**Khuyến nghị:** quét thử 2-3 mã vạch thật (1 mã trùng, 1 mã ngoài danh sách nếu có expected-list) để nghe đúng 3 loại tone và xác nhận badge/summary cập nhật đúng theo thời gian thực.

## Audit code sau khi ship (2026-08-21) — 2 bug thật tìm được + đã fix

Tự review lại toàn bộ diff VIP-02 (không phải chỉ nhìn lại test), so sánh với các quy ước sẵn có trong codebase:

1. **Beep bỏ qua `prefs.beep`/silent mode** — `handleAuditScan()` gọi thẳng `beepConfirm()/beepError()/beepDuplicate()` không qua điều kiện `prefs.beep && !isSilent()` mà `scanFeedback()`/`errorFeedback()` (dùng cho mọi luồng quét khác trong app) đều tuân thủ. Hậu quả: user tắt Beep trong Settings, hoặc để điện thoại chế độ im lặng/rung — audit mode vẫn phát tiếng mọi lần quét, sai quy ước toàn app. **Fix:** bọc khối `when(outcome)` bằng `if (prefs.beep && !isSilent())`; bump `isSilent()` từ `private` lên `internal` trong `ScanFeedback.kt` để tái dùng được.
2. **Mất dữ liệu audit khi Activity bị tạo lại (xoay màn hình)** — `auditSession` là `var` trong bộ nhớ, `ActivityCamera` không khoá orientation và `onSaveInstanceState`/`onRestoreInstanceState` cũ chỉ lưu zoom/frontFacing/bulkMode/restrictFormat, không có audit session. Xoay máy giữa phiên kiểm kê → mất sạch số đã quét + danh sách expected, không cảnh báo. **Fix:** thêm `AuditSession.snapshotEntries()`/`restoreEntries()` + `expectedCodesList`, lưu/khôi phục qua `onSaveInstanceState`/`onRestoreInstanceState` (content/format/count dạng 3 mảng song song trong Bundle). Thêm unit test `snapshotAndRestore_reproducesSameCountsAndOutcomes` (73 test total, pass).

**Live-verify fix #2 trên Galaxy A50s:** bật VIP giả (kỹ thuật cũ), start audit với expected-list "A001/A002/A003", ép Activity bị huỷ+tạo lại thật (tắt auto-rotate, đổi `user_rotation` sang landscape rồi portrait — log xác nhận camera đóng/mở lại, tức Activity thật sự recreate chứ không chỉ resize) → mở lại summary sheet, thấy đúng **"0 scanned · 0 unexpected · 3 expected code(s) not yet scanned"** — khớp 100% trạng thái trước khi xoay. Không crash, logcat sạch trong toàn bộ phiên test.

**Còn lại chưa verify (không đổi so với trước):** `handleAuditScan()` qua 1 lần quét camera thật — không có mã vạch vật lý sẵn có trong phiên test này nữa, giữ nguyên là gap đã biết.

## Chưa làm (deferred, ngoài scope MVP)

- **GS1 Application Identifier parser** (lot/expiry/serial) — cần thêm logic parse riêng cho barcode chuẩn GS1-128/DataMatrix, effort M-L độc lập.
- **Cảnh báo trùng serial xuyên nhiều phiên** — hiện tại dedupe chỉ trong phạm vi 1 `AuditSession` (session-scoped, in-memory). Muốn cảnh báo "serial này đã audit ở phiên trước" cần lưu lịch sử audit xuống DB, effort tăng đáng kể (schema mới + migration).
- Export chỉ có CSV, chưa có Excel (.xlsx) thật — CSV mở được bằng Excel/Sheets nên coi là đủ cho MVP.
