# BUG-07 — 🟡 P2 — ZXing Intent Protocol format string

**Status:** ✅ DONE — 2026-08-21

`putExtra("SCAN_RESULT_FORMAT", result.format)` truyền thẳng enum thay vì String. Đã sửa `view/act/ActivityCamera.kt:getReturnIntent()` → `result.format.name`. Verify: `compileDevDebugKotlin` BUILD SUCCESSFUL, không có call site nào khác đọc lại giá trị này trong app (chỉ ảnh hưởng app thứ 3 gọi theo chuẩn ZXing).
