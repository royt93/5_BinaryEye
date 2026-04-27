# Prompt Template: Migrate hệ thống Ad sang `_AdmobWrapper` (Android native)

---

## 0. Bối cảnh — fill trước khi chạy

| Field | Giá trị |
|---|---|
| Project hiện tại (cwd, absolute path) | `<PROJECT_PATH>` |
| Lib reference (absolute path) | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper` |
| File spec gốc của lib | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper/doc/AD.MD` |
| Lib coordinate + version | `<GROUP:ARTIFACT:VERSION>` (vd `com.github.royt93:AdmobApplovinWrapper:1.1.3`) |
| Build variant phải xanh (debug) | `<assemble{Flavor}Debug>` |
| Build variant phải xanh (release) | `<assemble{Flavor}Release>` |
| Provider mặc định | `<AdMob \| AppLovin MAX>` (xem `BuildConfig.IS_ENABLE_ADMOB` trong project) | Ưu tiên dùng applovin
| VIP key 30 ngày (plain) | `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` |
| VIP key 3 ngày (plain) | `eQ7@93L0f!2Y2707xN04021993u0I#2aK` |
| Mức che giấu VIP key trong code | `<Base64 \| AES \| NDK>` (mặc định Base64) |

Tôi sẽ cung câp thông tin admob/applovin
***ADMOB: bạn tự check ở project hiện tại
***APPLOVIN: bạn tự check ở project hiện tại, tôi bổ sung thêm
sdk key: e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt
ad id banner: cần hỏi
ad id interstitial: cần hỏi
ad open app: cần hỏi
ad rewarded: cần hỏi
Nếu user không đưa đủ các key trên, hãy lập tức hỏi owner để bổ sung ngay 
Cố gắng che giấu các thông tin nhạy cảm này nhé, có thể dùng gradle.properties 

---

## Cấm (áp dụng mọi project)

- Tự ý thêm ad slot mới ngoài các slot đã tồn tại trong code hiện tại.
- Tự ý refactor code không liên quan đến migrate ad.
- Tự ý nâng version lib hoặc đổi flag `IS_ENABLE_ADMOB`.
- Lặp lại đọc cùng 1 file > 1 lần với mục đích "double check" — review trên kết quả đã có, không re-read.
- Tự thêm trigger interstitial mới (vd "show sau mỗi action X") nếu code cũ chưa có.

---

## Step 1 — Đọc lib reference

- `ls` đệ quy `<LIB_PATH>` để nắm cấu trúc.
- Đọc `<LIB_PATH>/doc/AD.MD` (spec gốc) trước.
- Đọc sample app trong lib (thư mục `app/` hoặc `sample/`), đặc biệt các call site: `AdManager.setConfig`, `initialize`, `requestConsentInfoUpdate`, `awaitSplashComplete`, `loadBanner`, `showInterstitial`, `loadRewarded`, `showRewarded`, `activateVipByKey`, `AdSafetyLimits`.
- Lưu ý AdSafetyLimits ở mode debug cần value dễ dãi hơn cho QC dễ test
**Output bắt buộc**: 1 đoạn ≤ 10 dòng tóm tắt API surface của lib và lifecycle nó tự quản (App Open auto qua `ProcessLifecycleOwner`, banner auto pause/resume/destroy, frequency cap…).

## Step 2 — Audit hiện trạng project

Trả lời 5 câu (yes/no + dẫn chứng `file:line`):

1. Project đã apply `_AdmobWrapper` chưa? Hay vẫn còn SDK cũ (vd `AdMobManager`, `AdsHelper`, `MaxManager`…)?
2. `Application.onCreate` đang gọi gì để init ad?
3. Splash activity có gọi consent + App Open warmup chưa?
4. Activity nào đang load banner và interstitial bằng API nào (`AdView`, `InterstitialAd.load`, `MaxAdView`…)?
5. Đã có VIP screen chưa? Nếu có thì UI gồm gì?

## Step 3 — Viết kế hoạch migrate vào `<PROJECT_PATH>/doc/AD.MD`

File đích: `<PROJECT_PATH>/doc/AD.MD` (tạo mới nếu chưa có).

Bắt buộc có các section:

**(1) Touchpoint table** — mỗi slot 1 dòng. Nếu slot chưa tồn tại trong project, ghi `(chưa có)` ở cột "API cũ thay thế":

| Slot | File hiện tại | Trigger | API mới | API cũ thay thế |
|---|---|---|---|---|
| Banner | `<host activity/fragment>` | `<onCreate / onViewCreated>` | `AdManager.loadBanner` | `<API cũ>` |
| Interstitial | `<host>` | `<trigger giữ nguyên>` | `AdManager.showInterstitial` | `<API cũ>` |
| App Open | Splash + foreground resume | warmup ở splash + `ProcessLifecycleOwner` cho resume | `AdManager.awaitSplashComplete` | `<API cũ hoặc (chưa có)>` |
| Rewarded | VIP screen, nút "Watch ad → 3 days VIP" | user click | `AdManager.loadRewarded` + `showRewarded`, fallback `showInterstitial` | `<API cũ hoặc (chưa có)>` |

**(2) Step-by-step migrate plan** — 6 bước (Setup, App Init + Consent, VIP UI, Banner, Interstitial, Rewarded). Mỗi bước nói rõ file edit, hàm thêm, dòng xoá.

**(3) Bảng so sánh trước/sau** — tối thiểu 8 dòng (Provider, Architecture, App Open, Consent, Memory leak risk, Frequency cap, VIP, Lifecycle).

**(4) Thông tin cần user cấp** — list rõ những gì còn thiếu: SDK key thật, ad unit ID thật, test device ID, EEA test config, banner placement layout id…

## Step 4 — Lifecycle requirement (rule cứng, áp dụng mọi project)

- **App Open ở Splash**: gọi `requestConsentInfoUpdate` xong → `awaitSplashComplete` → mới navigate sang main. **KHÔNG** show App Open trên splash thuần (vi phạm AdMob policy "Disallowed interstitial implementations").
- **App Open khi quay lại foreground**: dùng cơ chế của lib (đã hook `ProcessLifecycleOwner`). KHÔNG tự subscribe `ON_START` lần thứ 2 trong project — tránh double-show.
- **Banner**: gắn vào layout container có sẵn (`<R.id.banner_container>` hoặc tương đương). KHÔNG gọi `pause()/resume()/destroy()` thủ công — lib tự lo.
- **Interstitial**: chỉ thay call site cũ, không thêm trigger mới. Tin tưởng `AdSafetyLimits` để chống click-tặc.
- **Rewarded**: chỉ load + show khi user bấm nút trong VIP screen.

## Step 5 — Phạm vi thay thế

Chỉ thay đúng các call site cũ. Trước khi xoá file/hàm SDK cũ, **grep verify** không còn ai dùng:

- Liệt kê file SDK cũ sẽ xoá (vd `AdMobManager.kt`, `AdsHelper.kt`…).
- Với mỗi file: grep tên class/hàm trong toàn project, xác nhận 0 reference, rồi mới xoá.
- Không tự thêm banner/interstitial/rewarded ở activity/fragment khác.

## Step 6 — Build & memory check

- Chạy 2 build variant đã chốt ở Section 0 — cả 2 phải xanh.
- Memory leak: review thủ công 3 điểm (KHÔNG cần LeakCanary):
  - Activity host banner có giữ reference banner sau `onDestroy` không?
  - Fragment/Activity host rewarded có giữ reference callback không?
  - `Application` class có leak `Activity` qua `AdManager` callback không?
- Output: bảng "điểm review / kết luận / `file:line` dẫn chứng".

## Step 7 — Policy check (5 rule cụ thể, không hơn không kém)

| # | Rule | Cách verify |
|---|---|---|
| 1 | Banner không che nội dung tương tác | Đọc layout host banner, xác nhận banner ở dock bottom/top, không overlay nút bấm |
| 2 | App Open không show trên splash thuần — chỉ show khi warmup xong trước khi vào main, hoặc khi resume từ background | Đọc Splash activity + lifecycle hook |
| 3 | Interstitial không show liên tiếp dưới ngưỡng frequency cap | Tin `AdSafetyLimits` mặc định, ghi rõ giá trị mặc định lib dùng |
| 4 | Rewarded chỉ trigger khi user bấm nút | Đọc VIP screen |
| 5 | Không tự click ad / không có UI giả mạo close button | Đọc layout banner |

Báo cáo: PASS/FAIL từng rule + `file:line` dẫn chứng.

## Step 8 — Consent (UMP)

- Gọi `AdManager.requestConsentInfoUpdate` ở Splash `onCreate`.
- Block load App Open + Banner + Interstitial cho tới khi consent resolved.
- Test scenario tối thiểu cần ghi nhận trong báo cáo (KHÔNG cần chạy thực):
  - Non-EEA: skip dialog, ad load bình thường.
  - EEA, user accept: dialog xong, ad load.
  - EEA, user deny: dialog xong, không load personalized ad (lib tự lo).

## Step 9 — VIP 1 ngày auto-activate lần đầu cài

- Lưu ý cần check xem bản thân SDK đã hỗ trợ tự động kích hoạt vip 1 ngày chưa, nếu có rồi thì bỏ qua vì có thể duplicate code
Skip nếu Section 0 ghi `N/A` cho VIP key.

- Lưu flag `pref_first_launch_vip_granted` trong `SharedPreferences`.
- Trong `Application.onCreate` sau khi `AdManager.initialize`, nếu flag chưa set → gọi API activate 1 ngày của lib (verify tên hàm chính xác ở Step 1) → set flag.
- Idempotent: lần mở thứ 2 không re-activate.

## Step 10 — UI/UX VIP management screen

Skip nếu Section 0 ghi `N/A` cho VIP key.

Tạo/cập nhật VIP màn hình với 3 thành phần:

1. **Status row**: "VIP Active: <N> days left" hoặc "Free User". Get state qua API của lib (verify tên hàm ở Step 1).
2. **Button "Nhập mã VIP"**: mở dialog input → user paste plain key → app Base64-encode rồi gọi `AdManager.activateVipByKey(base64)` → toast success/fail.
3. **Button "Watch ad for 3 days VIP"**:
   - Click → `AdManager.loadRewarded` → `showRewarded`.
   - `onUserEarnedReward` → activate 3-day VIP bằng key 3 ngày Base64 ở Section 0.
   - Nếu rewarded load fail → fallback `AdManager.showInterstitial` rồi vẫn activate 3 ngày (theo spec).
4. **Che giấu key**: theo mức đã chọn ở Section 0. Mặc định Base64 hardcode — KHÔNG tự thêm AES/NDK trừ khi user yêu cầu.
5. Entry point: thêm preference/menu item trong settings screen của project trỏ tới fragment này.
6. UI/UX rõ ràng, animation sinh động, hãy tham khảo màn hình VIP của các startup khác để bắt chước.
7. Cần đầy đủ thông tin rõ ràng, ngày kích hoạt, ngày kết thúc, nếu có count down càng tốt, cần check kỹ memory leak

## Step 11 — Self-review & cập nhật `<PROJECT_PATH>/doc/AD.MD`

Review duy nhất 1 lần, KHÔNG lặp 3 lần. Checklist:

- [ ] Mọi slot ad đã thay (grep tên SDK cũ không còn match)
- [ ] 2 build variant ở Section 0 đều xanh
- [ ] 5 policy rule đều PASS
- [ ] Consent đã wire ở Splash
- [ ] (Nếu áp dụng) VIP 1-day auto-activate có flag chống re-trigger
- [ ] (Nếu áp dụng) VIP screen có 3 thành phần (status, key input, watch ad)
- [ ] Bảng so sánh trước/sau đã viết trong `doc/AD.MD`
- [ ] List "info cần user cấp" đã viết trong `doc/AD.MD`

Nếu có item FAIL → fix → check lại item đó (không re-check item đã PASS).

## Step 12 — Báo cáo cuối (1 lần, ≤ 30 dòng)

Format cố định:

```
### Migrate Result
- Build: <variantDebug>=<PASS|FAIL>, <variantRelease>=<PASS|FAIL>
- Slots migrated: <N/M hoặc list slot fail>
- Policy: <5/5 hoặc list rule fail>
- Memory review: <OK | list điểm nghi ngờ>
- Files changed: <count>, lines +<X> -<Y>

### Cần user cấp (nếu có)
- <key/id còn thiếu, kèm chỗ sẽ điền>

### Risk còn lại
- <bullet ngắn>

### Skipped (nếu có)
- <step số — lý do skip, vd "Step 9–10 skipped vì project không có VIP feature">
```

KHÔNG lặp lại "3 round". Step 11 checklist PASS hết → dừng. Có FAIL thì fix tới khi PASS rồi mới báo cáo.
