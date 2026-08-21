# BUG-11 + BUG-12 — 🟡 P2 — Cursor leak + race condition khi search History

**Status:** ✅ DONE — 2026-08-21, verify trên máy thật (History load/search bình thường, không crash)

`frm/FHistory.kt update()`:
- **BUG-11**: `val ac = activity ?: return@withContext` giờ đóng `cursor` trước khi return (`cursor?.close()`) nếu fragment đã detach — trước đây cursor mở ở background thread bị rò rỉ trong trường hợp này.
- **BUG-12**: thêm `searchJob: Job?` field, mỗi lần `update()` gọi sẽ `searchJob?.cancel()` trước khi launch job mới + `delay(SEARCH_DEBOUNCE_MS = 300L)` — tránh mỗi keystroke tạo 1 query DB, và tránh race (kết quả cũ về sau đè kết quả mới).

Verify: compile + 33 unit test pass; trên Pixel 7 Pro mở History, gõ search, xoá lịch sử — tất cả hoạt động bình thường, logcat sạch không exception.
