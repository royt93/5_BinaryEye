# E2 — 🟠 History Date Group Header

**Status:** ✅ DONE — 2026-08-21, verify trên Pixel 7 Pro

## Implementation — Hướng A (vá CursorAdapter, không migrate RecyclerView)

- `adapter/DateGroup.kt` (mới) — pure function `classifyDateGroup(datetimeText, nowMs)` phân loại Today/Yesterday/This Week/Older, không phụ thuộc Android framework.
- `adapter/ScansAdapter.kt` — viết lại:
  - Precompute `rows: List<Row>` (Header hoặc Item) 1 lần khi tạo adapter (khớp cách `FHistory` luôn tạo adapter mới mỗi lần cursor đổi, không gọi lại `changeCursor` với cursor khác null).
  - Override `getCount/getViewTypeCount/getItemViewType/getItem/getItemId/getView/isEnabled/areAllItemsEnabled` để map vị trí ListView → vị trí Cursor thật, và loại header khỏi click/long-click (`isEnabled=false`).
  - `res/layout/roy_v_row_scan_header.xml` (mới) — 1 TextView bold uppercase.
- `strings.xml`: `history_group_today/yesterday/this_week/older`.

## Test

- Unit: `DateGroupTest.kt` (7 test) — biên ngày, parse lỗi, future-date an toàn.
- Widget (Robolectric): `ScansAdapterRobolectricTest.kt` (5 test) — đếm đúng số dòng (item+header), header/item đúng `isEnabled`, header hiện đúng label, item hiện đúng content, `getItem`/`getContent` map đúng theo position.
- Verify trên máy thật: insert 5 scan trải 4 nhóm (Today×2/Yesterday/This Week/Older) → header hiện đúng thứ tự và nhãn; tap vào header **không** mở detail (đúng thiết kế); tap vào item mở đúng detail; long-press item vẫn chọn đúng (ActionMode + highlight); filter "Today" → chỉ còn header "TODAY" + 2 item đúng.
- Logcat sạch suốt phiên test, không FATAL/exception.

## Rủi ro đã cân nhắc

Đây là thay đổi risk cao nhất trong Sprint 2 vì đụng core position-mapping của `CursorAdapter` (ListView position ≠ Cursor position sau khi chèn header) — mọi method dùng `position` (`getItem`, `getItemId`, `getView`, `select`, `getContent`, `getName`) đều đã audit lại để đảm bảo map đúng qua `rows[position]`.
