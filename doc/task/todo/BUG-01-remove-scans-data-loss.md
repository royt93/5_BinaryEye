# BUG-01 — 🔴 P0 — Xóa nhầm toàn bộ lịch sử khi đang filter

**Status:** 📋 TODO — Sprint 0
**Nguồn:** codex + agy + claude (3/3 độc lập) + tự verify

## Hiện trạng (đã verify)

`database/Db.kt:191`:
```kotlin
fun removeScans(query: String? = null) {
    db.delete(SCANS, getWhereClause(query, ""), getWhereArguments(query))
}
```
Chỉ nhận `query` (text search), không nhận `dateRange`/`formatGroup`.

`frm/FHistory.kt:454`:
```kotlin
.setPositiveButton(android.R.string.ok) { _, _ ->
    db.removeScans(scanFilter.query)   // BUG: bỏ qua scanFilter.dateRange/formatGroup
    updateAndClearFilter()
}
```
Dialog xác nhận dùng `scanFilter.isDefault` (tính cả date/format) để chọn message "reallyRemoveAllScans" vs "reallyRemoveSelectedScans" — nhưng hành động xóa thật chỉ dùng `scanFilter.query`.

**Kịch bản mất dữ liệu:** User filter "Today + QR only" (không gõ text search) → `scanFilter.query == null` → bấm nút xóa (tưởng chỉ xóa các scan đang hiển thị) → `getWhereClause(null, "")` trả về WHERE rỗng → `db.delete(SCANS, "", null)` xóa **TOÀN BỘ** bảng `scans`, không chỉ phần đang lọc.

## Fix

1. Đổi chữ ký: `fun removeScans(filter: ScanFilter)`.
2. Dùng `filter.toWhereClause()` / `filter.toWhereArgs()` (đã có sẵn, pure-function từ fix A8 trong `ScanFilter.kt`) thay cho `getWhereClause(query, "")`.
3. Gọi từ `FHistory.kt:454`: `db.removeScans(scanFilter)`.
4. Kiểm tra còn nơi nào khác gọi `db.removeScans(...)` với chữ ký cũ (grep toàn repo) để cập nhật đồng bộ.

## Test

- Unit test (JVM, theo pattern `ScanFilterTest` đã có): filter "Today only" → `removeScans(filter)` → chỉ xóa đúng record khớp WHERE, record khác ngày còn nguyên.
- Manual: scan 3 mã khác ngày → filter "Today" → bấm xóa → chỉ mã hôm nay biến mất, 2 mã còn lại vẫn còn trong History (bỏ filter để xem).
- Manual: không filter gì (mặc định) → bấm xóa → xác nhận đúng là xóa tất cả (hành vi mong muốn khi không filter).
