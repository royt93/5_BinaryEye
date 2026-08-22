# FEAT-NEW-01 — 🏦 VietQR / EMVCo Banking QR Parser (MVP)

**Status:** ✅ DONE — unit-test đầy đủ + **live-verify thành công end-to-end** trên Samsung Galaxy S24 Ultra (Android 16), sau khi fix BUG-18 (bug pre-existing ở `ActivityPick`, không phải lỗi VietQR) — 2026-08-22.

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

### Cập nhật 2026-08-22 — root cause đã xác định, KHÔNG phải lỗi VietQR

Máy A50s mất kết nối vĩnh viễn; user cắm lại máy khác (Galaxy S24 Ultra, Android 16). Test lại từ đầu, cẩn thận hơn (force-stop hoàn toàn trước mỗi lần thử, tạo QR mới ở size 1024px, verify qua `content query` lấy đúng content URI thay vì đoán): **vẫn "Không tìm thấy mã vạch" y hệt**, trên máy hoàn toàn khác + Android version khác (16 thay vì 11) + size khác (1024px thay vì 640px).

**→ Kết luận chắc chắn: đây là bug thật, tái hiện được, KHÔNG phải do thiết bị/độ phân giải/dữ liệu VietQR.** Đã đọc source `ActivityPick.kt` để xác định phạm vi: bug nằm ở tầng `scanWithinBounds()`/`cropImageView.getBoundsRect()` (chọn vùng ảnh để decode khi chưa có ROI tùy chỉnh) — quan sát trực quan overlay tô xám (vùng sẽ decode) không che phủ trọn vẹn ảnh, để lộ dải mỏng cạnh phải/dưới nằm ngoài vùng decode. Đây là **lỗi tồn tại từ trước** trong hạ tầng `ActivityPick` dùng chung cho MỌI tính năng "Pick file" (không riêng gì VietQR), **xảy ra hoàn toàn TRƯỚC KHI** `EmvQrParser`/`VietQrParser`/`VietQrAction` có cơ hội chạy (các file này chỉ nhận input là text ĐÃ decode thành công). Đã ghi vào backlog như **BUG-18** (`doc/task/BACKLOG.md`) với đầy đủ bằng chứng loại trừ (không phải format restriction, không phải crop-handle tồn dư, không phải native lib lỗi) và khuyến nghị hướng debug tiếp (breakpoint thật trong Android Studio, không làm được qua ADB screen-scraping).

**Ý nghĩa cho FEAT-NEW-01 (lúc đó):** logic parse (`EmvQrParser`/`VietQrParser`) đã được unit-test đầy đủ và đúng chuẩn thật (11 test, payload mẫu dựng bằng script theo đúng cấu trúc TLV EMVCo/NAPAS công khai). Đường **live camera** (không qua "Pick file") không bị ảnh hưởng bởi BUG-18.

### Cập nhật 2026-08-22 (tiếp) — root cause đầy đủ + FIX + live-verify thành công

Đào sâu thêm bằng debug log trực tiếp (`Log.i` tạm trong `scanWithinBounds()`, gỡ sau khi xong) thay vì suy đoán qua screenshot: root cause thật **không phải** `cropImageView.getBoundsRect()` như nghi vấn ban đầu, mà là:

> `DetectorView.onLayout()` **tự động** gọi `setHandleToDefaultRoi()` (đặt 1 vùng crop "gợi ý" 80% từ tâm) và set `handleActive = true` **ngay khi ảnh vừa load xong** — trước cả khi user chạm màn hình. Vì vậy `detectorView.roi` KHÔNG BAO GIỜ rỗng/width<1 trong thực tế, khiến `scanWithinBounds()` luôn dùng vùng crop gợi ý (nhỏ hơn ảnh thật ~20%) để decode thay vì toàn bộ ảnh — cắt mất mép barcode, đặc biệt nghiêm trọng với QR tự generate (chiếm gần hết khung, không có margin dư).

**Fix** (`view/widget/DetectorView.kt` + `view/act/ActivityPick.kt`): thêm `DetectorView.hasUserAdjustedRoi` — chỉ `true` khi user THẬT SỰ chạm/nhả tay khỏi crop handle (`onTouchEvent` ACTION_UP), không bị set bởi vị trí "gợi ý" tự động của `onLayout()`. `ActivityPick.scanWithinBounds()` dùng cờ này: mặc định quét **toàn bộ ảnh** (`mappedRect`) cho tới khi user chủ động kéo chỉnh crop, lúc đó mới dùng `detectorView.roi` như trước.

**Live-verify trên Galaxy S24 Ultra (build đã fix):**
1. QR đơn giản "hello world test" qua Pick file → **decode đúng 100%** (trước fix: "Không tìm thấy mã vạch" với cùng ảnh, cùng máy).
2. QR VietQR mẫu (149 ký tự, dày đặc) qua Pick file → **decode đúng**, sheet kết quả hiện đúng `VietQrAction.displayText()`:
   ```
   Bank: Vietcombank
   Account number: 0123456789
   Amount: 500000 VND
   Message: Thanh toan don hang
   ```
3. Bấm nút "COPY ACCOUNT NUMBER" (icon bank riêng, đúng label) → toast **"Copied account number 0123456789"** đúng 100%.
4. Không crash trong toàn bộ phiên. 106/106 unit test pass sau fix.

Đã ghi BUG-18 trong `doc/task/BACKLOG.md` là ✅ FIXED với đầy đủ diễn giải root cause + evidence. Đã gỡ hết debug log tạm trước khi commit.

**Kết luận:** FEAT-NEW-01 verify hoàn tất end-to-end, không còn gap nào mở.

## Chưa làm (deferred, ngoài scope MVP)

- Mở thẳng app ngân hàng cụ thể qua deep link — không có chuẩn chung, effort cao/độ tin cậy thấp.
- Validate CRC (tag 63) của payload EMVCo — hiện chỉ parse, không xác minh checksum.
- Mở rộng danh sách BIN ngân hàng ngoài top 10.
- Format số tiền có dấu phân cách hàng nghìn (hiện hiển thị raw digit string).
