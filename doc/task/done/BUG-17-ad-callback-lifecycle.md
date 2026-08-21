# BUG-17 — 🟢 P3 — Ad callback không check lifecycle trước startActivity

**Status:** ✅ DONE — 2026-08-21

`view/act/ActivityCamera.kt onOptionsItemSelected()` — 3 chỗ (`create`, `history`, `preferences`) gọi `AdManager.showInterstitial(this) { ... startActivity(...) }`; thêm guard `if (isFinishing || isDestroyed) return@showInterstitial` đầu callback để tránh navigate khi Activity đã finish/destroy lúc callback bất đồng bộ trả về. Verify: compile pass.

## Ghi chú — BUG-16 KHÔNG fix

`versionCode 20270822` vs `versionName '2026.08.22'` — kiểm tra `git log -p -- app/build.gradle` phát hiện pattern "2027xxxx" cho versionCode là **convention nhất quán qua nhiều release liên tiếp** (20270627 → 20270623 → 20270627 → 20270822), không phải typo đơn lẻ. Đổi versionCode về "2026xxxx" sẽ THẤP HƠN các bản đã release trước đó → vi phạm yêu cầu versionCode phải tăng đơn điệu của Play Store, có thể chặn submit bản build mới. **Chủ động không sửa**, để dev xác nhận ý định thật của convention này trước khi đụng vào.
