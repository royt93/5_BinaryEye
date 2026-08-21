# BUG-13 — 🟡 P2 — Export DB thiếu dữ liệu WAL

**Status:** ✅ DONE — 2026-08-21

Thêm `Db.checkpoint()` (`PRAGMA wal_checkpoint(FULL)`) trong `database/Db.kt`, gọi từ `Export.kt:exportDatabase()` trước khi copy file `.db` — đảm bảo dữ liệu mới nhất đã được flush từ `-wal` vào file chính trước khi export/backup. Verify: compile pass.
