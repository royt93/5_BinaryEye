# BUG-08 — 🟡 P2 — vCard số điện thoại thứ 3 sai key

**Status:** ✅ DONE — 2026-08-21

`view/actions/vtype/vcard/VCardAction.kt:55-56` — vế phải đổi từ `TERTIARY_PHONE_TYPE` (lặp) thành `TERTIARY_PHONE` đúng chuẩn. Verify: compile pass; không có unit test cho vCard builder (ghi nhận vào Epic G test-gap nếu cần sau).
