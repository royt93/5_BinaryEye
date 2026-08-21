# BUG-06 — 🟠 P1 — 3 ngôn ngữ lớn fallback về tiếng Anh

**Status:** 📋 TODO — Sprint 0
**Nguồn:** agy — đã tự verify toàn bộ chuỗi bằng chứng

## Hiện trạng (đã verify)

`ext/app/Locale.kt:11-16`:
```kotlin
val localeParts = localeName.split("-")
val locale = if (localeParts.size == 2) {
    Locale(localeParts[0], localeParts[1])   // parts[1] dùng thẳng làm ISO country code
} else {
    Locale(localeName)
}
```

`frm/FLanguageDialog.kt:32,38,39` truyền locale value theo convention thư mục tài nguyên Android (có tiền tố `r`):
```kotlin
Triple("🇨🇳", "简体中文", "zh-rCN"),
Triple("🇧🇷", "Português do Brasil", "pt-rBR"),
Triple("🇷🇺", "Русский", "ru-rRU"),
```
`res/` có sẵn `values-zh-rCN`, `values-pt-rBR`, `values-ru-rRU`.

**Vấn đề:** `"zh-rCN".split("-")` → `["zh", "rCN"]` → `Locale("zh", "rCN")`. `"rCN"` **không phải** mã quốc gia ISO 3166 hợp lệ (mã đúng là `"CN"`, tiền tố `r` chỉ là quy ước đặt tên thư mục tài nguyên Android, không phải một phần của mã locale thật). `Locale("zh","rCN")` không khớp bất kỳ locale hệ thống hợp lệ nào → khi `resources.updateConfiguration()` áp dụng, hệ thống không tìm thấy resource phù hợp → **fallback về `values/` (tiếng Anh)** cho toàn bộ Chinese Simplified, Brazilian Portuguese, Russian dù app quảng bá hỗ trợ 20+ ngôn ngữ.

## Fix

```kotlin
val localeParts = localeName.split("-")
val locale = if (localeParts.size == 2) {
    Locale(localeParts[0], localeParts[1].removePrefix("r"))
} else {
    Locale(localeName)
}
```
(Hoặc chuẩn hóa hơn: parse đúng BCP-47 bằng `Locale.forLanguageTag(localeName.replace("-r", "-"))`.)

## Test

- Manual: Settings → Language → chọn "简体中文" (zh-rCN) → xác nhận toàn bộ UI chuyển sang tiếng Trung (không phải tiếng Anh).
- Lặp lại cho "Português do Brasil" (pt-rBR) và "Русский" (ru-rRU).
- Kiểm tra các locale value KHÔNG có tiền tố `r` (đa số ngôn ngữ khác) vẫn hoạt động bình thường sau fix (không bị regression).
