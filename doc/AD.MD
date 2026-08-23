# Tích Hợp AdmobApplovinWrapper — Trạng Thái Hiện Tại

> Cập nhật: 2026-08-23 | Migration: ✅ HOÀN TẤT | Provider switch (AppLovin→AdMob): ✅ HOÀN TẤT
> Lib: `com.github.royt93:AdmobApplovinWrapper:1.6.21` (JitPack, tag mới nhất — nâng từ 1.1.3)
> `sdkadbmob/AdMobManager.kt` cũ đã **xoá hẳn** (commit `2af8f8a`), thay bằng wrapper.

---

## 1. Cấu Hình SDK Hiện Tại

| Field | Giá trị |
|---|---|
| Provider active | **AdMob** (`IS_ENABLE_ADMOB = true`, cả debug lẫn release) — AppLovin field vẫn khai (mediation/fallback bắt buộc theo SDK) |
| AdMob App ID (release) | `ca-app-pub-3004713799155145~6517489616` |
| AdMob App ID (debug) | `ca-app-pub-3940256099942544~3347511713` (Google test App ID) |
| AdMob Banner/Interstitial/AppOpen/Rewarded (release) | Xem `doc/admob/admob.MD` + `myKeyStore/.../keystore.properties` (`admob*Release`) |
| AdMob Banner/Interstitial/AppOpen/Rewarded (debug) | Google demo test unit ID — `gradle.properties` (`ADMOB_*_DEBUG`) |
| AppLovin SDK Key + 4 ad unit ID | Giữ nguyên trong `keystore.properties` — bắt buộc khai dù không active runtime |
| `allowLegacyPlaintextVipKey` | `true` — bắt buộc set khi nâng SDK ≥1.2.0, vì `FVipManagement` vẫn dùng `activateVipByKey` plaintext (chưa migrate token ECDSA); SDK mặc định tắt nhánh này |

> **Release gate còn thiếu (ngoài code, phải làm thủ công trên dashboard trước khi ship):**
> AdMob Console → Privacy & messaging → European regulations → tạo + **Publish** consent message cho đúng App ID `ca-app-pub-3004713799155145~6517489616` (nếu chưa làm, UMP có thể trả `Publisher misconfiguration`, fresh install fail-closed). Nếu có traffic Mỹ, làm thêm "US states" message tương tự. Xem Step 8.3 trong `doc/AD_PROMPT_AOS.MD`.
> Native ad unit (`ca-app-pub-3004713799155145/8952081261`, trong `admob.MD`) **KHÔNG dùng** — SDK wrapper không có API Native Ad, đã quyết định bỏ qua.

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
| **Banner** | `ActivityCamera.kt:167` | `onCreate` | `AdManager.loadBanner(...)` | View gắn vào `adView` |
| **Interstitial** | `ActivityCamera.kt:174` | `onCreate` (preload) | `AdManager.loadInterstitial(this)` | Show tại lines 326, 334, 370 |
| **App Open** | `ActivitySplash.kt:25,61` | `requestConsentInfoUpdate` callback | `AdManager.awaitSplashComplete(this)` | Block nav đến Main |
| **Rewarded** | `FVipManagement.kt:561` | User click "Watch Ad" | `AdManager.showRewarded(...)` | Preload tại `:187`; fallback → interstitial nếu fail |

---

## 3. Kiến Trúc Hiện Tại

```
RApp.onCreate()
  └── setupAdSystem()
        ├── AdSdkConfig(isEnableAdmob=true, admob*Id + applovin*Id, vipKeySecret,
        │               allowLegacyPlaintextVipKey=true, safety)
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
  └── AdManager.showRewarded(activity) { earned ->
        if (earned) dedup(rewardTransactionId) → AdManager.grantVipDays(ctx, 3) → saveVipMeta(3)
      }
  (rewardedListener.onUserEarnedReward: CHỈ log/UI, KHÔNG cấp thưởng — 1 kênh cấp duy nhất là
  callback showRewarded, đúng khuyến nghị doc Bước 4 "chỉ cộng thưởng ở MỘT kênh")

  (rowWatchAd.visibility = GONE khi isVipByKeyActive()==true, set trong refreshStatus() — SDK tự
  skip showRewarded cho VIP member; KHÔNG cần fallback/custom code nào cho case này, dùng đúng
  hành vi SDK sẵn có + ẩn UI tương ứng. Bản fix trước (interstitial fallback + dialog cảm ơn tự
  chế) đã bị xoá 2026-08-23 vì đó là dead code — xem mục 6 #13.)
```

---

## 4. So Sánh Trước / Sau Migration

| Tính Năng | Trước (AdMobManager cũ) | Sau (AdmobApplovinWrapper) |
|---|---|---|
| **Provider** | AdMob (Google) | **AdMob (Google), qua wrapper** — AppLovin field vẫn khai làm mediation/fallback |
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

| # | Issue | Priority | Status |
|---|---|---|---|
| 1 | `ActivityCamera.kt` còn stale imports AdMob SDK | 🟡 LOW | ✅ FIXED — đã xoá hết import `com.google.android.gms.ads.*` |
| 2 | In-App Purchase integration cho các gói VIP trả phí | 📋 Planned | ⏸️ Chờ M1 (Play Billing) |
| 3 | `RApp.kt` + `ActivityCamera.kt` còn `Log.d("roy93~")` debug | 🟡 LOW | ✅ FIXED — đã bọc `if (BuildConfig.DEBUG)` |
| 4 | Class name không theo convention (`CameraActivity` vs file `ActivityCamera.kt`) | 🟡 LOW | ✅ FIXED — class đã rename thành `ActivityCamera` |
| 5 | `ext/Activity.kt` (`rateAppInApp`) còn 9 `Log.*("roy93~")` unconditional | 🟡 LOW | ✅ FIXED — chuyển sang `SafeLogger` (wrapper, tự gate DEBUG) |
| 6 | SDK 1.1.3 → 1.6.21 (latest), bật `IS_ENABLE_ADMOB=true` debug+release | 🟢 DONE | ✅ FIXED — API surface đối chiếu trực tiếp source GitHub tag 1.6.21, không breaking cho call site hiện tại |
| 7 | `activateVipByKey` (plaintext) bị SDK ≥1.2.0 tắt mặc định (`allowLegacyPlaintextVipKey=false`) | 🔴 HIGH (silent VIP break) | ✅ FIXED — set `allowLegacyPlaintextVipKey=true` trong `RApp.kt`; chưa migrate sang VIP token ECDSA |
| 8 | Lint `FullBackupContent` (`path="."` invalid trên `<include domain="root">`) chặn `assembleProductionRelease` | 🔴 HIGH (release build fail, pre-existing từ SEC-03) | ✅ FIXED — bỏ `<include domain="root" path=".">` thừa (root đã default include) trong `backup_rules.xml` + `extraction_rules.xml`, giữ nguyên `<exclude domain="database">` |
| 9 | AdMob App ID hard-code 1 giá trị sample chung debug/release trong Manifest | 🟡 MEDIUM | ✅ FIXED — tách qua `manifestPlaceholders["admobAppId"]` theo build type |
| 10 | AdMob Console — publish European regulations (+ US states nếu cần) consent message | 📋 Cần làm thủ công | ⏸️ **CHƯA LÀM** — ngoài phạm vi code, xem cảnh báo ở mục 1 |
| 11 | Thiếu `<attribution>` tag (targetSdk 37 ≥31, Step 6 AD_PROMPT_AOS.MD) | 🟡 MEDIUM | ✅ FIXED — thêm `<attribution android:tag="ads" .../>` vào Manifest, phát hiện qua review độc lập (codex/agy/claude) |
| 12 | `AdSdkConfig` thiếu `appOpenExcludedActivities` — class Splash tên `ActivitySplash` không khớp default deprecated `"SplashActivity"`, App Open không được exclude khỏi Splash | 🟠 MEDIUM-HIGH | ✅ FIXED — thêm `appOpenExcludedActivities = listOf(ActivitySplash::class.java)` trong `RApp.kt`, phát hiện qua review độc lập |
| 13 | **Step 7 rule 6 VI PHẠM: `FVipManagement.kt` nhánh user-đã-VIP cấp +3 ngày VIP khi show interstitial (không phải rewarded)** | 🔴 CRITICAL (policy/ban-account risk) | ✅ FIXED (v2) — Fix v1 (bỏ cấp thưởng, giữ interstitial + dialog "cảm ơn" tự chế) bị chính user chỉ ra SAI hướng: tự viết code custom thay vì tin SDK. Đối chiếu `refreshStatus()`: `rowWatchAd.visibility = GONE` khi `isVipByKeyActive()==true` → nhánh `if (isVipByKeyActive())` trong `setupWatchAdRow()` là **dead code** (View GONE không nhận click). Fix v2: **xoá hẳn nhánh đó**, không còn fallback/dialog tự chế nào — `setupWatchAdRow()` chỉ còn gọi `AdManager.showRewarded(...)`. Xác nhận qua review độc lập vòng 2 (codex + agy + claude, cả 3 đồng ý 100%, không tìm thấy race condition nào lách được UI-hide; claude còn decompile SDK xác nhận `showRewarded()` tự bail nếu `isVip()==true` — defense-in-depth ở tầng SDK) |
| 14 | **Bug thật (không phải chỉ style): `onUserEarnedReward` dùng `activateVipByKey()` (có `networkChecker` gate, chỉ dành cho flow nhập-key) thay vì `grantVipDays()` (đúng API SDK cho reward-ad, không cần mạng, có atomic race-guard)** | 🔴 HIGH (mất reward khi mạng chập chờn ngay sau khi xem xong ad) | ✅ FIXED — đổi `FVipManagement.kt` dòng ~615 sang `AdManager.grantVipDays(context, 3)`. `btnSubmitKey` (dòng 461, flow nhập-key thủ công) giữ nguyên `activateVipByKey` — đúng mục đích. Phát hiện qua review độc lập vòng 2 (claude), verify lại bằng cách đọc trực tiếp source SDK 1.6.21 (`AdManager.kt:3803` có `networkChecker` gate, `:4066` `grantVipDays` dùng `addVipDaysAtomic` không gate mạng) |
| 15 | Reward cấp qua `rewardedListener.onUserEarnedReward` thay vì callback `showRewarded`, không dùng `rewardTransactionId` dedup — ngược khuyến nghị kênh của doc (Bước 4) | 🟡 LOW | ✅ FIXED — chuyển `grantVipDays`+`saveVipMeta` sang callback `showRewarded(earned=true)`, thêm dedup qua `AdManager.rewardTransactionId` (pref `vip_last_reward_tx`); `rewardedListener.onUserEarnedReward` giờ chỉ log. **Lưu ý:** verify qua source thật (`AppLovinProvider.kt:867-880`) — khi `grantRewardOnEarn=true` (default), cả 2 kênh fire ĐỒNG THỜI lúc ad còn che màn hình (không phải lúc dismiss), nên đổi kênh không đổi timing, chỉ đúng convention hơn — giữ nguyên pattern `pendingVipReward` (đợi `onResume` mới show dialog) |
| 16 | Nhánh dedup-reject (`txId` null/trùng) hoặc `grantVipDays` fail (overflow) không reset `rowWatchAd.isClickable`/text — nút kẹt "Đang mở…" mãi tới lần load ad kế | 🟡 LOW (edge-case hiếm, không trúng trong smoke test 15 phút) | ✅ FIXED — cả 2 nhánh giờ reset về `vip_plan_watch_sub_retry` + `isClickable=true`, đối xứng với nhánh `earned=false`. Phát hiện qua review độc lập gate-cuối (claude), 2/3 nguồn khác (codex/agy) bỏ sót |

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
