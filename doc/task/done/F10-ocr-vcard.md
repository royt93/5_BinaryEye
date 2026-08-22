# F10 — 📇 OCR → VCARD (re-scoped)

**Status:** 🟡 CODE DONE, unit-test đầy đủ, **device verify CHƯA làm** (thiết bị mất kết nối USB) — 2026-08-22.

## Scope (re-scope theo khuyến nghị AI, đã chốt trước đó)

Ban đầu là "OCR → QR", re-scope thành **OCR → VCARD**: OCR ảnh danh thiếp *có sẵn* (không cần pipeline camera trực tiếp), tách các trường có cấu trúc (tên/SĐT/email/công ty/chức danh) thay vì chỉ trả về text thô, cho sửa tay trước khi thêm vào Danh bạ.

## Implementation

- `view/actions/vtype/vcard/BusinessCardParser.kt` (mới) — pure logic, không phụ thuộc Android/ML Kit:
  - `parse(lines: List<String>): ParsedCard` — heuristic: email theo regex chuẩn, số điện thoại theo regex (≥7 chữ số liền mạch cho phép khoảng trắng/dấu ngoặc/gạch ngang), công ty/chức danh theo danh sách từ khoá (VN + EN), phần còn lại dòng đầu tiên = tên.
  - `toVCard(card): String` — build vCard 3.0 text, **tương thích trực tiếp với `VTypeParser`/`VCardAction` đã có sẵn trong app** (không viết lại logic thêm-vào-danh-bạ).
- `view/act/ActivityOcrCard.kt` (mới) — Activity nội bộ (không có intent-filter public):
  - Nhận `Uri` ảnh qua `intent.data`.
  - Chạy ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.1`, on-device, Latin script) trong coroutine (`suspendCancellableCoroutine` bọc callback Task-based API).
  - Parse kết quả qua `BusinessCardParser.parse()`.
  - Hiện `AlertDialog` với 5 `EditText` (Name/Phone/Email/Company/Title) đã điền sẵn, cho sửa tay.
  - Nút "Add to contacts" → build vCard từ giá trị đã sửa → gọi thẳng `VCardAction.execute()` (tái dùng nguyên luồng insert-contact có sẵn).
- `view/act/ActivityCamera.kt` — thêm menu item `ocrCard` trong overflow, dùng `ACTION_GET_CONTENT image/*` (y hệt pattern `pickFile` có sẵn) → route kết quả sang `ActivityOcrCard` qua request code riêng (`PICK_OCR_RESULT_CODE`).
- `res/menu/menu_a_camera.xml` — thêm `ocrCard` (tái dùng icon `ic_action_vcard` có sẵn, không tạo icon mới).
- `AndroidManifest.xml` — khai báo `ActivityOcrCard` (`exported="false"`, chỉ launch nội bộ qua Intent tường minh).
- `strings.xml` — `ocr_business_card`, `ocr_failed`, `ocr_card_*` (dialog title/hint từng trường).
- `app/build.gradle` — thêm `com.google.mlkit:text-recognition:16.0.1` (offline/on-device, không cần Firebase/google-services.json).

## Giới hạn MVP đã biết

- Chỉ lấy **1 số điện thoại + 1 email đầu tiên** tìm được (nếu danh thiếp có nhiều số/nhiều email, các số/email còn lại bị bỏ qua) — giữ UI đơn giản, đủ cho phần lớn danh thiếp thực tế.
- Heuristic nhận diện công ty/chức danh dựa trên danh sách từ khoá cố định (VN + EN phổ biến) — không hoàn hảo, nhưng người dùng sửa tay được trước khi lưu nên chấp nhận được cho MVP.
- ML Kit Text Recognition dùng model Latin mặc định — nhận tiếng Việt có dấu ở mức "tạm ổn" (không có model tiếng Việt chuyên biệt), giới hạn đã biết của ML Kit, không phải bug của code này.

## Test

- Unit: `BusinessCardParserTest.kt` (8 test) — danh thiếp điển hình đủ trường, nhiều số/nhiều email, dòng rỗng bị bỏ qua, không có dữ liệu cấu trúc (tên = dòng đầu), input rỗng, chuỗi số ngắn không bị nhận nhầm SĐT, round-trip `toVCard()` → `VTypeParser` (xác nhận tương thích 100% với luồng insert-contact có sẵn), các trường rỗng không xuất hiện trong vCard.
- Compile `assembleDevDebug` thành công (xác nhận ML Kit tích hợp đúng ở tầng manifest-merge/dexing, không chỉ compile Kotlin).
- **106/106 unit/widget test pass** toàn bộ project (không regression).

### Device verify — CHƯA làm

Thiết bị Galaxy A50s mất kết nối USB hoàn toàn trong lúc test FEAT-NEW-01 (VietQR) ngay trước đó, chưa kết nối lại được để test tính năng này. Cần khi máy nối lại:
1. Test ảnh danh thiếp thật (chụp/tìm ảnh có tên/SĐT/email/công ty) qua menu "Scan business card (OCR)".
2. Xác nhận ML Kit tải model lần đầu qua Google Play Services thành công (cần mạng — không phải lỗi nếu chưa từng dùng ML Kit trên máy này trước đó).
3. Xác nhận dialog hiện đúng field đã tách, sửa tay được, "Add to contacts" thêm đúng vào Danh bạ Android thật.
4. Test ảnh không có text/text không đọc được → không crash, hiện toast lỗi đúng.

## Chưa làm (deferred, ngoài scope MVP)

- Model tiếng Việt chuyên biệt cho OCR (nếu ML Kit Latin không đủ tốt trong thực tế — cần test thật với danh thiếp VN mới đánh giá được).
- Chọn nhiều số điện thoại/email khi danh thiếp có nhiều hơn 1.
- Chụp trực tiếp từ camera thay vì chỉ pick ảnh có sẵn (bị loại khỏi scope từ đầu theo quyết định re-scope).
