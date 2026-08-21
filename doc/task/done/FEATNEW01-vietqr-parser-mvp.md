# FEAT-NEW-01 — 🏦 VietQR / EMVCo Banking QR Parser (MVP)

**Status:** 🟡 CODE DONE, unit-test đầy đủ, **device round-trip CHƯA verify xong** — 2026-08-21.

## Scope MVP

Nhận diện QR chuyển khoản ngân hàng chuẩn VietQR/NAPAS 247 (dựa trên EMVCo QR Code Specification for Payment Systems, Merchant Presented Mode), tách Ngân hàng/Số tài khoản/Số tiền/Nội dung để hiển thị đẹp thay vì blob EMVCo thô, và copy nhanh số tài khoản. Mở thẳng app ngân hàng cụ thể (deep link riêng từng ngân hàng) **nằm ngoài scope MVP** — không có chuẩn chung, mỗi ngân hàng 1 scheme khác nhau (nếu có), độ tin cậy thấp, effort cao không tương xứng.

## Implementation

- `view/actions/vietqr/EmvQrParser.kt` (mới) — parser TLV (Tag-Length-Value) thuần theo chuẩn EMVCo, dùng đệ quy cho các trường lồng nhau (tag 38, 62).
- `view/actions/vietqr/VietQrParser.kt` (mới) — parse chuyên biệt VietQR/NAPAS: chỉ nhận diện QR có GUID NAPAS (`A000000727`) trong tag 38 để tránh nhận nhầm QR EMVCo của mạng khác mà app không tách đúng được. Tách GTIN/BIN ngân hàng/số tài khoản (tag 38 lồng nhau), số tiền (54)/tiền tệ (53)/tên (59)/nội dung (62→08).
- `view/actions/vietqr/VietBankDirectory.kt` (mới) — BIN → tên ngân hàng, chỉ 10 ngân hàng lớn nhất VN (Vietcombank, VietinBank, BIDV, Agribank, Techcombank, ACB, MB Bank, VPBank, Sacombank, TPBank) để giảm rủi ro sai lệch dữ liệu; BIN ngoài danh sách vẫn hiển thị được (raw BIN), không chặn parse chính.
- `view/actions/vietqr/VietQrAction.kt` (mới) — `IAction`: `canExecuteOn` dùng `VietQrParser.parse() != null`; `execute()` copy số tài khoản vào clipboard + toast; `displayText()` build chuỗi nhiều dòng "Bank/Account number/Amount/Message".
- `view/actions/IAction.kt` — thêm `fun displayText(context, data): String? = null` (mặc định null = giữ nguyên hành vi cũ, hiện `scan.content` thô). Chỉ `VietQrAction` override.
- `view/act/ActivityCamera.kt` (`showScanBottomSheet`) — ưu tiên `action?.displayText(...)` thay vì `scan.content` thô khi có.
- `view/actions/ActionRegistry.kt` — đăng ký `VietQrAction` (sau `TelAction`, trước `VCardAction`).
- `res/drawable/ic_action_bank.xml` (mới) — icon "account_balance" chuẩn Material, theo đúng style `ic_action_*` có sẵn.
- `strings.xml` — `vietqr_copy_account`, `vietqr_copied_toast`, `vietqr_no_account`, `vietqr_bank_label`, `vietqr_account_label`, `vietqr_amount_label`, `vietqr_message_label`.

## Test

- Unit: `EmvQrParserTest.kt` (6 test) — TLV parse cơ bản, nhiều trường liên tiếp, chuỗi rỗng, trường bị cắt cụt (không crash), độ dài không phải số (không crash), TLV lồng nhau parse lại được.
- Unit: `VietQrParserTest.kt` (5 test) — payload VietQR mẫu đầy đủ (dựng bằng script Python theo đúng cấu trúc TLV thật, xác nhận từng field), nội dung không phải EMVCo, EMVCo nhưng khác GUID NAPAS (trả null đúng), payload sai payload-format-indicator, BIN không nằm trong danh sách (vẫn expose BIN, bankName null).
- Compile + **98/98 unit/widget test pass** (không regression, gồm cả các test VIP-02/GS1/audit từ trước).

### Device verification — CHƯA hoàn tất

Cố gắng test round-trip thật trên Galaxy A50s: dùng chính app's Encode feature tạo QR chứa payload VietQR mẫu (bracketed thủ công quá phức tạp nên build lại đúng payload TLV thật bằng Python, nhập qua `adb shell input text`), lưu PNG vào Downloads, decode lại bằng ActivityPick (gọi thẳng qua `am start -a android.intent.action.VIEW -d content://media/... -n .../.view.act.ActivityPick` vì UI picker hệ thống không phản hồi tap ổn định qua adb sau nhiều lần thử).

**Kết quả:** ActivityPick load đúng ảnh QR đã lưu (hiển thị đúng, không crash), nhưng bấm "Scan code" trả về **"No barcode found"** — ZXing không decode được ảnh QR tĩnh này, dù pattern nhìn hợp lệ bằng mắt (finder pattern rõ, đủ quiet zone). Đây là lỗi ở tầng decode ảnh của ZXing/ActivityPick — **xảy ra TRƯỚC KHI code VietQR của tôi có cơ hội chạy** (VietQrParser chỉ nhận input là text đã decode thành công), nên không phản ánh đúng/sai của logic VietQR. Đang định thử tạo lại QR với kích thước 1024px thì **thiết bị mất kết nối USB hoàn toàn** giữa chừng, không tự kết nối lại sau nhiều lần retry — dừng lại ở đây.

**Chưa xác định được nguyên nhân "No barcode found"** — có thể là:
- Lỗi/giới hạn có sẵn trong luồng ActivityPick với ảnh PNG do chính app tạo ra (chưa từng test round-trip encode→pick trước đây trong session này).
- Vấn đề độ phân giải/kích thước ảnh (640×640 mặc định) khi decode qua đường pick-from-file (khác pipeline với camera frame trực tiếp).
- Không liên quan gì đến `EmvQrParser`/`VietQrParser`/`VietQrAction` — các file này chưa từng được thực thi trong lần test này vì luồng chưa bao giờ tới bước gọi `ActionRegistry.getAction()`.

**Khuyến nghị:** khi thiết bị kết nối lại, thử lại với QR 1024px hoặc quét bằng **QR VietQR thật** (ví dụ generate từ web VietQR.io hoặc quét QR chuyển khoản thật trên hoá đơn/POS) qua camera trực tiếp — đây là con đường gần với use case thật nhất và tránh hẳn nghi vấn về pipeline pick-from-file.

## Chưa làm (deferred, ngoài scope MVP)

- Mở thẳng app ngân hàng cụ thể qua deep link — không có chuẩn chung, effort cao/độ tin cậy thấp.
- Validate CRC (tag 63) của payload EMVCo — hiện chỉ parse, không xác minh checksum.
- Mở rộng danh sách BIN ngân hàng ngoài top 10.
- Format số tiền có dấu phân cách hàng nghìn (hiện hiển thị raw digit string).
