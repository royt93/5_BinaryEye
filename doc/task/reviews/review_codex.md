## 1. Bug / crash risk cần sửa

- **P0 – Xóa nhầm lịch sử:** UI cảnh báo xóa các scan đang lọc, nhưng `removeScans()` chỉ nhận chuỗi tìm kiếm, bỏ qua filter ngày và format. Ví dụ đang xem “Today + QR”, thao tác Clear có thể xóa toàn bộ lịch sử nếu query rỗng. [FHistory.kt:444](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FHistory.kt:444>), [Db.kt:191](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/Db.kt:191>)

- **P0 – Crash khi decoder trả danh sách rỗng:** hai nơi gọi `first()` sau khi chỉ kiểm tra nullable. `emptyList()` sẽ gây `NoSuchElementException`. [ActivityCamera.kt:572](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/act/ActivityCamera.kt:572>), [ActivityPick.kt:156](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/act/ActivityPick.kt:156>)

- **P1 – Rò bitmap/OOM khi kéo vùng crop:** mỗi lần ROI thay đổi tạo một bitmap `cropped`, nhưng không recycle sau decode. Thao tác kéo liên tục có thể tạo nhiều bitmap 1024px song song. [ActivityPick.kt:127](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/act/ActivityPick.kt:127>)

- **P1 – Race và rò Cursor khi tìm kiếm nhanh:** mỗi ký tự tạo coroutine DB mới; kết quả cũ có thể về sau và ghi đè kết quả mới. Nếu Fragment detach tại `activity ?: return`, cursor vừa mở không được đóng. [FHistory.kt:330](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FHistory.kt:330>)

- **P1 – Export DB sống có thể thiếu dữ liệu WAL:** sao chép trực tiếp `history.db` trong khi database vẫn mở; dữ liệu mới có thể nằm trong `-wal`. `Application.onTerminate()` gần như không được gọi trên thiết bị thật, nên `db.close()` không bảo đảm checkpoint. [Export.kt:9](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/Export.kt:9>), [RApp.kt:28](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/RApp.kt:28>)

- **P1 – Rủi ro doanh thu VIP:** secret và hai mã redeem được nhúng thẳng vào `BuildConfig`, rồi so sánh hoàn toàn phía client; APK decompile được và trạng thái local có thể bị patch. [build.gradle:68](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/build.gradle:68>), [FVipManagement.kt:90](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FVipManagement.kt:90>), [FVipManagement.kt:439](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FVipManagement.kt:439>)

- **P2 – CSV injection:** content bắt đầu bằng `=`, `+`, `-`, `@` vẫn được xuất nguyên văn; mở trong Excel/Sheets có thể thực thi công thức. [CsvExport.kt:75](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/CsvExport.kt:75>)

## 2. Tech debt / code quality

- `ActivityCamera` 921 dòng và `FVipManagement` 1.015 dòng đang trộn UI, camera, decode, ads, persistence và navigation; rất khó test và dễ tạo lifecycle bug.
- `ActionRegistry` dùng `Set` dù thứ tự quyết định hành vi. Nên biểu diễn rõ bằng `List` hoặc danh sách có priority; đừng phụ thuộc đặc tính triển khai của `setOf`. [ActionRegistry.kt:17](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/actions/ActionRegistry.kt:17>)
- `db` và `prefs` là mutable global singleton, không có ownership/thread contract. [RApp.kt:15](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/RApp.kt:15>)
- Các thao tác rename/delete DB vẫn chạy trực tiếp từ callback UI. [FHistory.kt:413](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FHistory.kt:413>)
- Backup đang include toàn bộ root, gồm lịch sử scan và metadata VIP; cần quyết định rõ dữ liệu nào được cloud backup. [backup_rules.xml:1](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/res/xml/backup_rules.xml:1>)
- `usesCleartextTraffic="true"` áp dụng toàn app. Nên dùng Network Security Config theo domain hoặc chỉ bật khi người dùng chủ động cấu hình webhook HTTP. [AndroidManifest.xml:58](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/AndroidManifest.xml:58>)
- Nhiều UI string hard-code tiếng Việt/Anh, trong khi app quảng bá 20+ ngôn ngữ. [Activity.kt:208](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/ext/Activity.kt:208>), [roy_bottom_sheet_vip.xml:103](</Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/res/layout/roy_bottom_sheet_vip.xml:103>)

## 3. Enhancement tính năng hiện có

- **Camera:** decode frame trên worker đơn nhất với backpressure; hỗ trợ nhiều symbol/frame; overlay confidence/format; giữ torch/zoom riêng theo camera ID.
- **History:** sort newest/oldest, custom date range, undo delete, paging, debounce search và export đúng snapshot.
- **Encode:** kiểm tra khả năng quét sau khi thêm logo/màu; cảnh báo contrast thấp; template Wi‑Fi/contact/event thay vì bắt nhập raw text.
- **VIP/ads:** Google Play Billing + backend verification hoặc Play Integrity; không thưởng VIP từ interstitial thông thường; có Restore Purchase và trạng thái offline có thời hạn.
- **Settings:** import/export cấu hình, reset theo nhóm, giải thích quyền ngay tại feature.
- **Localization:** kiểm tra parity toàn bộ resource; loại hard-code; dùng locale-aware date, giá và countdown.

## 4. Tính năng mới nên làm

- **Inventory mode:** scan số lượng, SKU, ghi chú rồi xuất CSV; giá trị kinh doanh cao, độ khó trung bình.
- **GS1 Application Identifier parser:** tách GTIN, expiry, lot, serial; rất hợp barcode chuyên nghiệp, độ khó trung bình.
- **Duplicate/counterfeit alert:** cảnh báo cùng serial xuất hiện nhiều lần hoặc sai checksum; giá trị cao, độ khó trung bình.
- **Structured-append assembler:** ghép QR/PDF417 nhiều phần thành payload hoàn chỉnh; khác biệt kỹ thuật tốt, độ khó cao.
- **Local automation rules:** điều kiện format/prefix → tag, webhook, copy hoặc cảnh báo; giá trị cao cho power user, độ khó trung bình–cao.

## 5. Tối đa 5 tính năng VIP độc quyền

1. **Secure QR Vault:** lịch sử mã hóa bằng Android Keystore, biometric unlock, auto-lock và chống screenshot.
2. **Authenticity Passport:** ký QR do app tạo và xác minh chữ ký offline, hiển thị “Trusted by Cat Scanner”.
3. **Batch Audit Session:** phiên kiểm kê có expected list, báo thiếu/thừa/trùng và xuất biên bản.
4. **Smart GS1 Inspector:** giải thích hạn dùng/lot/serial, cảnh báo hết hạn và quy tắc tùy chỉnh.
5. **Private Automation Studio:** rule engine offline cho tag/webhook/Bluetooth/export, có log và retry queue.

## 6. Đánh giá 14 item backlog

- **Đồng ý:** E1, E2, F3, F5, F8. Đây là các cải thiện có use case rõ; riêng F3 nên dùng bảng `tags` + junction table, không lưu CSV trong một cột.
- **Đồng ý nhưng hạ ưu tiên:** E3 chỉ là UX preset vì SeekBar đã làm được; E8 tốt cho retention nhưng cần cân đối doanh thu App Open.
- **Xem như đã hoàn thành:** E7 đã có `saveZoom()/restoreZoom()`; E5 đã truyền đầy đủ `scanFilter` vào export. Chỉ cần test và sửa các bug liên quan.
- **Không đồng ý:** E4 không đáng thêm dependency chỉ để thay confetti hiện có; ưu tiên crash/data/security.
- **Chỉ làm sau khi có guardrail:** F9 có thể tự mở URL độc hại hoặc tự kết nối Wi‑Fi; cần allowlist, preview lần đầu và không auto-action với payload nhạy cảm.
- **Không ưu tiên:** F4 tăng permission/privacy burden nhưng giá trị đại chúng thấp; chỉ nên bật opt-in và lưu vị trí coarse khi người dùng yêu cầu.
- **Không đồng ý với thiết kế F7 hiện tại:** WorkManager không bảo đảm notification “đúng giờ”; exact reminder phải đánh giá AlarmManager/exact-alarm policy. Bản thân use case cũng yếu.
- **Đồng ý ý tưởng, nhưng thấp:** F10 có thể hữu ích, song tăng đáng kể APK/dependency; nên bắt đầu OCR từ ảnh có sẵn thay vì thêm live camera pipeline.

## 7. Những điểm backlog bỏ sót

- P0 xóa nhầm lịch sử khi filter ngày/format.
- Crash `first()` và rò bitmap ở image scan.
- Export SQLite không bao gồm dữ liệu WAL.
- Bảo vệ doanh thu: redeem/VIP hoàn toàn phía client và chưa có purchase verification.
- Quyền riêng tư: full backup lịch sử, cleartext toàn app, CSV injection.
- Search coroutine race/cursor leak.
- Test migration DB và test contract cho thứ tự `ActionRegistry`.
- Sau targetSdk 37, cần audit đầy đủ behavior change, exported component, permission/notification và edge-to-edge thay vì chỉ dựa vào build pass.

Đánh giá này được thực hiện hoàn toàn chỉ-đọc; không có file nào được tạo, sửa hoặc xóa.
