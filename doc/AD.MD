# Tích Hợp AdmobApplovinWrapper — Trạng Thái Hiện Tại

> Cập nhật: 2026-06-22 | Migration: ✅ HOÀN TẤT
> Lib: `com.github.royt93:AdmobApplovinWrapper:1.1.3`

---

## 1. Cấu Hình SDK Hiện Tại

| Field | Giá trị |
|---|---|
| Provider active | **AppLovin MAX** (`IS_ENABLE_ADMOB = false`) |
| AppLovin SDK Key | `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt` |
| Banner Ad Unit | `84d187d1b08adc3d` |
| Interstitial Ad Unit | `5ac61198c318d67b` |
| App Open Ad Unit | `6569037b86c0256e` |
| Rewarded Ad Unit | `671ca9067e78cc47` |

### VIP Keys (Base64 encoded — KHÔNG đổi nếu không muốn invalidate keys cũ)

| Key | Plain | Base64 |
|---|---|---|
| 30 Days | `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` | `OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA==` |
| 3 Days | `eQ7@93L0f!2Y2707xN04021993u0I#2aK` | `ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL` |

`vipKeySecret` trong `RApp.setupAdSystem()` = Base64 của key 30 ngày.

---

## 2. Touchpoint Table (Trạng Thái Sau Migration)

| Slot | File | Trigger | API hiện tại | Ghi chú |
|---|---|---|---|---|
| **Banner** | `ActivityCamera.kt:161` | `onCreate` | `AdManager.loadBanner(...)` | View gắn vào `adView` |
| **Interstitial** | `ActivityCamera.kt:168` | `onCreate` (preload) | `AdManager.loadInterstitial(this)` | Show tại lines 320, 332, 372 |
| **App Open** | `ActivitySplash.kt` | `requestConsentInfoUpdate` callback | `AdManager.awaitSplashComplete(this)` | Block nav đến Main |
| **Rewarded** | `FVipManagement.kt:524` | User click "Watch Ad" | `AdManager.showRewarded(...)` | Fallback → interstitial nếu fail |

---

## 3. Kiến Trúc Hiện Tại

```
RApp.onCreate()
  └── setupAdSystem()
        ├── AdSdkConfig(applovinSdkKey, banner/inter/appOpen/rewarded IDs, vipKeySecret, safety)
        └── AdManager.initialize(this) { success, gaid -> ... }

ActivitySplash.onCreate()
  ├── AdManager.requestConsentInfoUpdate(activity, tagForUnderAgeOfConsent=false) { canRequestAds ->
  │     if (canRequestAds) → runSplashAdFlow()
  │     else → goToMain()
  │   }
  └── runSplashAdFlow() [CoroutineScope Main]
        └── AdManager.awaitSplashComplete(this) → goToMain()

CameraActivity.onCreate()
  ├── AdManager.loadBanner(...)   → adView
  └── AdManager.loadInterstitial(this)

CameraActivity [scan decoded]
  └── AdManager.showInterstitial(this) { success -> ... }

FVipManagement [Watch Ad button]
  └── AdManager.showRewarded(..., listener)
        ├── onUserEarnedReward → activateVip(3 days)
        └── onAdFailedToLoad/Show → showInterstitial fallback → activateVip(3 days)
```

---

## 4. So Sánh Trước / Sau Migration

| Tính Năng | Trước (AdMobManager cũ) | Sau (AdmobApplovinWrapper) |
|---|---|---|
| **Provider** | AdMob (Google) | AppLovin MAX (Admob disabled) |
| **Kiến trúc** | Monolith, hardcode, self-managed lifecycle | Tách biệt, Coroutine-based, auto-lifecycle |
| **App Open** | Manual resume check, dễ crash/leak | Auto via `ProcessLifecycleOwner` trong lib |
| **UMP Consent** | Không có | `requestConsentInfoUpdate` ở Splash |
| **Ad Safety** | Thô sơ | `AdSafetyLimits` với 7 lớp bảo vệ + frequency cap |
| **Frequency Cap** | Không có | Built-in qua `AdSafetyLimits.TEST` (debug) / default (release) |
| **VIP System** | Không có | `AdManager.activateVipByKey(base64)`, countdown, UI đầy đủ |
| **Memory Leak** | Handler ẩn danh, Activity strong ref | WeakReference, `splashScope?.cancel()` trong `onDestroy` |
| **Rewarded Ad** | Không có | Tích hợp trong `FVipManagement` với fallback interstitial |
| **Banner Lifecycle** | Manual pause/resume/destroy | Auto qua lib |

---

## 5. VIP Management Screen — Trạng Thái

**Fragment:** `FVipManagement.kt`
**Layout:** `roy_frm_vip_management.xml`
**Entry point:** Settings screen (`FPreferences`) → VIP menu item

### Components đã implement (9/9)

| # | Thành phần | Status |
|---|---|---|
| 1 | Status header (chip + crown icon) | ✅ `chipStatus`, `ivVipIcon` |
| 2 | Activation date row | ✅ `tvVipActivated` |
| 3 | Expiry date row | ✅ `tvVipExpiry` |
| 4 | Progress bar | ✅ `progressVip` với `animateProgress()` |
| 5 | Countdown text (`d h m s`) | ✅ `tvCountdown` via `CountDownTimer` |
| 6 | Button "Nhập mã VIP" | ✅ `etVipKey` + MaterialAlertDialog |
| 7 | Button "Watch ad → 3 days VIP" | ✅ `setupWatchAdRow()` |
| 8 | Toolbar badge VIP | ✅ Hiển thị chip vàng trên toolbar |
| 9 | Cancel VIP button | ✅ `setupCancelButton()` + confirm dialog |

### Animations đã implement (5/5)

| # | Animation | Impl | Status |
|---|---|---|---|
| 1 | Stagger entrance | `runStaggerEntrance()` — alpha 0→1, translateY 40→0 | ✅ |
| 2 | Hero glow rotation | `startHeroGlowRotation()` — 360° / 12s, infinite, cancel onPause | ✅ |
| 3 | Watch ad pulse | `startWatchAdPulse()` — scale 1→1.05→1, infinite | ✅ |
| 4 | Crown shimmer | `startCrownShimmer()` — rotate ±5°, repeat | ✅ |
| 5 | Confetti celebration | `playCelebration()` + `showConfetti()` + haptic | ✅ |

### Purchase Options (In-App Purchase — PENDING)

| Gói | Giá | Status |
|---|---|---|
| Watch Ad | Free | ✅ Hoạt động |
| VIP 30 ngày | ~$0.50 | ⏸️ Disabled (chờ IAP integration) |
| VIP 90 ngày | ~$1.00 | ⏸️ Disabled |
| VIP 1 năm | ~$2.00 | ⏸️ Disabled |
| VIP Lifetime | ~$3.00 | ⏸️ Disabled |

---

## 6. Open Issues / Cần Làm

| # | Issue | Priority |
|---|---|---|
| 1 | `ActivityCamera.kt` còn stale imports AdMob SDK (lines 25–28) | 🟡 LOW |
| 2 | In-App Purchase integration cho các gói VIP trả phí | 📋 Planned |
| 3 | `RApp.kt` và `ActivityCamera.kt` còn `Log.d("roy93~", ...)` debug trong production | 🟡 LOW |
| 4 | `ActivityCamera` class name không theo convention (`CameraActivity` vs file `ActivityCamera.kt`) | 🟡 LOW |

---

## 7. AdSafetyLimits Config

```kotlin
// Trong RApp.setupAdSystem():
safety = if (BuildConfig.DEBUG) AdSafetyLimits.TEST else AdSafetyLimits()
```

| Field | Debug (`TEST`) | Release (default) |
|---|---|---|
| Interstitial min interval | ~10s | ~60s |
| App Open min interval | ~10s | ~120s |
| Max per session | Không giới hạn | Theo default lib |
