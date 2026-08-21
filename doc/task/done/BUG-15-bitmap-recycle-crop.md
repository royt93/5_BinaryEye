# BUG-15 — 🟡 P2 — Bitmap không recycle khi kéo vùng crop (OOM risk)

**Status:** ✅ DONE — 2026-08-21

`view/act/ActivityPick.kt scanWithinBounds()` — bọc phần decode trong `try/finally`, gọi `cropped.recycle()` ở `finally` sau khi decode xong (bitmap `cropped` chỉ dùng để decode, không hiển thị UI nên an toàn recycle ngay). Trước đây mỗi lần kéo ROI tạo bitmap mới không giải phóng, có thể giữ nhiều bitmap 1024px cùng lúc. Verify: compile + 33 unit test pass.
