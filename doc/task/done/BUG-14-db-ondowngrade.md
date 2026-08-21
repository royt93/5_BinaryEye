# BUG-14 — 🟡 P2 — Thiếu onDowngrade() trong SQLiteOpenHelper

**Status:** ✅ DONE — 2026-08-21

Thêm override `onDowngrade()` rỗng trong `database/Db.kt` `OpenHelper` — mọi migration hiện tại chỉ `ADD COLUMN` (không xoá/đổi tên), nên giữ nguyên schema (no-op) là an toàn cho code cũ đọc lại DB có version cao hơn (vd sau "undo update"/restore backup). Nếu không override, `SQLiteOpenHelper` mặc định ném `SQLiteException` và crash app khi mở. Verify: compile pass.
