# 🚀 Kế Hoạch Tích Hợp AdmobApplovinWrapper & VIP Management

## 1. Thông tin cấu hình SDK
- **Provider**: AppLovin MAX (`IS_ENABLE_ADMOB = false`)
- **AppLovin SDK Key**: `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt`
- **Banner Ad**: `84d187d1b08adc3d`
- **Interstitial Ad**: `5ac61198c318d67b`
- **App Open Ad**: `6569037b86c0256e`
- **Rewarded Ad**: `671ca9067e78cc47`

### VIP Keys (Base64 Encoded trong code)
- **30 Days Key**: `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` -> Base64: `OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA==`
- **3 Days Key**: `eQ7@93L0f!2Y2707xN04021993u0I#2aK` -> Base64: `ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL`
- **Secret Key System**: Sẽ cấu hình `AdSdkConfig.vipKeySecret` thành `OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA==` (cho 30 ngày) và xử lý riêng biệt key 3 ngày.

## 2. Các Bước Triển Khai (Touchpoints)

### Bước 1: Setup & Dependency
- Cập nhật `build.gradle` thêm JitPack repository và dependency `com.github.royt93:AdmobApplovinWrapper:1.1.3`.
- Xoá thư mục `sdkadbmob` cũ chứa `AdMobManager.kt` monolith để dọn dẹp code và tránh xung đột (không bị memory leak như bản cũ).

### Bước 2: App Initialization & UMP Consent (`RApp.kt` & `ActivitySplash.kt`)
- Trong `RApp.kt`: Gọi `AdManager.setConfig(AdSdkConfig(...))` và `AdManager.initialize`.
- Thêm logic **VIP 1 ngày tự động kích hoạt** ở lần mở app đầu tiên (check `SharedPreferences`).
- Trong `ActivitySplash.kt`: Gọi `AdManager.requestConsentInfoUpdate` để hỏi UMP Consent (bắt buộc cho EEA). Sau khi user đồng ý (hoặc không ở EEA), chạy coroutine `AdManager.awaitSplashComplete` để chờ load App Open Ad rồi vào `MainActivity`.
- Vòng đời App Open khi user chuyển từ background về foreground sẽ được SDK quản lý tự động.

### Bước 3: VIP Management UI/UX
- Thêm section mới trong màn hình Cài Đặt (Preferences) trỏ tới `FVipManagement.kt` (Fragment).
- UI bao gồm:
  - Text view trạng thái VIP (Ví dụ: `VIP Active: 29 days left` hoặc `Free User`).
  - Nút **Nhập mã VIP**: Mở dialog cho phép user dán mã Base64 (30 ngày hoặc 3 ngày). Code sẽ decode và kích hoạt qua `AdManager.activateVipByKey`.
  - Nút **Watch Ad for 3 Days VIP**: Gọi `AdManager.loadRewarded` và `AdManager.showRewarded`. Nếu xem xong -> `onUserEarnedReward` -> kích hoạt VIP 3 ngày. Nếu rewarded load lỗi -> fallback qua `AdManager.showInterstitial`.

### Bước 4: Banner Ad (`ActivityCamera.kt`)
- Xoá setup banner cũ.
- Thêm include `@layout/layout_ad_banner` vào `ActivityCamera.kt`.
- Sử dụng `AdManager.loadBanner` để SDK tự động handle lifecycle (pause/resume/destroy), đảm bảo không leak memory.

### Bước 5: Interstitial Ad (`ActivityCamera.kt`)
- Chỉ thay thế những chỗ đang gọi `AdMobManager.showInterstitial` cũ (sau khi scan hoặc các luồng tương tự) thành `AdManager.showInterstitial`. SDK đã lo AdSafety, Frequency Capping chống click tặc.

## 3. So Sánh Trước & Sau Migrate

| Tính Năng | Trước Khi Migrate (`AdMobManager` cũ) | Sau Khi Migrate (`_AdmobWrapper`) |
| --- | --- | --- |
| **Provider** | AdMob (Google) | AppLovin MAX |
| **Kiến trúc** | Monolith, hardcode, tự quản lý vòng đời | Tách biệt, Coroutine-based, Auto-Lifecycle |
| **App Open** | Dễ crash/leak, manual resume check | Tự động via ProcessLifecycleOwner |
| **UMP Consent** | Không có | Có sẵn, tích hợp chặt chẽ ở Splash |
| **Ad Safety** | Thô sơ | Tích hợp 7 lớp bảo vệ, Frequency Capping |
| **VIP System** | Không có | Chặn ad toàn diện, tính expiry time chính xác |

## 4. Open Questions / Thông Tin Cần Xác Nhận
Tôi đã có đủ ID và Key bạn cung cấp. Tuy nhiên, tôi cần hỏi:
1. Bạn muốn import thư viện qua JitPack (mạng) hay module nội bộ (`include ':AdmobWrapper'`)? (Tôi sẽ mặc định dùng JitPack vì nó dễ quản lý nhất).
2. Mã VIP 3 ngày và 30 ngày sẽ dùng để activate `vipByKey`. Thư viện chỉ hỗ trợ 1 `vipKeySecret` cho thời gian cấu hình mặc định, nên tôi sẽ dùng logic Custom để cấp VIP tương ứng với số ngày tùy theo key nhập vào. Bạn đồng ý chứ?

## 5. UI VIP — Premium polish (2026-04-27)

Đợt nâng cấp UI để hướng tới "xịn nhất có thể" theo Step 10 của `doc/AD_PROMPT_AOS.MD`.

### Layout `roy_frm_vip_management.xml`

| Component | Trước | Sau |
|---|---|---|
| Hero header | Gradient blue-grey + crown 3D + countdown | + Status chip ("VIP ACTIVE" gold / "TÀI KHOẢN MIỄN PHÍ" neutral) trên đỉnh, + radial glow xoay (12s/loop) sau crown, + dòng hint "Nâng cấp VIP để mở khoá toàn bộ tính năng" cho user free |
| Status chip | Không có | `chipStatus` chuyển nền `roy_bg_status_chip_gold` ↔ `roy_bg_status_chip` theo state |
| Đặc quyền VIP | Không có | Card `featuresCard` (4 mục: tắt ad / quét không giới hạn / hỗ trợ ưu tiên / tính năng độc quyền) với check icon vàng |
| Plan icons | Emoji 👑💎⭐🔮 trong TextView | Vector drawable (`ic_play_circle_filled`, `ic_gift_box`, `ic_diamond_premium`, `ic_star_premium`, `ic_infinity`) trên các circle gradient màu (green/blue/purple/gold/pink) |
| Plan badges | Chỉ "MIỄN PHÍ" cho Watch Ad | + "−33%" / "−67%" cho 90d / 1Y, + "BEST VALUE" gradient tím cho Lifetime |
| Plan card bg | `bg_liquid_glass_dark` chung | Active row `roy_bg_vip_card_active` (gold tint), locked rows `roy_bg_vip_card_locked` (neutral, lock icon `ic_lock_outline`) |
| Section header | TextView dọc | Divider gradient hai bên + label vàng letterSpacing |

### Animation mới (theo Step 10.2)

| # | Animation | Cấu hình |
|---|---|---|
| 1 | Stagger entrance | `runStaggerEntrance()` — alpha 0→1 + translationY 40→0, 380ms, delay 70ms cho mỗi row (`featuresCard`, `rowWatchAd`, `planRow30`, `planRow90`, `planRow1Y`, `planRowLifetime`) |
| 2 | Hero glow rotation | `viewHeroGlow` xoay 360° trong 12s, infinite, linear, lifecycle-bound (cancel onPause) |
| 3 | Watch ad pulse | Khi free user, `rowWatchAd` scale 1.0↔1.02 mỗi 1.5s; cancel khi VIP active hoặc onPause |
| 4 | Crown shimmer | (giữ) rotate ±5° mỗi 800ms |
| 5 | Confetti + scale celebration | (giữ) khi `activateVipByKey` thành công |
| 6 | Glow sweep progress | (giữ) khi VIP active |

### Drawable mới

- Vector icons: `ic_play_circle_filled`, `ic_gift_box`, `ic_diamond_premium`, `ic_star_premium`, `ic_infinity`, `ic_lock_outline`, `ic_check_circle_gold`, `ic_shield_check`, `ic_bolt`, `ic_rocket`, `ic_sparkle`.
- Backgrounds: `roy_bg_savings_badge`, `roy_bg_best_value`, `roy_bg_features_card`, `roy_bg_vip_card_active`, `roy_bg_vip_card_locked`, `roy_bg_section_divider`, `roy_bg_hero_overlay`, `roy_bg_status_chip`, `roy_bg_status_chip_gold`, `roy_bg_icon_circle_{gold,purple,blue,silver,pink}`.

### Memory leak rule (Step 10.3)

Tất cả animator mới (`heroGlowAnim`, `watchAdPulseAnim`) đều cancel ở `onPause` và `onDestroyView`. Listener vẫn dùng `WeakReference<Fragment>`. Không thêm `Handler.postDelayed` cho countdown.

### Build verify

`./gradlew assembleDevDebug` → BUILD SUCCESSFUL (9s, không lỗi mới).
