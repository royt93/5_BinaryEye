# Code Review Report

> Ngày review: 2026-03-18 | Files scanned: 71 Kotlin files

---

## 🔴 HIGH — Bugs / Crash Risk

### H1 · `ActivityMain.kt` line 90 — Non-null assertion `!!` có thể crash
```kotlin
intent.getParcelableExtra(DECODED)!!   // NPE nếu extra null
```
**Vấn đề:** `getParcelableExtra()` trả về `null` nếu key không tồn tại hoặc type mismatch → app crash với `NullPointerException`.  
**Fix:**
```kotlin
intent.getParcelableExtra(DECODED) ?: return FPreferences()
```

---

### H2 · `BluetoothSender.kt` line 20 — Unmanaged `CoroutineScope`
```kotlin
CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) { ... }
```
**Vấn đề:** Scope tạo ra không bao giờ bị cancel → nếu kết nối Bluetooth fail và coroutine còn đang chạy, memory leak xảy ra.  
**Fix:** Dùng scope cấp module-level với lifecycle rõ ràng hoặc `GlobalScope` (chấp nhận cho one-shot).

---

### H3 · `ScanSender.kt` line 22 — Unmanaged `CoroutineScope`
```kotlin
CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) { ... }
```
**Vấn đề:** Tương tự H2 — scope không được cancel nếu caller bị destroy trước khi response về.  
**Fix:** Truyền scope từ caller (Activity/Fragment) vào hàm, hoặc dùng `GlobalScope.launch` nếu chấp nhận fire-and-forget.

---

## 🟠 MEDIUM — Memory Leaks

### M1 · `BluetoothSender.kt` lines 55–61 — File-level global mutable state
```kotlin
private var socket: BluetoothSocket? = null
private var writer: OutputStreamWriter? = null
private var isConnected = false
```
**Vấn đề:** `socket` và `writer` là file-level globals (chia sẻ toàn process). Nếu Activity bị destroy mà coroutine chưa kết thúc, các object này vẫn được giữ → leak. Ngoài ra không thread-safe.  
**Fix:** Đóng gói trong class `BluetoothSender` với lifecycle quản lý bởi `ViewModel` hoặc `Application`.

---

### M2 · `Beeps.kt` lines 7–8 — File-level ToneGenerator globals
```kotlin
private var confirmToneGenerator: ToneGenerator? = null
private var errorToneGenerator: ToneGenerator? = null
```
**Vấn đề:** Nếu `releaseToneGenerators()` không được gọi (ví dụ khi `onDestroy()` bị bỏ sót), `ToneGenerator` không được release → tốn tài nguyên Audio hardware.  
**Hiện trạng:** `ActivityPick` có gọi `releaseToneGenerators()` trong `onDestroy()` — OK. Nhưng `ActivityCamera` cần kiểm tra.  
**Mức độ:** Thấp nếu tất cả caller đều release, Medium nếu có caller bỏ sót.

---

## 🟡 LOW — Warnings / Code Quality

### W1 · `BaseActivity.kt` line 45 — `println()` debug log trong production
```kotlin
println("Adaptive refresh rate applied: ${highestRefreshRateMode.refreshRate} Hz")
```
**Vấn đề:** Log debug xuất hiện trong production build, gây lộ thông tin device và tốn I/O.  
**Fix:** Xóa hoặc dùng `Log.d("BaseActivity", ...)` chỉ trong DEBUG build.

---

### W2 · `BaseActivity.kt` lines 30–35 — Dead `else` branch không thể đạt được
```kotlin
val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    display
} else {
    @Suppress("DEPRECATION")
    wm.defaultDisplay   // ← DEAD CODE: hàm này chỉ được gọi khi SDK >= R (line 23)
}
```
**Vấn đề:** Hàm `enableAdaptiveRefreshRate()` chỉ được gọi khi `SDK >= R`, vậy `else` branch không bao giờ chạy.  
**Fix:** Đơn giản hóa thành `val display = display`.

---

### W3 · Nhiều nơi — Dead code `attachBaseContext` bị comment
```kotlin
// ActivityMain.kt lines 49–52
// ActivityPick.kt lines 65–68
//    override fun attachBaseContext(base: Context?) { ... }
```
**Vấn đề:** Code bị comment nhưng không xóa → gây confuse khi đọc code.  
**Fix:** Xóa các block comment này vì `BaseActivity` đã handle `attachBaseContext` rồi.

---

### W4 · `ActivityPick.kt` line 240 — Bug logic trong `rotateClockwise()`
```kotlin
cropImageView.imageRotation += 90 % 360
```
**Vấn đề:** Operator precedence: `90 % 360` = `90` (hằng số!). Ý định là `(imageRotation + 90) % 360`.  
**Kết quả thực tế:** Mỗi lần tap → +90 degrees, không wrap về 0 sau 360°. Ảnh có thể rotate đến 720°, 1080°... **Vẫn hoạt động** do view tự normalize, nhưng giá trị `imageRotation` tích lũy mãi mãi.  
**Fix:**
```kotlin
cropImageView.imageRotation = (cropImageView.imageRotation + 90) % 360
```

---

### W5 · `FBarcode.kt` line 76 — Redundant `let` với biến không-null
```kotlin
var message = e.message
if (message.isNullOrEmpty()) {
    message = getString(R.string.error_encoding_barcode)
}
message.let {
    ac.toast(message)   // ← dùng `message` thay vì `it`, `.let {}` vô nghĩa
}
```
**Fix:**
```kotlin
ac.toast(message ?: getString(R.string.error_encoding_barcode))
```

---

### W6 · `AdMobManager.kt` — EventBus CoroutineScope một lần không được quản lý
(đã ghi nhận từ phiên trước — low risk vì one-shot)

---

## 📋 Tổng hợp theo mức độ

| ID | File | Severity | Loại | Mô tả ngắn |
|----|------|----------|------|------------|
| H1 | `ActivityMain.kt:90` | 🔴 HIGH | Bug/Crash | `!!` non-null assertion gây NPE |
| H2 | `BluetoothSender.kt:20` | 🔴 HIGH | Memory Leak | Unmanaged CoroutineScope |
| H3 | `ScanSender.kt:22` | 🔴 HIGH | Memory Leak | Unmanaged CoroutineScope |
| M1 | `BluetoothSender.kt:55-61` | 🟠 MEDIUM | Memory Leak | Global mutable state (socket, writer) |
| M2 | `Beeps.kt:7-8` | 🟠 MEDIUM | Resource Leak | ToneGenerator globals cần verify release |
| W1 | `BaseActivity.kt:45` | 🟡 LOW | Warning | `println()` debug trong production |
| W2 | `BaseActivity.kt:30-35` | 🟡 LOW | Dead Code | `else` branch không thể đạt được |
| W3 | Nhiều file | 🟡 LOW | Dead Code | `attachBaseContext` comment blocks |
| W4 | `ActivityPick.kt:240` | 🟡 LOW | Bug Logic | `90 % 360` operator precedence sai |
| W5 | `FBarcode.kt:76` | 🟡 LOW | Code Quality | Redundant `.let {}` block |

---

## Gợi ý fix ưu tiên

1. **H1** — Fix `!!` trong `ActivityMain` → nguy cơ crash production cao nhất
2. **H2+H3** — BluetoothSender & ScanSender → pass scope từ caller
3. **W4** — `rotateClockwise()` → logic bug không ảnh hưởng UX ngay nhưng sẽ gây vấn đề khi save rotation state
4. **W1** — Xóa `println()` trước khi release
5. **W2+W3** — Cleanup dead code
