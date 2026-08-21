# BUG-04 — 🔴 P1 — Crash khi decoder trả danh sách rỗng

**Status:** ✅ DONE — 2026-08-21, build+test pass
**Nguồn:** codex + claude (2/2 độc lập)

## Hiện trạng

`view/act/ActivityCamera.kt:572` và `view/act/ActivityPick.kt:156`: sau khi gọi `ZxingCpp.readByteArray(...)`, code chỉ null-check kết quả rồi gọi `.first()` trên list — nếu decoder trả `emptyList()` (không null, nhưng rỗng), `.first()` ném `NoSuchElementException`.

`ActivityPick` nghiêm trọng hơn: coroutine bao quanh không có `CoroutineExceptionHandler`, nên exception này crash **toàn bộ app**, không chỉ activity hiện tại.

## Fix

1. Đổi `.first()` → `.firstOrNull() ?: return` (hoặc early-return/toast phù hợp ngữ cảnh) ở cả 2 vị trí.
2. `ActivityPick`: cân nhắc bọc coroutine decode bằng `CoroutineExceptionHandler` để crash tương lai (nếu có) không kéo sập app, chỉ log + toast lỗi.

## Test

- Manual: đưa camera/ảnh vào vùng không có mã nào để decode → app không crash, xử lý gracefully (toast "không tìm thấy mã" hoặc tương đương).
- Nếu có thể mock `ZxingCpp` trả `emptyList()` trong test Robolectric/instrumented, thêm case cho cả `ActivityCamera` và `ActivityPick`.
