# BUG-05 — 🟠 P1 — Bluetooth vô hiệu hóa sai trên Android 7-11

**Status:** 📋 TODO — Sprint 0
**Nguồn:** agy — đã tự verify

## Hiện trạng (đã verify)

`ext/app/Permissions.kt:34`:
```kotlin
fun Activity.hasBluetoothPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    hasPermission(Manifest.permission.BLUETOOTH_CONNECT, PERMISSION_BLUETOOTH)
} else {
    false   // BUG
}
```
Trên Android < 12 (API < 31), quyền Bluetooth là **install-time permission** (khai báo trong manifest là tự động cấp, không cần runtime request). Trả `false` ở nhánh `else` khiến tính năng "Send scan via Bluetooth" (`BluetoothSender`) **không bao giờ hoạt động** trên Android 7 tới 11 — 5 phiên bản Android, kể cả khi manifest đã khai báo đúng permission.

## Fix

```kotlin
} else {
    true   // install-time permission trước Android 12, luôn coi như đã cấp
}
```
Xác nhận `AndroidManifest.xml` có khai báo `<uses-permission android:name="android.permission.BLUETOOTH" />` (và `BLUETOOTH_ADMIN` nếu cần) không bị giới hạn `maxSdkVersion` sai.

## Test

- Emulator API ≤ 30: bật tính năng gửi scan qua Bluetooth trong Settings → scan 1 mã → xác nhận flow Bluetooth kích hoạt (không bị chặn ở bước check permission).
- Emulator API ≥ 31 (S+): xác nhận vẫn request runtime permission `BLUETOOTH_CONNECT` bình thường, không bị ảnh hưởng bởi fix.
