# BUG-02 — 🔴 P1 — Crash 100% trên Android 7.0/7.1 khi mở màn Encode

**Status:** 📋 TODO — Sprint 0
**Nguồn:** agy — đã tự verify

## Hiện trạng (đã verify)

`frm/FEncode.kt:294-295`:
```kotlin
swatchFg.background = colorCircle(fgColor, needsBorder = Color.alpha(fgColor) > 200 && Color.luminance(fgColor) > 0.9f)
swatchBg.background = colorCircle(bgColor, needsBorder = Color.alpha(bgColor) > 200 && Color.luminance(bgColor) > 0.9f)
```
`Color.luminance(fgColor)` gọi `android.graphics.Color.luminance(int)` — API chỉ tồn tại từ **API 26**. App `minSdk = 24`. Trên Android 7.0 (API 24) / 7.1 (API 25), gọi hàm này ném `NoSuchMethodError` ngay khi user mở màn Encode và code chạy tới đoạn set màu swatch.

## Fix

Thay bằng `androidx.core.graphics.ColorUtils.calculateLuminance(color)` (đã có sẵn trong `androidx.core`, hỗ trợ từ API 1, trả `Double` — nhớ so sánh `> 0.9` thay vì `0.9f` nếu đổi kiểu, hoặc ép `.toFloat()`).

```kotlin
import androidx.core.graphics.ColorUtils
// ...
Color.alpha(fgColor) > 200 && ColorUtils.calculateLuminance(fgColor) > 0.9
```

## Test

- Chạy trên emulator API 24 (hoặc API 25) → mở tab Encode → không crash, swatch màu hiện đúng viền khi màu sáng gần trắng.
- Robolectric widget test (nếu thêm được `@Config(sdk = [24])` cho case này) verify không throw.
