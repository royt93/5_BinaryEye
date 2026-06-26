# Animation Test Cases — BinaryEye / Cat Scanner

> Tạo: 2026-06-22 | Scope: Manual UI test cho tất cả animations
> Môi trường: DevDebug build, API 24–36

---

## Quy ước

- **PASS** = animation chạy đúng như mô tả, không giật, không freeze
- **FAIL** = animation không chạy, giật, memory leak, hoặc không cancel khi cần
- **N/A** = không applicable với device/API đang test

---

## Module 1: FVipManagement — Animation Entrance (Stagger)

### AT-VIP-01: Stagger entrance khi mở VIP screen lần đầu

**Precondition:** App ở màn hình Settings, chưa mở VIP screen.
**Steps:**
1. Tap menu item "VIP Management" trong Settings

**Expected:**
- Các card và row xuất hiện từ dưới lên theo thứ tự tuần tự, cách nhau ~70ms:
  - `featuresCard` → `rowWatchAd` → `planRow30` → `planRow90` → `planRow1Y` → `planRowLifetime`
- Mỗi view: alpha 0 → 1, translationY 40dp → 0, duration 380ms
- Không có view nào "nhảy" hoặc hiện sẵn trước animation

**Pass criteria:** 6 views animate tuần tự, mượt mà, tổng duration ~(380 + 70×5) = ~730ms

---

### AT-VIP-02: Stagger entrance khi navigate back rồi mở lại

**Precondition:** Đã mở VIP screen một lần.
**Steps:**
1. Nhấn Back để về Settings
2. Tap "VIP Management" lần 2

**Expected:** Animation stagger chạy lại từ đầu, không bị skip hoặc incomplete.

---

### AT-VIP-03: Fragment slide-in từ dưới

**Precondition:** Ở Settings screen.
**Steps:**
1. Tap "VIP Management"

**Expected:** Toàn bộ VIP fragment slide từ dưới lên, animation `slide_in_bottom` khoảng 300ms. Xảy ra TRƯỚC stagger entrance (hai animation chain nhau).

---

## Module 2: FVipManagement — Hero Glow Rotation

### AT-VIP-04: Hero glow xoay khi VIP screen visible

**Precondition:** VIP screen đang hiển thị.
**Verify:**
- `viewHeroGlow` xoay liên tục 360°
- Duration: ~12 giây / vòng
- Interpolator: linear (tốc độ đều)
- Lặp vô hạn

**Pass criteria:** Quan sát 15 giây — glow xoay đều, không giật.

---

### AT-VIP-05: Hero glow DỪNG khi app vào background

**Precondition:** VIP screen đang mở, glow đang xoay.
**Steps:**
1. Nhấn Home button (app vào background)
2. Chờ 5 giây
3. Quay lại app

**Expected:**
- Khi vào background: glow animation dừng (`onPause` cancel)
- Khi quay lại: glow animation resume (`onResume` start lại)

**Fail case:** Nếu animation tiếp tục chạy ở background → memory/CPU leak.

---

### AT-VIP-06: Hero glow DỪNG khi navigate away

**Steps:**
1. Từ VIP screen, nhấn Back
2. Mở VIP screen lại

**Expected:** Không có animation "phantom" chạy; animation mới bắt đầu clean khi mở lại.

---

## Module 3: FVipManagement — Watch Ad Pulse

### AT-VIP-07: Pulse animation trên row "Watch Ad" — Free user

**Precondition:** User đang ở Free (chưa có VIP).
**Verify:**
- Row `rowWatchAd` có pulse animation: scale 1.0 → 1.05 → 1.0
- Lặp vô hạn, mượt mà
- Animation chạy ngay khi `onResume`

**Pass criteria:** Nhìn thấy hiệu ứng "nhịp tim" trên nút Watch Ad.

---

### AT-VIP-08: Pulse animation DỪNG khi vào background

**Steps:** Tương tự AT-VIP-05 nhưng focus vào `rowWatchAd`.

**Expected:** Pulse dừng `onPause`, resume `onResume`.

---

### AT-VIP-09: Pulse animation khi VIP active

**Precondition:** User có VIP đang active.
**Verify:** `rowWatchAd` có thể bị ẩn hoặc pulse không chạy (tùy logic). Không crash.

---

## Module 4: FVipManagement — Crown Shimmer

### AT-VIP-10: Crown icon shimmer xoay nhẹ

**Precondition:** VIP screen đang mở.
**Verify:**
- `ivVipIcon` (crown/icon) xoay -5° → +5° qua lại
- Mượt mà, liên tục
- Duration và repeatCount theo code (`startCrownShimmer()`)

**Pass criteria:** Crown có hiệu ứng "lắc lư" nhẹ, không giật.

---

### AT-VIP-11: Crown shimmer DỪNG khi app vào background

**Expected:** Cancel `shimmerAnim` trong `onPause`, restart trong `onResume`.

---

## Module 5: FVipManagement — Celebration / Confetti

### AT-VIP-12: Celebration animation khi activate VIP bằng key hợp lệ

**Precondition:** User chưa có VIP, có key hợp lệ.
**Steps:**
1. Nhập key VIP hợp lệ vào EditText
2. Tap "Activate" / nhấn button confirm

**Expected (tất cả phải xảy ra liên tiếp):**
1. Crown icon animate: scale 0 → 1.2 → 1.0, alpha 0 → 1, duration ~1 giây
2. Confetti xuất hiện (particles bay từ trên xuống hoặc burst từ trung tâm)
3. Haptic feedback: `VibrationEffect.EFFECT_CLICK` (1 lần, ngắn)
4. UI cập nhật ngay: chip status chuyển sang "VIP Active", countdown bắt đầu

**Pass criteria:** Tất cả 4 bước xảy ra trong 2 giây.

---

### AT-VIP-13: Celebration animation khi earn reward từ Watch Ad

**Steps:**
1. Tap "Watch Ad → 3 Days VIP"
2. Xem ad đến cuối
3. Ad dismiss

**Expected:** Tương tự AT-VIP-12 — celebration + haptic + UI update ngay sau khi ad close.

---

### AT-VIP-14: Celebration KHÔNG xuất hiện khi key sai

**Steps:**
1. Nhập key sai (bất kỳ chuỗi random)
2. Tap confirm

**Expected:** Dialog lỗi hiện ra (từ SDK), KHÔNG có celebration animation, KHÔNG có haptic.

---

### AT-VIP-15: Confetti không memory leak sau khi dismiss

**Precondition:** Đã trigger celebration thành công.
**Steps:**
1. Chờ confetti kết thúc (~3 giây)
2. Navigate back về Settings
3. Mở VIP screen lại

**Expected:** Không có confetti "phantom" còn tồn tại, màn hình sạch.

---

## Module 6: FVipManagement — Progress Bar Animation

### AT-VIP-16: Progress bar animate khi VIP đang active

**Precondition:** User có VIP còn hiệu lực.
**Verify:**
- `progressVip` hiển thị đúng % thời gian còn lại
- Khi mở screen: progress animate từ 0 → target value (không nhảy đột ngột)
- Animation `animateProgress()` có easing

**Pass criteria:** Progress bar fill mượt từ trái sang phải đến đúng vị trí.

---

### AT-VIP-17: Progress bar update real-time

**Precondition:** VIP còn <1 giờ hiệu lực (hoặc test với key 3 ngày gần hết).
**Steps:**
1. Mở VIP screen, quan sát progress
2. Chờ 1 phút
3. Kiểm tra progress

**Expected:** Progress giảm nhẹ tương ứng với thời gian đã trôi. Countdown text cập nhật mỗi giây.

---

### AT-VIP-18: Progress bar ẩn khi Free user

**Precondition:** Không có VIP.
**Verify:** `progressVip` không visible. Không crash.

---

## Module 7: FVipManagement — Countdown Timer

### AT-VIP-19: Countdown hiển thị đúng định dạng

**Precondition:** VIP active với > 1 ngày còn lại.
**Verify:**
- Format: `Xd Xh Xm Xs` (ví dụ: `2d 14h 32m 15s`)
- Cập nhật mỗi giây (không skip giây)
- Không flash/nhấp nháy khi update

---

### AT-VIP-20: Countdown sử dụng CountDownTimer (không dùng Handler)

**Verify (code review):**
- `FVipManagement.kt` dùng `CountDownTimer`, không `Handler.postDelayed`
- `countDownTimer?.cancel()` được gọi trong `onDestroyView()`

**Pass criteria (runtime):** Mở VIP screen 30 giây → navigate back → không có crash hoặc NPE trong logcat.

---

### AT-VIP-21: Countdown dừng khi VIP expire

**Precondition:** VIP sắp hết (test với key giả có thời gian ngắn hoặc mock).
**Steps:**
1. Để countdown chạy đến 0

**Expected:**
- Countdown dừng
- UI switch về trạng thái Free
- Haptic feedback (nếu có implementation)
- KHÔNG crash

---

## Module 8: FVipManagement — Bottom Sheet Animation

### AT-VIP-22: Bottom sheet slide-in khi mở dialog nhập key

**Steps:**
1. Tap nút "Nhập mã VIP"

**Expected:** Bottom sheet xuất hiện từ dưới lên, animation mượt ~300ms.

---

### AT-VIP-23: Bottom sheet dismiss animation

**Steps:**
1. Mở bottom sheet (nhập key)
2. Swipe down hoặc tap outside để dismiss

**Expected:** Bottom sheet slide xuống và dismiss, không freeze.

---

### AT-VIP-24: Bottom sheet tự scroll khi keyboard mở

**Steps:**
1. Mở bottom sheet nhập key
2. Tap vào EditText → keyboard hiện

**Expected:**
- Nội dung bottom sheet scroll lên để EditText visible
- EditText không bị che bởi keyboard
- Khi dismiss keyboard, scroll về vị trí ban đầu

---

## Module 9: Splash Screen Animations

### AT-SPL-01: Fade-in animation cho app name và version

**Steps:**
1. Khởi động app từ launcher

**Expected:**
- `tvAppName` và `tvVersion` fade-in từ transparent (`splash_fade_in` anim)
- Duration: theo config trong anim XML
- Smooth, không flash

---

### AT-SPL-02: Slide-up animation cho bottom layout

**Expected:** `layoutBottom` slide từ dưới lên trong khi app name đang fade-in. Hai animation chạy song song, phối hợp tốt.

---

### AT-SPL-03: Splash transition sang ActivityMain

**Steps:**
1. Chờ splash finish (sau consent + App Open ad)

**Expected:**
- Transition từ Splash → Main mượt mà (không black flash)
- Handler `finishRunnable` cancel đúng nếu Activity bị destroy trước khi runnable fire

---

## Module 10: History / Camera Animations

### AT-CAM-01: Scan animation khi detect barcode

**Steps:**
1. Mở CameraActivity
2. Hướng camera vào barcode

**Expected:**
- `DetectorView` hiển thị overlay/highlight trên vùng barcode
- Animation nhanh, không lag camera

---

### AT-HIST-01: List item slide-in khi mở History

**Steps:**
1. Từ Main, chuyển sang tab History

**Expected:**
- Nếu có `roy_item_anim_slide_up.xml` / `roy_layout_anim_slide_up.xml`: items slide-in từ dưới
- Không giật khi scroll nhanh

---

## Module 11: Memory Leak — Animation Cleanup

### AT-MEM-01: Không có animator leak khi rotate device

**Steps:**
1. Mở VIP screen
2. Xoay device 3–4 lần (portrait ↔ landscape)

**Expected:**
- KHÔNG crash với `IllegalStateException` từ animator
- Logcat không có `ObjectAnimator` warnings
- Các animations restart clean sau mỗi lần xoay

---

### AT-MEM-02: Không có leak khi switch fragment liên tục

**Steps:**
1. Mở VIP screen từ Settings
2. Back → mở lại VIP → back → mở lại (5 lần)

**Expected:**
- KHÔNG OutOfMemoryError
- Animator objects được cancel và GC sau mỗi lần `onDestroyView`
- Verified qua `FVipManagement.onDestroyView()` gọi: `shimmerAnim?.cancel()`, `heroGlowAnim?.cancel()`, `countDownTimer?.cancel()`

---

### AT-MEM-03: Không có animator leak khi app background/foreground liên tục

**Steps:**
1. Mở VIP screen
2. Home → app → Home → app (10 lần nhanh)

**Expected:**
- Không crash
- Animations resume đúng sau mỗi lần foreground
- Không có multiple animator instances chạy song song

---

## Checklist Tóm Tắt

| Module | Test Case | Kết Quả | Ghi chú |
|--------|-----------|---------|---------|
| Stagger Entrance | AT-VIP-01 | | |
| Stagger Entrance | AT-VIP-02 | | |
| Fragment Slide-in | AT-VIP-03 | | |
| Hero Glow | AT-VIP-04 | | |
| Hero Glow | AT-VIP-05 | | |
| Hero Glow | AT-VIP-06 | | |
| Watch Ad Pulse | AT-VIP-07 | | |
| Watch Ad Pulse | AT-VIP-08 | | |
| Watch Ad Pulse | AT-VIP-09 | | |
| Crown Shimmer | AT-VIP-10 | | |
| Crown Shimmer | AT-VIP-11 | | |
| Celebration | AT-VIP-12 | | |
| Celebration | AT-VIP-13 | | |
| Celebration | AT-VIP-14 | | |
| Celebration | AT-VIP-15 | | |
| Progress Bar | AT-VIP-16 | | |
| Progress Bar | AT-VIP-17 | | |
| Progress Bar | AT-VIP-18 | | |
| Countdown | AT-VIP-19 | | |
| Countdown | AT-VIP-20 | | |
| Countdown | AT-VIP-21 | | |
| Bottom Sheet | AT-VIP-22 | | |
| Bottom Sheet | AT-VIP-23 | | |
| Bottom Sheet | AT-VIP-24 | | |
| Splash | AT-SPL-01 | | |
| Splash | AT-SPL-02 | | |
| Splash | AT-SPL-03 | | |
| Camera | AT-CAM-01 | | |
| History | AT-HIST-01 | | |
| Memory Leak | AT-MEM-01 | | |
| Memory Leak | AT-MEM-02 | | |
| Memory Leak | AT-MEM-03 | | |
