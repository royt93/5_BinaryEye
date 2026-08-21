Dưới đây là báo cáo **Đánh giá & Review chéo độc lập (Second Opinion)** toàn diện cho project **BinaryEye / Cat Scanner** (`com.mckimquyen.binaryeye`).

---

# BÁO CÁO REVIEW CHÉO ĐỘC LẬP (CODE REVIEW & STRATEGIC ROADMAP)

---

## 1. Bug / Crash Risk Cần Fix Ngay (Kèm File:Line Cụ Thể)

### 🔴 [CRITICAL CRASH] `NoSuchMethodError` trên Android 7.0 & 7.1 (API 24 & 25)
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FEncode.kt#L294-L295`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FEncode.kt#L294-L295)
* **Chi tiết:** Gọi trực tiếp `Color.luminance(fgColor)` và `Color.luminance(bgColor)`. Phương thức này chỉ được thêm từ **API level 26** (Android 8.0). Trong khi `minSdkVersion` của app là **24**.
* **Hậu quả:** 100% crash ngay lập tức khi người dùng chạy Android 7.0/7.1 mở màn hình Tạo mã (`FEncode`).
* **Khắc phục:** Dùng `androidx.core.graphics.ColorUtils.calculateLuminance(color)`.

---

### 🔴 [CRITICAL CRASH] `NullPointerException` khi gọi `Scan.hashCode()`
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/database/Scan.kt#L66`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/Scan.kt#L66)
* **Chi tiết:** Trường `version` là kiểu nullable (`String?`), nhưng trong `hashCode()` lại gọi `result = 31 * result + version.hashCode()` mà không dùng safe-call `?.` hay fallback `0`.
* **Hậu quả:** Ném `NullPointerException` khi đối tượng `Scan` có `version == null` được đưa vào `HashSet`, `HashMap`, `Set`, hoặc so sánh data class.
* **Khắc phục:** Sửa thành `(version?.hashCode() ?: 0)`.

---

### 🔴 [CRITICAL DATA LOSS] Xóa nhầm toàn bộ lịch sử khi đang lọc danh sách
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FHistory.kt#L454`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FHistory.kt#L454) & [`app/src/main/kotlin/com/mckimquyen/binaryeye/database/Db.kt#L191`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/Db.kt#L191)
* **Chi tiết:** `askToRemoveScans()` gọi `db.removeScans(scanFilter.query)`. Phương thức `removeScans` chỉ nhận `query` dạng chuỗi và bỏ qua hoàn toàn `dateRange` / `formatGroup`. Khi người dùng lọc theo ngày (ví dụ: `TODAY`) và bấm "Xóa danh sách", `scanFilter.query` là `null`, dẫn đến việc `db.delete(SCANS, "", null)` **xóa sạch toàn bộ bảng lịch sử của người dùng**.
* **Khắc phục:** Cập nhật `Db.removeScans(filter: ScanFilter)` sử dụng `filter.toWhereClause()` và `filter.toWhereArgs()`.

---

### 🟠 [BUG] Bluetooth bị tê liệt hoàn toàn trên Android 7 đến Android 11 (API 24–30)
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/ext/app/Permissions.kt#L34-L40`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/ext/app/Permissions.kt#L34-L40)
* **Chi tiết:** 
  ```kotlin
  fun Activity.hasBluetoothPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      hasPermission(Manifest.permission.BLUETOOTH_CONNECT, PERMISSION_BLUETOOTH)
  } else {
      false // <-- BUG NẰM Ở ĐÂY
  }
  ```
* **Hậu quả:** Trên Android < 12, quyền Bluetooth là quyền cài đặt (install-time permission). Việc trả về `false` khiến tính năng gửi mã scan qua Bluetooth (`sendBluetoothAsync`) không bao giờ được kích hoạt trên Android 7–11.
* **Khắc phục:** Trả về `true` trong nhánh `else`.

---

### 🟠 [BUG] Vi phạm ZXing Intent Protocol chuẩn
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/view/act/ActivityCamera.kt#L901`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/act/ActivityCamera.kt#L901)
* **Chi tiết:** `putExtra("SCAN_RESULT_FORMAT", result.format)` truyền trực tiếp đối tượng Enum `BarcodeFormat` thay vì `String`.
* **Hậu quả:** Ứng dụng bên thứ 3 gọi Cat Scanner qua Intent `com.google.zxing.client.android.SCAN` theo chuẩn ZXing sẽ nhận được `null` hoặc bị crash khi gọi `intent.getStringExtra("SCAN_RESULT_FORMAT")`.
* **Khắc phục:** Sửa thành `putExtra("SCAN_RESULT_FORMAT", result.format.name)`.

---

### 🟠 [BUG] Gán sai key số điện thoại thứ 3 trong vCard
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/view/actions/vtype/vcard/VCardAction.kt#L55-L56`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/actions/vtype/vcard/VCardAction.kt#L55-L56)
* **Chi tiết:** `2 -> ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE to ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE`.
* **Hậu quả:** Key nhận giá trị số điện thoại bị trỏ nhầm vào `TERTIARY_PHONE_TYPE` thay vì `TERTIARY_PHONE`, khiến số điện thoại thứ 3 không được lưu vào danh bạ và đè mất thông tin loại số.
* **Khắc phục:** Sửa thành `Insert.TERTIARY_PHONE_TYPE to ContactsContract.Intents.Insert.TERTIARY_PHONE`.

---

### 🟡 [BUG] Sai MIME type file SVG khi lưu và chia sẻ
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FBarcode.kt#L292`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FBarcode.kt#L292)
* **Chi tiết:** Khai báo sai chính tả `private const val MIME_SVG = "image/svg+xmg"`.
* **Hậu quả:** Các app mở file / trình duyệt không nhận diện được file SVG chia sẻ từ app.
* **Khắc phục:** Sửa thành `"image/svg+xml"`.

---

### 🟡 [BUG] Lỗi cú pháp CSV Export (Thừa Delimiter cuối dòng)
* **Vị trí:** [`app/src/main/kotlin/com/mckimquyen/binaryeye/database/CsvExport.kt#L88`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/CsvExport.kt#L88)
* **Chi tiết:** Trong vòng lặp `indices.forEach`, mỗi cột đều bị append thêm `delimiter`, kể cả cột cuối cùng trước `\n`. Trong khi dòng header (L52) dùng `joinToString(separator=delimiter)`.
* **Hậu quả:** Dòng dữ liệu luôn có $N$ dấu phẩy trong khi header chỉ có $N-1$ dấu phẩy, tạo ra 1 cột trống ảo ở cuối khi mở bằng Excel/Google Sheets.

---

## 2. Tech Debt & Code Quality Cần Cải Thiện (Kèm File:Line)

1. **Sử dụng API Camera cũ (`android.hardware.Camera`)**
   * [`ActivityCamera.kt:9, 263, 497, 610`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/act/ActivityCamera.kt#L9): API `android.hardware.Camera` đã bị Android deprecate từ API 21. Nó gặp vấn đề về autofocus và điều khiển flash trên các dòng máy mới chạy Android 14/15/16.
   * *Đề xuất:* Lên lộ trình chuyển dần sang **CameraX** (hỗ trợ ImageAnalysis streaming thẳng vào zxing-cpp).

2. **Hardcode đường dẫn Database và rủi ro backup file WAL**
   * [`Export.kt:10-13`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/database/Export.kt#L10-L13): Hardcode `File(Environment.getDataDirectory(), "//data//${packageName}//databases//${Db.FILE_NAME}")`. Đường dẫn này không chuẩn trên multi-user hoặc work profile.
   * *Đề xuất:* Dùng `context.getDatabasePath(Db.FILE_NAME)` và thực hiện `WAL checkpoint` trước khi copy file.

3. **Giao diện History và Batch dùng View truyền thống không tái sử dụng**
   * [`FHistory.kt:49`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FHistory.kt#L49), [`ScansAdapter.kt:18`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/adapter/ScansAdapter.kt#L18): Vẫn dùng `ListView` + `CursorAdapter`. Khó mở rộng header phân nhóm, animation vuốt xóa.
   * [`FBatchEncode.kt:128-136`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FBatchEncode.kt#L128-L136): Nhồi 200 `ImageView` chứa Bitmap 256x256 vào một `LinearLayout` nằm trong `ScrollView` gây ngốn ~50MB RAM và giật lag Main thread.
   * *Đề xuất:* Chuyển sang `RecyclerView` với `ListAdapter` và `DiffUtil`.

4. **Global Mutable State & Nguy cơ Memory Leak**
   * [`Permissions.kt:10`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/ext/app/Permissions.kt#L10): `var permissionGrantedCallback: (() -> Any)? = null` là biến top-level giữ lambda tham chiếu Activity. Khi xoay màn hình (recreate), callback này giữ chặt Activity cũ gây memory leak và mất sự kiện.
   * [`WifiAction.kt:14`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/view/actions/wifi/WifiAction.kt#L14): `var password: String? = null` nằm trong `object WifiAction` là singleton, có thể bị ghi đè ngoài ý muốn hoặc giữ mật khẩu trong RAM.

5. **Phân bổ bộ nhớ lãng phí khi cuộn danh sách**
   * [`ScansAdapter.kt:147`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/adapter/ScansAdapter.kt#L147): Khởi tạo lại `SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)` ở mỗi lần `bindView`. Nên tái sử dụng instance hoặc dùng format tĩnh.

6. **Anti-pattern tắt app bằng Process Exit**
   * [`FPreferences.kt:217`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260427_BinaryEye/app/src/main/kotlin/com/mckimquyen/binaryeye/frm/FPreferences.kt#L217): Gọi `Runtime.getRuntime().exit(0)` khi đổi ngôn ngữ.
   * *Đề xuất:* Dùng chuẩn `LocaleManagerCompat.setApplicationLocales()` (Android 13+) và `recreate()`.

---

## 3. Enhancement Cho Tính Năng Hiện Có

* **Scan Camera:**
  * *Auto-Torch thông minh:* Tích hợp cảm biến `Sensor.TYPE_LIGHT` với thuật toán debounce/hysteresis (bật khi lux < 10 kéo dài 1.5s, tự tắt khi lux > 50).
  * *Haptic Feedback sắc nét:* Bổ sung phản hồi rung kiểu `VibrationEffect.EFFECT_CLICK` khi máy ảnh bắt trúng barcode giúp cảm giác quét nhạy hơn.
* **History:**
  * *Swipe-to-delete & Quick actions:* Vuốt sang trái để xóa (kèm nút Undo Snackbar), vuốt sang phải để share/copy nhanh.
  * *Ghi nhớ bộ lọc & Tối ưu xuất CSV/JSON:* Hiển thị rõ số lượng bản ghi thỏa mãn bộ lọc trước khi export.
* **Encode QR/Barcode:**
  * *Mẫu QR thông minh (Smart Templates):* Thêm form nhập liệu trực quan cho Wi-Fi (tự điền SSID hiện tại), vCard Danh bạ, Sự kiện Lịch, Email, Tọa độ GPS.
  * *Xuất Vector độ phân giải cao:* Cho phép xuất ảnh PNG chất lượng in ấn (1024px, 2048px) và xuất file PDF chuẩn để in tem nhãn.
* **VIP & Ads:**
  * *Tắt quảng cáo tức thì:* Khi kích hoạt VIP, ẩn ngay banner trên màn hình thông qua Event flow mà không cần người dùng thoát app vào lại.
  * *Streak quà tặng xem quảng cáo:* Xem quảng cáo nhận 3 ngày VIP liên tục 3 lần thì tặng thêm bonus 7 ngày để khuyến khích xem rewarded ads.
* **Settings & Localization:**
  * Sử dụng chuẩn Per-App Language của Android 13+ thay vì kill tiến trình.

---

## 4. Đề Xuất Tính Năng Mới (Business Value + Độ Khó Kỹ Thuật)

| Tính năng mới | Mô tả & Giá trị | Business Value | Độ khó |
|---|---|---|---|
| **VietQR / EMVCo Banking Parser & Generator** | Nhận diện mã QR chuyển khoản ngân hàng (chuẩn NAPAS247/VietQR/EMVCo), tự tách thông tin Số tài khoản, Ngân hàng nhận, Tên chủ thẻ, Số tiền để copy nhanh hoặc mở app ngân hàng. Rất mạnh ở thị trường VN/Đông Nam Á. | 🔴 **Rất cao** | 🟡 **Thấp - Vừa** |
| **OCR Text-to-QR (ML Kit)** | Dùng Google ML Kit trích xuất chữ viết/số điện thoại từ văn bản, biển hiệu, danh thiếp rồi chuyển thành QR Code chỉ bằng 1 chạm. | 🟠 **Cao** | 🟠 **Vừa** |
| **Tra cứu mã vạch sản phẩm (Product & Price Lookup)** | Quét mã EAN/UPC sản phẩm để tra cứu xuất xứ, giá tham khảo, thành phần qua OpenFoodFacts / UPC Database mở. | 🟠 **Cao** | 🟡 **Thấp** |
| **Chế độ kiểm kê hàng hóa liên tục (Inventory Batch Count)** | Quét mã vạch liên tục và tự động tăng số lượng đếm (Counter), cảnh báo mã trùng bằng âm thanh khác biệt, xuất báo cáo kho hàng ra Excel. | 🟠 **Cao** (Khách B2B/Shop) | 🟠 **Vừa** |
| **Widget & Quick Tile nâng cao** | Mở nhanh camera quét hoặc hiển thị trực tiếp mã thẻ thành viên/QR WiFi ưa thích ngay ngoài Home Screen. | 🟡 **Vừa** | 🟡 **Thấp** |

---

## 5. Top 5 Tính Năng ĐỘC QUYỀN (VIP-Gated Exclusive) Giúp Tăng Doanh Thu

1. **Biometric Vault & Chế độ Quét Ẩn Danh (Incognito Scanner):**
   * *Mô tả:* Khóa bảo vệ toàn bộ lịch sử bằng Vân tay/FaceID. Tích hợp chế độ "Quét ẩn danh" (không lưu lịch sử, tự xóa clipboard sau 60 giây khi quét mật khẩu WiFi, thẻ tín dụng).
2. **Pro QR Design Studio (QR Nghệ Thuật & Xuất Bản In Tem Nhãn):**
   * *Mô tả:* Cho phép chọn hình dạng mắt mã QR (tròn, vuông, kim cương), màu gradient nghệ thuật, tự chèn logo công ty không làm hỏng mã, xuất định dạng Vector SVG/PDF chuẩn in ấn công nghiệp.
3. **Kiểm Kê Kho Hàng & Quét Đa Mã Đồng Thời (Multi-Barcode Continuous Scanner):**
   * *Mô tả:* Nhận diện cùng lúc nhiều mã vạch trong 1 khung hình camera, đếm số lượng, phân loại mã và xuất file Excel/CSV chuẩn theo cột tùy chỉnh phục vụ quản lý kho.
4. **Sao Lưu & Đồng Bộ Cloud Riêng Tư (Google Drive Sync):**
   * *Mô tả:* Tự động đồng bộ lịch sử và các mã barcode đã tạo lên Google Drive cá nhân của người dùng, không bao giờ mất dữ liệu khi đổi thiết bị.
5. **Truy Xuất Xuất Xứ & Cảnh Báo Sản Phẩm (Product Intelligence):**
   * *Mô tả:* Quét mã vạch hàng hóa nhận diện quốc gia xuất xứ chính xác (GTIN prefix), tra cứu thành phần độc hại, cảnh báo hàng giả/thu hồi và điểm dinh dưỡng (Nutri-Score).

---

## 6. So Sánh & Đánh Giá 14 Item Trong Backlog (`doc/task.md`)

Sau khi đối chiếu với tài liệu `doc/task.md`, dưới đây là quan điểm review chéo độc lập:

| Task ID | Tính năng | Đánh giá | Lý do chi tiết |
|---|---|---|---|
| **E1** | Torch auto-on | ✅ **ĐỒNG Ý** | Rất hữu ích khi quét trong môi trường tối. Cần lưu ý thêm bộ lọc chống nhấp nháy (hysteresis debounce) khi ánh sáng mấp mé ngưỡng. |
| **E2** | History date group header | ✅ **ĐỒNG Ý (CẦN SỬA CÁCH LÀM)** | Giao diện cần phân nhóm ngày, nhưng **không nên cố chắp vá `CursorAdapter`** (`getViewTypeCount=2`). Nên migrate thẳng `ListView` sang `RecyclerView` để tránh bug position khi scroll. |
| **E3** | Custom QR size preset | ✅ **ĐỒNG Ý** | Đơn giản, thêm các chip preset (128, 256, 512, 1024, 2048px) giúp trải nghiệm tạo mã thuận tiện hơn. |
| **E4** | Confetti nâng cấp | 🟡 **KHÔNG ĐỒNG Ý ƯU TIÊN** | Không mang lại giá trị cốt lõi cho người dùng. Thêm thư viện `konfetti-android` làm tăng kích thước APK không cần thiết; hiệu ứng hiện tại bằng code tay đã đủ đẹp và nhẹ. |
| **E5** | CSV export theo filter | ✅ **ĐỒNG Ý & CẦN FIX BUG** | Cực kỳ quan trọng. Cần kết hợp sửa lỗi cú pháp thừa dấu phẩy cuối dòng và sửa hàm xóa nhầm toàn bộ DB đã nêu ở mục 1. |
| **E7** | Zoom memory | ✅ **ĐỒNG Ý** | Tính năng đã có nền tảng sẵn trong code, chỉ cần bổ sung toggle bật/tắt trong Cài đặt và hoàn thiện test. |
| **E8** | Splash skip gần đây | ✅ **ĐỒNG Ý** | Giúp người dùng mở lại app trong vòng 30 phút vào thẳng camera ngay mà không bị khó chịu bởi App Open Ad. |
| **F3** | Scan tags (Gắn nhãn) | ✅ **ĐỒNG Ý** | Hữu ích cho người dùng nhiều mã (phân loại Hóa đơn, Cá nhân, Công việc). Rất phù hợp để gate tính năng VIP (Free tối đa 2 tag, VIP không giới hạn). |
| **F4** | Geo-tag scans | 🟡 **KHÔNG ĐỒNG Ý ƯU TIÊN** | Việc đòi quyền vị trí (`ACCESS_FINE_LOCATION`) liên tục khi quét mã dễ khiến người dùng cảnh giác, sợ bị theo dõi và Google Play kiểm duyệt khắt khe hơn. |
| **F5** | Barcode compare | 🟡 **KHÔNG ĐỒNG Ý ƯU TIÊN** | Nhu cầu so sánh 2 mã barcode rất ít (niche use-case), chỉ nên làm sau khi đã hoàn thành các tính năng thanh toán/kiểm kê. |
| **F7** | Scan reminder | ❌ **KHÔNG ĐỒNG Ý** | Scanner là công cụ tiện ích nhanh, người dùng không dùng nó như app To-do. Cồng kềnh thêm `WorkManager` và quyền thông báo `POST_NOTIFICATIONS`. |
| **F8** | Biometric lock history | ✅ **ĐỒNG Ý HOÀN TOÀN** | Tính năng đáng giá nhất để thúc đẩy người dùng mua VIP (bảo vệ các thông tin quét nhạy cảm như Wi-Fi, danh bạ, tài khoản). |
| **F9** | Auto-action config | ✅ **ĐỒNG Ý** | Nâng cao trải nghiệm quét: tự mở URL, tự kết nối WiFi mà không cần bấm thêm nút nào. |
| **F10** | OCR -> QR | ✅ **ĐỒNG Ý** | ML Kit Text Recognition chạy on-device rất mượt, giúp tạo mã nhanh từ tài liệu thực tế. |

---

## 7. Những Điểm Quan Trọng Backlog Đang Bỏ Sót

1. **Bỏ sót hoàn toàn các Crash & Bug nghiêm trọng ở runtime:**
   * Lỗi crash `Color.luminance()` trên Android 7 (API 24/25) (`FEncode.kt:294`).
   * Lỗi crash NPE `Scan.hashCode()` (`Scan.kt:66`).
   * Lỗi tê liệt Bluetooth trên Android < 12 (`Permissions.kt:39`).
   * Lỗi vi phạm hợp đồng Intent ZXing (`ActivityCamera.kt:901`).
2. **Lỗ hổng xóa nhầm toàn bộ Database (`FHistory.kt:454`):**
   * Người dùng lọc danh sách và bấm xóa sẽ bị mất trắng toàn bộ lịch sử quét từ trước tới nay.
3. **Chưa có lộ trình chuẩn bị chuyển đổi sang CameraX:**
   * Việc phụ thuộc vào `CameraView` dựa trên `android.hardware.Camera` cũ sẽ sớm gặp giới hạn phần cứng trên các máy Android 15/16 đời mới.
4. **Bỏ quên mỏ vàng "VietQR / Banking QR":**
   * Đây là tính năng có nhu cầu thực tế cao nhất hàng ngày tại thị trường Việt Nam và khu vực, có thể tăng gấp 3 lần lượng mở app (DAU).
5. **Giao diện VIP hiển thị giá In-App Purchase nhưng chưa nối Google Play Billing:**
   * Màn hình `FVipManagement` hiển thị các mức giá $0.50, $1.00... nhưng chưa có code BillingClient, người dùng tưởng tính năng bị liệt. Cần đồng bộ giữa UI và việc tích hợp thư viện thanh toán.
