# E5 — 🟠 CSV/JSON Export theo filter — prompt filtered/all

**Status:** ✅ DONE — 2026-08-21, verify trên Pixel 7 Pro

## Implementation

`frm/FHistory.kt askToExportToFile()` — sau khi user chọn format export (không áp dụng cho "db" — luôn là bản sao toàn bộ file), nếu đang có filter active (`!scanFilter.isDefault`), hiện dialog hỏi "Export filtered (N) or all?" trước khi hỏi tên file. Chọn "Toàn bộ" dùng `ScanFilter()` mặc định thay `scanFilter` hiện tại cho câu query.

`strings.xml`: `export_filter_prompt_title/message/filtered/all`.

## Test

Verify trên máy thật: 5 scan (2 Today + 1 mỗi nhóm khác) → filter "Today" → Export CSV → dialog hiện đúng "Export only the 2 matching scan(s), or all scans?" → chọn **ALL** → file xuất ra có đủ **5/5** dòng (không bị giới hạn theo filter đang active) → xác nhận logic override đúng.

Không cần thêm unit test riêng: logic chỉ là 1 nhánh điều kiện + dùng lại `ScanFilter`/`db.getScansDetailed` đã có test gián tiếp qua các test khác (`ScanFilterTest`, verify CSV BUG-10 trên máy thật).
