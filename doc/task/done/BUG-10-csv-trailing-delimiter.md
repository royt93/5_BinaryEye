# BUG-10 — 🟡 P2 — CSV export lệch số cột giữa header và data

**Status:** ✅ DONE — 2026-08-21, verify trên máy thật

`database/CsvExport.kt:toCsvRecord()` — đổi từ `forEach { append(delimiter) }` (thừa delimiter ở cột cuối) sang `joinToString(separator = delimiter)` giống hệt convention của header. Verify trên Pixel 7 Pro: export 2 dòng test → `awk -F',' '{print NF}'` cho cả header và data đều ra đúng **12 cột** (trước fix data sẽ có 13 cột do delimiter thừa).
