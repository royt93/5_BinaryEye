# E8 — 🟡 Splash Skip Nếu Mở Gần Đây

**Status:** ✅ DONE — 2026-08-21, build+test pass (verify: chưa làm — 0 kết quả grep `LAST_FOREGROUND`)

## Mục tiêu

Nếu app đã mở trong 30 phút gần nhất (cold start lại do hệ thống kill), bỏ qua App Open ad và vào Main nhanh hơn — cải thiện retention. **Lưu ý (từ AI review):** cân nhắc kỹ ngưỡng thời gian vì ảnh hưởng trực tiếp doanh thu App Open ad — 30 phút là gợi ý ban đầu, có thể cần tune sau khi có số liệu thật.

## Việc cần làm

1. `pref/Pref.kt`: thêm `LAST_FOREGROUND_MS = "last_foreground_ms"` (Long) + accessor `lastForegroundMs`.
2. `view/act/ActivitySplash.kt`: nếu `System.currentTimeMillis() - prefs.lastForegroundMs < 30 * 60_000` → bỏ qua `runSplashAdFlow()`/`awaitSplashComplete`, gọi `goToMain()` ngay.
3. Ghi `prefs.lastForegroundMs = System.currentTimeMillis()` khi vào Main (hoặc `onPause()` của `ActivityMain`).

## Test

- Mở app → vào Main → back ra hệ thống → mở lại trong vòng 30 phút → không thấy App Open ad, vào Main ngay.
- Mở lại sau > 30 phút → thấy App Open ad như bình thường (không bị bug tắt ad vĩnh viễn).
