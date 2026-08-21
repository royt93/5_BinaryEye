# Product Backlog — BinaryEye / Cat Scanner

> Tổng hợp: 2026-08-21. Thay thế `doc/task.md` làm nguồn ưu tiên chính (task.md giữ lại làm lịch sử/tham chiếu implementation-plan chi tiết cho 14 item cũ).
> Phương pháp: 1 agent audit nội bộ (grep/đọc code thật, quyền Read-only) + 3 AI CLI độc lập review chéo ở chế độ **read-only/plan** (không cấp quyền sửa file — chỉ cần ý kiến, không cần edit, nên không dùng full "yolo"/"dangerously-skip-permissions"):
> - `codex exec -s read-only` → `reviews/review_codex.md`
> - `agy -p --mode plan` → `reviews/review_agy.md`
> - `claude -p --permission-mode plan` → `reviews/review_claude_instance.md`
> - `gemini -p --approval-mode plan` → **THẤT BẠI** (tài khoản free-tier Gemini Code Assist không còn được hỗ trợ, cần migrate Antigravity — bỏ qua, không phải lỗi của repo)
> - Agent nội bộ (Explore/general-purpose) → `reviews/review_internal_agent.md`
>
> **Độ tin cậy:** ✅✅✅/✅✅ = được ≥2 nguồn độc lập phát hiện trùng khớp (tin cậy cao). ✅ = 1 nguồn, đã tự verify lại bằng grep trực tiếp trước khi đưa vào đây. ⚠️ = 1 nguồn, CHƯA tự verify — kiểm tra lại trước khi bắt tay code.
> Tất cả bug ở Epic A bên dưới đã được **tự tay verify lại bằng grep/đọc code** (không chỉ tin lời AI) trước khi đưa vào doc này.

---

## 🎯 Quyết định Sprint 0 (chốt với user 2026-08-21)

| Câu hỏi | Quyết định |
|---|---|
| Ưu tiên sprint tới | **Fix bug P0/P1 (Epic A) + song song E3 + E8** (đã gần xong, effort nhỏ) |
| Rescope 14 item cũ | **Theo khuyến nghị AI** — đóng E4 (đã xong bằng giải pháp khác), hạ F4 + F7 xuống 💭 Idea, re-scope F10 → OCR→VCARD |
| Hướng tính năng độc quyền | **Cả 3**: VIP-01 (Secure Vault), VIP-02 (B2B Batch Audit Suite), FEAT-NEW-01 (VietQR/Banking Parser) — giữ làm top-pick cho Epic F, triển khai sau khi xong Sprint 0 |

**Sprint 0 — đã tách file riêng trong `doc/task/todo/`** (kanban: di chuyển sang `inprogress/` rồi `done/` khi làm):
`BUG-01` (P0 data loss) · `BUG-02` (crash API24) · `BUG-03` (NPE hashCode) · `BUG-04` (crash empty decode) · `BUG-05` (Bluetooth pre-S) · `BUG-06` (Locale 3 ngôn ngữ) · `E3` (QR size preset) · `E8` (splash skip gần đây)

BUG-07 → BUG-17 (P2/P3, phần lớn 1-dòng fix) chưa tách file — cân nhắc gộp opportunistic vào Sprint 0 vì effort rất thấp, nhưng không bắt buộc.

---

## Epic A — 🔴 P0/P1 Bug Fixes (data loss / crash / security) — làm TRƯỚC mọi việc khác

| ID | Mức | Bug | File:Line | Nguồn | Fix |
|----|-----|-----|-----------|-------|-----|
| BUG-01 | 🔴 P0 — DATA LOSS | `db.removeScans(scanFilter.query)` chỉ nhận chuỗi search, **bỏ qua hoàn toàn filter ngày/format**. User filter "Today + QR" → bấm "Clear" → nếu không có text search, `scanFilter.query == null` → xoá SẠCH toàn bộ lịch sử, không chỉ phần đang lọc. | `database/Db.kt:191`, `frm/FHistory.kt:454` | ✅✅✅ (codex + agy + claude, độc lập, đã tự verify code) | Đổi `removeScans(query)` → `removeScans(filter: ScanFilter)`, dùng `filter.toWhereClause()`/`toWhereArgs()` (đã pure-function sẵn từ fix A8). |
| BUG-02 | 🔴 P1 — CRASH minSdk | `Color.luminance(fgColor)` (`android.graphics.Color`) chỉ có từ API 26, app minSdk 24 → crash 100% trên Android 7.0/7.1 khi mở màn Encode. | `frm/FEncode.kt:294-295` | ✅ (agy, đã verify: import đúng `android.graphics.Color`, gọi `.luminance()` API26+) | Dùng `androidx.core.graphics.ColorUtils.calculateLuminance(color)` thay thế. |
| BUG-03 | 🔴 P1 — CRASH (NPE) | `Scan.hashCode()` gọi `version.hashCode()` không safe-call, trong khi `version: String?` — mọi field nullable khác trong cùng hàm đều dùng `?.hashCode() ?: 0`, chỉ riêng dòng này thiếu. | `database/Scan.kt:66` | ✅ (agy, đã verify: field nullable dòng 17, các dòng 65/70-73 xung quanh đều safe-call đúng, chỉ dòng 66 thiếu) | `(version?.hashCode() ?: 0)`. |
| BUG-04 | 🔴 P1 — CRASH | `.first()` gọi sau khi chỉ null-check danh sách kết quả decode, không check rỗng → `NoSuchElementException` khi decoder trả `emptyList()`. `ActivityPick` crash cả app vì coroutine không có `CoroutineExceptionHandler`. | `view/act/ActivityCamera.kt:572`, `view/act/ActivityPick.kt:156` | ✅✅ (codex + claude, độc lập) | `.firstOrNull() ?: return`. |
| BUG-05 | 🟠 P1 — Bluetooth vô hiệu hoá sai | `hasBluetoothPermission()` trả `false` cho mọi thiết bị < Android 12 (API 24-30), trong khi Bluetooth trước Android 12 là install-time permission (luôn được cấp). Tính năng "Send scan via Bluetooth" không bao giờ hoạt động trên 5 phiên bản Android. | `ext/app/Permissions.kt:34-42` | ✅ (agy, đã verify else-branch tồn tại đúng như mô tả) | Đổi nhánh `else` → `true`. |
| BUG-06 | 🟠 P1 — Hỏng Locale cho 3 ngôn ngữ lớn | `applyLocale()` split theo `"-"` rồi lấy phần sau làm ISO country code thẳng: `"zh-rCN".split("-")[1] = "rCN"` (còn dính tiền tố `r` kiểu Android resource-qualifier, không phải ISO 3166 hợp lệ) → `Locale("zh","rCN")` không match `values-zh-rCN` → fallback tiếng Anh. **Đã tự verify**: `FLanguageDialog.kt:32,38,39` dùng đúng value `"zh-rCN"`, `"pt-rBR"`, `"ru-rRU"`; `res/` có `values-zh-rCN`, `values-pt-rBR`, `values-ru-rRU`. | `ext/app/Locale.kt:11-16` | ✅ (agy, đã tự verify toàn bộ chuỗi bằng chứng) | Bóc tiền tố `r`: `Locale(localeParts[0], localeParts[1].removePrefix("r"))`, hoặc parse đúng BCP-47 bằng `Locale.forLanguageTag()`. |
| BUG-07 | 🟡 P2 | ZXing Intent Protocol: `putExtra("SCAN_RESULT_FORMAT", result.format)` truyền thẳng enum thay vì String — app thứ 3 gọi theo chuẩn ZXing `SCAN_RESULT_FORMAT` sẽ nhận `null`/crash khi `getStringExtra`. | `view/act/ActivityCamera.kt:901` | ✅ (agy, đã verify) | `putExtra("SCAN_RESULT_FORMAT", result.format.name)`. |
| BUG-08 | 🟡 P2 | vCard: số điện thoại thứ 3 gán sai key — cả 2 vế đều là `TERTIARY_PHONE_TYPE`, mất field giá trị số điện thoại thực (`TERTIARY_PHONE`). | `view/actions/vtype/vcard/VCardAction.kt:55-56` | ✅ (agy, đã verify) | Vế phải đổi thành `Insert.TERTIARY_PHONE`. |
| BUG-09 | 🟡 P2 | Lỗi chính tả MIME type: `"image/svg+xmg"` thay vì `"image/svg+xml"` — app nhận share/save SVG không nhận diện đúng loại file. | `frm/FBarcode.kt:292` | ✅ (agy, đã verify) | Sửa `"image/svg+xml"`. |
| BUG-10 | 🟡 P2 | CSV export: dòng dữ liệu có delimiter thừa ở cuối (append delimiter sau MỌI cột kể cả cột cuối) trong khi header dùng `joinToString` không có trailing delimiter → lệch số cột, Excel/Sheets hiện thêm 1 cột rỗng ảo. | `database/CsvExport.kt:75-90` | ✅ (codex, đã verify logic joinToString vs forEach+append) | Header/data phải cùng convention — dùng `joinToString(separator=delimiter)` cho cả 2, hoặc bỏ `sb.append(delimiter)` ở phần tử cuối. |
| BUG-11 | 🟡 P2 | Cursor leak: check `activity ?: return@withContext` **trước** khi đóng cursor khi Fragment detach giữa lúc search đang chạy — 1 trong số ít chỗ không dùng `.use{}`. | `frm/FHistory.kt:330-336` | ✅✅ (codex + claude) | Đóng cursor trong `finally`/dùng `.use{}` bất kể activity còn sống hay không. |
| BUG-12 | 🟡 P2 | Race condition tìm kiếm: mỗi keystroke tạo coroutine DB mới không debounce, không hủy request cũ → kết quả cũ có thể ghi đè kết quả mới nếu về sau. | `frm/FHistory.kt:267-270` | ✅✅ (codex + claude) | Debounce 300ms + hủy `Job` cũ trước khi launch cái mới. |
| BUG-13 | 🟡 P2 | Export DB "sống": copy trực tiếp file `.db` trong khi database đang mở, WAL chưa checkpoint → dữ liệu mới nhất có thể thiếu trong file export/backup. `onTerminate()` không đảm bảo được gọi trên thiết bị thật. | `database/Export.kt:9-13` | ✅ (codex) | Checkpoint WAL (`PRAGMA wal_checkpoint(FULL)`) trước khi copy, hoặc dùng `SQLiteDatabase.backup()`. |
| BUG-14 | 🟡 P2 | Thiếu `onDowngrade()` trong `SQLiteOpenHelper` — nếu user restore backup DB version cũ (Play "undo update"/khôi phục thiết bị) trong khi app đã chạy phiên bản mới hơn từng có schema cao hơn rồi bị downgrade app, `onUpgrade` không xử lý ngược → crash mở app. | `database/Db.kt:201-228` | ⚠️ (claude, chưa tự verify sâu — cần confirm kịch bản thực tế có xảy ra không với minSdk hiện tại) | Override `onDowngrade()` — ít nhất fallback drop & recreate table thay vì crash. |
| BUG-15 | 🟡 P2 — OOM risk | Mỗi lần kéo vùng crop (ROI) trong `ActivityPick` tạo 1 bitmap mới, không `recycle()` bitmap cũ — kéo liên tục có thể giữ nhiều bitmap 1024px cùng lúc. | `view/act/ActivityPick.kt:127` | ✅ (codex) | Recycle bitmap trước khi gán bitmap mới. |
| BUG-16 | 🟢 P3 | `versionCode 20270822` (mã hoá năm **2027**) nhưng `versionName '2026.08.22'` (năm **2026**) — lệch 1 năm, gây nhầm lẫn mọi bản build sau nếu convention là encode ngày release vào versionCode. | `app/build.gradle:63-64` | ✅ (claude, đã tự verify literal trong build.gradle) | Đồng bộ lại 1 trong 2, xác nhận với dev đâu là ngày release dự kiến thật. |
| BUG-17 | 🟢 P3 | Ad callback (`onAdShown`/`onAdDismissed`...) không check `isFinishing`/lifecycle trước khi `startActivity` — 3 chỗ. | `view/act/ActivityCamera.kt:325-338,369-375` | ⚠️ (claude, chưa tự verify) | Guard `if (!isFinishing && !isDestroyed)` trước khi navigate trong callback. |

---

## Epic B — 🔒 Bảo mật & Riêng tư (Security/Privacy debt)

| ID | Vấn đề | File | Nguồn | Ghi chú |
|----|--------|------|-------|---------|
| SEC-01 | **VIP hoàn toàn client-side, crackable**: `VIP_KEY_SECRET` + 2 redeem code (30 ngày/3 ngày) nhúng thẳng vào `BuildConfig` (string literal, R8 không obfuscate được string constant) — decompile APK bằng jadx vài giây là lấy được secret, patch VIP miễn phí vĩnh viễn cho mọi máy. | `app/build.gradle:73` (`VIP_KEY_SECRET`), `frm/FVipManagement.kt:90,439` | ✅✅ (claude + codex, độc lập, claude đã tự verify build.gradle:73-75) | Đã biết trước 1 phần (doc/AD.MD ghi "đổi secret invalidates key cũ") nhưng chưa từng được flag như **lỗ hổng bảo mật/doanh thu**. Fix triệt để cần backend verification (Play Integrity API hoặc server redeem) — cộng hưởng tốt với M1 (Play Billing). |
| SEC-02 | `allowBackup="true"` không exclude VIP status/prefs khỏi `adb backup`/auto-backup → user có thể nhân bản trạng thái VIP giữa nhiều máy hoặc restore lại VIP đã hết hạn. | `AndroidManifest.xml`, thiếu rule trong backup config | ✅ (claude, đã tự verify — cần review `backup_rules.xml`) | Cộng hưởng SEC-01. |
| SEC-03 | `backup_rules.xml` include toàn bộ root (cả lịch sử scan chứa nội dung nhạy cảm: WiFi password, thẻ tín dụng quét được, vCard) vào cloud backup mà không có exclude rule rõ ràng. | `app/src/main/res/xml/backup_rules.xml:1` | ✅ (codex) | Nên loại trừ `database/` khỏi full-backup hoặc mã hoá trước khi backup — liên quan trực tiếp tới ý tưởng VIP-01 (Secure Vault). |
| SEC-04 | `usesCleartextTraffic="true"` áp dụng cho TOÀN APP thay vì chỉ domain cần thiết (webhook HTTP tuỳ chỉnh của user). | `AndroidManifest.xml:58` | ✅ (codex) | Dùng Network Security Config theo domain, chỉ bật cleartext khi user tự cấu hình webhook HTTP. |
| SEC-05 | CSV export không escape content bắt đầu bằng `=`, `+`, `-`, `@` — mở trong Excel/Google Sheets có thể bị diễn giải thành công thức (CSV Injection / Formula Injection). Đặc biệt nguy hiểm vì đây là app scanner — content do bên ngoài (mã QR độc hại) quyết định, không phải user tự gõ. | `database/CsvExport.kt:75` | ✅ (codex) | Prefix `'` (single-quote) cho các giá trị bắt đầu bằng ký tự công thức trước khi ghi CSV. |
| SEC-06 | `ActionRegistry` dùng `Set<IAction>` cho 1 invariant bắt buộc thứ tự (WebAction phải cuối) — chỉ đúng nhờ `setOf` triển khai ngầm là `LinkedHashSet`, không compiler-safe, dễ vỡ khi ai đó đổi sang `hashSetOf`/thêm phần tử không để ý. | `view/actions/ActionRegistry.kt:17` | ✅✅ (codex + claude) | Đổi sang `List<IAction>` cho rõ ràng ý định + invariant. |

---

## Epic C — 💰 Monetization / VIP Integrity

Gộp với `M1-M6` cũ trong `doc/task.md` (IAP), bổ sung phát hiện mới:

| ID | Việc | Trạng thái | Ghi chú |
|----|------|------------|---------|
| M1 | Google Play Billing Library integration | 📋 TODO — **2/3 AI review đề xuất nâng lên P0/High** (không phải Deferred như doc cũ) | UI đã dựng 80% (`FVipManagement` hiện 4 nút gói 30 ngày/90 ngày/1 năm/lifetime nhưng **không có `onClickListener`** — verify bởi claude) — nối billing là phần lõi còn thiếu. |
| M1b (mới) | Fix SEC-01/SEC-02 (VIP crack qua BuildConfig + backup) cùng đợt với M1 | 📋 TODO — mới, chưa từng có trong task.md | Không có ý nghĩa làm Billing cho gói trả phí trong khi flow miễn phí (VIP key) đang bị lộ hoàn toàn phía client. |
| M2-M5 | VIP 30/90/365/Lifetime — enable nút, nối billing | ⏸️ phụ thuộc M1 | |
| M6 | Restore purchase | ⏸️ phụ thuộc M1 | Bắt buộc theo chính sách Play Store. |

---

## Epic D — 🔧 Enhance tính năng hiện có (refresh từ 14 item cũ)

> Trạng thái đã **verify lại bằng grep thực tế** (agent nội bộ), không tin theo doc cũ.

| ID | Feature | Trạng thái thực tế (2026-08-21) | Đồng thuận AI review | Khuyến nghị |
|----|---------|-----------------------------------|----------------------|-------------|
| E1 | Torch auto-on (light sensor) | 📋 Chưa làm (verify: 0 kết quả `TYPE_LIGHT`) | ✅✅✅ đồng ý làm, nhớ thêm debounce/hysteresis chống nhấp nháy | Giữ nguyên priority 🔴 High |
| E2 | History date group header | 📋 Chưa làm | ✅✅ đồng ý nhưng **đổi cách làm**: 1 nguồn khuyên vá `CursorAdapter` (rủi ro thấp), 1 nguồn khuyên migrate hẳn sang `RecyclerView` (đúng chuẩn hơn nhưng tốn hơn, đụng ActionMode/selection hiện tại) | Quyết định cách làm trước khi bắt tay — xem Q2 AskUserQuestion |
| E3 | Custom QR size preset chip | 🟡 Đã có SeekBar liên tục, thiếu 4 preset chip rời | ✅ đồng ý, effort nhỏ | Giữ nguyên |
| E4 | Confetti nâng cấp (konfetti lib) | ✅ **Coi như xong** — đã tự viết custom particle system thay thế, không cần thêm dependency | ✅✅ **2 nguồn khuyên KHÔNG làm** (không cần lib ngoài, ưu tiên bug thật hơn) — nhưng phát hiện B1 (leak nhẹ khi destroy sớm) cần vá | 🎯 **CHỐT: Đóng E4**, mở BUG (B1) thay thế |
| E5 | CSV export theo filter | 🟡 Phần lõi đã xong (`scanFilter` đã được dùng khi export), thiếu prompt "filtered vs all" | ✅ đồng ý, ưu tiên kèm BUG-10 (fix delimiter) | Gộp chung 1 PR với BUG-10 |
| E7 | Camera zoom memory | ✅ **Đã xong hẳn** — doc cũ liệt kê sai | ✅✅✅ xác nhận đã có | Đóng E7, chỉ cần thêm test |
| E8 | Splash skip nếu mở gần đây | 📋 Chưa làm | ✅✅ đồng ý (tăng retention) nhưng cân nhắc giảm doanh thu App Open ad | Giữ, nhưng cấu hình ngưỡng cẩn thận |

---

## Epic E — ✨ Tính năng mới (F3-F10 cũ + mới phát hiện)

| ID | Feature | Đồng thuận | Khuyến nghị hành động |
|----|---------|------------|------------------------|
| F3 | Scan Groups/Tags | ✅✅✅ đồng ý làm, VIP-gate tag không giới hạn | 1 nguồn khuyên: dùng bảng `tags` + junction table thay vì CSV trong 1 cột (chuẩn hoá hơn) — cân nhắc effort tăng thêm |
| F5 | Barcode compare | 🟡 hỗn hợp: 1 nguồn đồng ý, 2 nguồn hạ ưu tiên (niche use-case) | Hạ xuống 💭 Idea, làm sau |
| F8 | Biometric lock history | ✅✅✅ đồng ý mạnh — "tính năng đáng giá nhất để thúc đẩy VIP" | Giữ 🔴 High, gộp chung với ý tưởng VIP-01 Secure Vault (Epic F) cho trọn vẹn hơn là làm riêng lẻ |
| F9 | Auto-action config | ✅✅ đồng ý nhưng **cảnh báo an toàn**: tự mở URL/tự connect WiFi không hỏi có thể bị lợi dụng (QR độc hại tự thực thi) | Cần thêm allowlist scheme + preview lần đầu trước khi bật auto-action cho content nhạy cảm |
| F4 | Geo-tag scans | ❌❌ 2/3 nguồn khuyên **không ưu tiên** (permission burden, giảm trust cho 1 app scanner, review Play Store khắt khe) | 🎯 **CHỐT: hạ xuống 💭 Idea** (chỉ làm nếu opt-in rõ ràng + coarse location) |
| F7 | Scan reminder | ❌❌ 2/3 nguồn khuyên **không làm** (niche, cồng kềnh WorkManager, `WorkManager` không đảm bảo đúng giờ tuyệt đối) | 🎯 **CHỐT: hạ xuống 💭 Idea** |
| F10 | ~~OCR → QR~~ → **OCR → VCARD** | 🟡 đồng ý ý tưởng nhưng re-scope: bắt đầu OCR từ ảnh có sẵn (không cần live camera pipeline), đổi đích thành OCR→VCARD (parse danh thiếp có cấu trúc: tên/SĐT/email/công ty) thay vì OCR→text thô | 🎯 **CHỐT: re-scope theo khuyến nghị AI** |
| **FEAT-NEW-01** ⭐🎯 | **VietQR / EMVCo Banking QR Parser** — nhận diện QR chuyển khoản ngân hàng chuẩn NAPAS247/VietQR/EMVCo, tự tách Số tài khoản/Ngân hàng/Tên chủ thẻ/Số tiền để copy nhanh hoặc mở thẳng app ngân hàng | ⚠️ 1 nguồn (agy) nhưng đánh giá **"Business Value: Rất cao, Độ khó: Thấp-Vừa"**, rất hợp thị trường VN/ĐNA của app này | **Đề xuất flagship mới**, xem Q3 AskUserQuestion |
| FEAT-NEW-02 | GS1 Application Identifier parser (GTIN/expiry/lot/serial) | ✅✅ (codex trong new-feature + VIP list) | Nền tảng cho cả VIP-02 (B2B suite) |
| FEAT-NEW-03 | Product & Price Lookup (OpenFoodFacts/UPC DB) | ✅ (agy) | Freemium — basic free, VIP unlimited/cảnh báo dị ứng |
| FEAT-NEW-04 | Duplicate/counterfeit serial alert | ✅✅ (codex + claude, dạng B2B "Verify & Track") | Nền tảng chung với VIP-02 |
| FEAT-NEW-05 | Pagination cho History (hiện load hết 1 lần) | ✅ (claude) | Effort thấp-trung bình, ảnh hưởng hiệu năng thực tế khi lịch sử lớn |

---

## Epic F — 👑 Tính năng ĐỘC QUYỀN / VIP (tổng hợp & dedup từ 4 nguồn, ~20 ý tưởng gốc → 11 cụm)

| ID | Tên | Mô tả gộp | Đồng thuận nguồn | Effort |
|----|-----|-----------|-------------------|--------|
| VIP-01 ⭐🎯 | **Secure Vault & Incognito Scanner** | Mã hoá lịch sử bằng Android Keystore + biometric unlock (khớp F8) + chế độ "quét ẩn danh" (không lưu, tự xoá clipboard sau 60s cho nội dung nhạy cảm như WiFi/thẻ) + chống screenshot + recovery-key. Đồng thời vá SEC-01/SEC-03. | ✅✅✅ **3/4 nguồn đề xuất độc lập** (agy, claude, codex) — tín hiệu mạnh nhất trong toàn bộ review | M |
| VIP-02 ⭐🎯 | **B2B Batch Audit & Inventory Suite** | Quét liên tục đếm số lượng/SKU, so khớp expected-list, cảnh báo thiếu/thừa/trùng bằng âm thanh khác biệt, GS1 AI parser (lot/expiry/serial), cảnh báo hàng giả/trùng serial, xuất báo cáo Excel/CSV chuẩn cột. | ✅✅✅✅ **CẢ 4 NGUỒN** đều đề xuất biến thể của ý này độc lập (agy: multi-barcode inventory; claude: batch business workflow + verify&track; codex: batch audit session + GS1 inspector + inventory mode) — tín hiệu mạnh NHẤT toàn review, nhắm thị trường B2B/SME sẵn sàng trả tiền hơn cá nhân | L |
| VIP-03 | Pro QR Design Studio | Mở rộng F2 (đã xong): chọn hình mắt QR (tròn/vuông/kim cương), gradient màu, xuất Vector SVG/PDF chuẩn in ấn công nghiệp | ✅ (agy) | M |
| VIP-04 | Cloud Backup & Scheduled Export | Đồng bộ lịch sử lên Google Drive cá nhân của user + "export profile" tự động chạy định kỳ qua WorkManager | ✅✅ (agy: Drive sync; nội bộ: export template/auto-backup) | M |
| VIP-05 | Smart Dynamic QR (redirect server-managed) | QR trỏ tới redirect server, cho phép sửa đích sau khi đã in ra — mô hình SaaS, recurring revenue cao nhất trong toàn bộ ý tưởng nhưng cần hạ tầng backend | ✅ (claude) | XL (cần backend riêng) |
| VIP-06 | Authenticity Passport | App tự ký QR tạo ra, xác minh chữ ký offline, hiển thị badge "Trusted by Cat Scanner" — chống giả mạo QR | ✅ (codex) | L |
| VIP-07 | Product Intelligence & Recall Alert | Mở rộng FEAT-NEW-03: nhận diện xuất xứ qua GTIN prefix, cảnh báo hàng giả/thu hồi, điểm Nutri-Score | ✅ (agy, dạng freemium) | M |
| VIP-08 | Scan Streak / Daily Check-in | Đếm chuỗi ngày liên tiếp có scan, badge mốc thưởng | ✅ (nội bộ) | S — quick win |
| VIP-09 | Smart Duplicate-Scan Warning | Cảnh báo khi scan lại nội dung đã quét gần đây | ✅ (nội bộ) | S — quick win |
| VIP-10 | VIP Format Filter mở rộng | Filter theo `BarcodeFormat` cụ thể (không chỉ 4 nhóm thô hiện tại) | ✅ (nội bộ) | M |
| VIP-11 | Private Automation Studio | Rule engine offline: điều kiện format/prefix → tag/webhook/Bluetooth/export tự động, có log + retry queue (bản nâng cao của F9) | ✅ (codex) | L |

**🎯 CHỐT với user (2026-08-21):** cả 3 hướng **VIP-01 + VIP-02 + FEAT-NEW-01** (xem Epic E) được chọn làm top-pick, triển khai sau khi xong Sprint 0. VIP-01/VIP-02 có đồng thuận cao nhất (3-4/4 nguồn AI độc lập); FEAT-NEW-01 (VietQR) tuy chỉ 1 nguồn đề xuất nhưng được đánh giá business value cao nhất cho thị trường VN. Thứ tự làm cụ thể (VIP-01 trước hay VIP-02 trước hay VietQR trước) chưa chốt — quyết định khi bắt đầu Sprint 1, dựa trên effort thực tế lúc đó.

---

## Epic G — 🧪 Test Coverage Gaps

File lớn/logic phức tạp **chưa có test nào** (theo agent nội bộ, đã grep xác nhận):

| File | Dòng | Ghi chú |
|------|------|---------|
| `frm/FVipManagement.kt` | 1015 | VIP state, celebration, watch-ad reward flow, countdown |
| `view/act/ActivityCamera.kt` | 921 | Core scan loop, zoom/torch, ad gating, double-back |
| `frm/FDecode.kt` | 610 | Action dispatch UI sau decode |
| `view/widget/DetectorView.kt` | 441 | Vẽ ROI, `onSaveInstanceState`, `postDelayed` invalidate |
| `ext/Activity.kt` | 421 | `rateAppInApp()` và tiện ích khác |
| `frm/FBarcode.kt` | 401 | **Ưu tiên** — `overlayLogo` corner-radius là pure-logic, dễ tách unit test JVM |
| `view/actions/wifi/WifiConnector.kt` / `WifiAction.kt` | 379 | **Ưu tiên** — parsing WiFi QR là pure-logic, nhiều nhánh API level, rất đáng unit test |
| `pref/Pref.kt` | 393 | Toàn bộ typed accessor chưa test |
| `database/Scan.kt` | 217 | Model/parsing, liên quan trực tiếp BUG-03 |

Bổ sung từ review ngoài: thiếu test cho DB migration (`onUpgrade`/`onDowngrade`, liên quan BUG-14) và test contract cho thứ tự `ActionRegistry` (liên quan SEC-06).

---

## Ghi chú minh bạch quy trình

- 1 sub-agent của `agy` (trong review độc lập) tự báo cáo đã chạy `rm -rf` trên thư mục tạm `/tmp/adwrapper_src` (tự tạo để giải nén source thư viện ad đọc tham khảo) — **không đụng file nào trong repo**, tự nhận và báo cáo minh bạch trong output. Không có thiệt hại, nhưng ghi nhận lại đây theo đúng tinh thần minh bạch.
- File thô đầy đủ của từng review lưu tại `doc/task/reviews/` để truy vết khi cần (codex review gốc dài ~5300 dòng do CLI in cả reasoning trace, chỉ phần kết luận cuối được trích lưu).
- `gemini` không dùng được (lỗi tài khoản, không phải lỗi kỹ thuật review) — có thể thử lại sau khi user migrate Antigravity nếu muốn có góc nhìn thứ 4.
