ExitPlanMode không khả dụng trong môi trường này (tool không tồn tại dù workflow nhắc tới). Đây là task đọc-only thuần túy, không phải implementation plan — nên trả lời thẳng nội dung phân tích đầy đủ (đã lưu tại `~/.claude/plans/ban-la-mot-ai-groovy-starlight.md`) làm câu trả lời cuối cùng:

---

# Review độc lập — BinaryEye / Cat Scanner

*(review-only, không sửa file nào; 3 agent đọc song song camera/scan-dispatch, history/DB, VIP/ads/settings/locale; đã tự verify vài phát hiện quan trọng nhất bằng grep trực tiếp trước khi chốt)*

## 1. Bug / crash risk cần fix

**Nghiêm trọng nhất — xóa nhầm history vượt filter (data loss)**
`database/Db.kt:191-193` `removeScans(query: String? = null)` chỉ nhận text query, KHÔNG nhận `ScanFilter`. `frm/FHistory.kt:454` gọi `db.removeScans(scanFilter.query)` — dialog xác nhận dựa trên `scanFilter.isDefault` (có tính format/date) nhưng DELETE thật bỏ qua 2 filter đó. User filter "QR only, Today" → bấm Clear → tưởng chỉ xóa scan hiển thị → thực tế xóa toàn bộ history. Ưu tiên cao nhất toàn bộ review.

**Cursor leak khi fragment detach giữa chừng** — `frm/FHistory.kt:333-336`: check `activity ?: return@withContext` TRƯỚC khi đóng cursor (dòng 349). Chỗ khác trong file đều dùng `.use{}` đúng — đây là ngoại lệ.

**Main-thread synchronous DB write, ANR risk** — `ActivityCamera.kt:763` (mỗi scan, kể cả bulk-mode), `:880` (Save), `FHistory.kt:415,432,454`. Không index `SCANS_DATETIME` dù mọi query `ORDER BY ... DESC`.

**`onDowngrade` thiếu** — `Db.kt:201-228`. minSdk 24 + Play "undo update"/restore backup DB cũ → crash ngay khi mở app.

**ReDoS risk trên main thread** — `VType.kt:8-14` regex backtracking-prone chạy synchronous trong `canExecuteOn()` (`ActivityCamera.kt:821`), không timeout. VCARD/VEVENT craft ác ý có thể treo UI.

**`.first()` không null-safe** — `ActivityCamera.kt:563-577`, `ActivityPick.kt:127-173` sau `ZxingCpp.readByteArray`; `ActivityPick` case crash cả app vì coroutine không có `CoroutineExceptionHandler`.

**Ad callback không check lifecycle** — `ActivityCamera.kt:325-338,369-375`, 3 chỗ `startActivity` trong callback không check `isFinishing`.

**`versionCode`/`versionName` lệch năm (đã verify)** — `app/build.gradle:63-64`: `versionCode 20270822` (2027) vs `versionName '2026.08.22'` (2026). Không phá build nhưng gây nhầm lẫn mọi release sau.

## 2. Tech debt / code quality

- `ActivityCamera.kt` (~750 dòng) God-class, ad-load pattern lặp 3 chỗ y hệt.
- `ActionRegistry.REGISTRY` dùng `Set<IAction>` (`ActionRegistry.kt:17`) cho invariant thứ tự bắt buộc (WebAction cuối cùng) — chỉ đúng nhờ `setOf`=`LinkedHashSet` ngầm, không compiler-safe.
- Mỗi action tự `String(data)` riêng thay vì convert 1 lần ở registry.
- Không index DB, search không debounce (`FHistory.kt:267-270`, query mỗi keystroke).
- Cột export duplicate 4 chỗ (CSV/JSON/2 query trong `Db.kt`).
- **VIP monetization 100% client-side, crackable (đã verify)**: `app/build.gradle:73-75` bake `VIP_KEY_SECRET`/redeem code thẳng vào `BuildConfig` — R8 không obfuscate string literal, extract bằng jadx vài giây → VIP miễn phí vĩnh viễn cho mọi máy tới khi đổi string + release lại.
- **`allowBackup="true"` không exclude VIP prefs (đã verify)** — `adb backup/restore` nhân bản VIP status.
- **`Locale.kt` bug (đã verify)**: `ext/app/Locale.kt:11-16` split `-` rồi coi phần sau là ISO country code thẳng → `zh-rCN`/`pt-rBR`/`ru-rRU` ra `Locale("zh","rCN")` — không hợp lệ → **Chinese, Russian, Brazilian Portuguese fallback về English**. Fix 1 dòng, ROI cực cao.
- 4 nút plan VIP (30/90/365/lifetime) không có `onClickListener`, chưa có Play Billing dependency — UI dựng 80%, thiếu wiring.

## 3. Enhancement cho tính năng hiện có

Full-text search có index, pagination history, transaction cho bulk delete, pinch-to-zoom thật (hiện chỉ swipe), allow-list scheme cho `binaryeye://scan?ret=` (return URL nhận thẳng từ Intent ngoài — pattern confused-deputy), contrast check màu QR trước khi save, hoàn thiện Play Billing, exclude VIP prefs khỏi backup, fix `Locale.kt`.

## 4. Tính năng mới nên làm

1. **Google Play Billing** cho VIP tier — hạ tầng UI sẵn 80%, ROI cao nhất.
2. **Pagination History** — độ khó thấp-trung bình.
3. **Auto-action config** (trùng F9 backlog) — tận dụng `ActionRegistry` sẵn có.
4. **Backup/restore history JSON round-trip** (không chỉ copy raw .db).
5. **Cảnh báo an toàn trước khi auto-mở URL/action nguy hiểm**.

## 5. Top 5 tính năng VIP-gated độc quyền, tăng doanh thu

1. **Vault** — history mã hóa + biometric lock + recovery-key (khác F8 thường ở phần recovery). Độ khó trung bình.
2. **Batch business workflow** — scan hàng loạt → auto-tag theo regex tự định nghĩa → export template Excel/Sheet, nhắm SME/kho hàng (sẵn trả tiền hơn cá nhân). Độ khó trung bình.
3. **Verify & Track** — cảnh báo hàng giả/duplicate scan (khác F5 compare thủ công), giá trị B2B. Độ khó trung bình-cao.
4. **Smart QR động** — QR trỏ redirect server-managed, sửa được đích sau khi in — SaaS-style, recurring revenue cao nhất trong 5 ý. Độ khó cao.
5. **OCR → structured VCARD** (mở rộng F10) — OCR danh thiếp tự parse field, không chỉ OCR→text→QR thô. Độ khó trung bình-cao.

## 6. Đối chiếu `doc/task.md` (14 item TODO)

**Đồng ý:** E1 (torch auto-on, design ổn), E5 (CSV filter export, doc tự nhận gần xong), E7 (zoom memory, gần như có sẵn), F9 (auto-action, tận dụng registry sẵn), F8 (biometric lock, VIP-gate hợp lý).

**Không đồng ý / hạ ưu tiên:**
- **E4 (confetti)** — cosmetic thuần, nên đẩy xuống thấp hơn nữa vì có bug data-loss thật (mục 1) và lỗ hổng VIP-crack (mục 2) chưa có trong backlog nào.
- **F7 (scan reminder)** — use-case niche, độ khó L không tương xứng giá trị, nên hạ về 💭 Idea.
- **F4 (geo-tag)** — thêm permission location cho feature niche có thể tăng uninstall/giảm trust trên app scanner, cân nhắc kỹ.
- **F10 (OCR→QR)** — nên re-scope thành OCR→VCARD (mục 5.5) thay vì OCR→QR text thuần (giá trị thấp hơn công gõ tay).
- **M1-M6 (IAP) đang Deferred** — KHÔNG đồng ý deferred, nên là ưu tiên 🔴 cao nhất cùng đợt với fix VIP-crack, vì UI đã có sẵn và lỗ hổng doanh thu đang rò rỉ thật.

## 7. Gaps — doc bỏ sót, quan trọng

1. Bug `removeScans` xóa nhầm vượt filter (mục 1) — chưa có task nào, nghiêm trọng nhất.
2. VIP crack qua BuildConfig string + backup leak (mục 2) — hoàn toàn vắng mặt trong task.md/feature.md, ảnh hưởng doanh thu trực tiếp.
3. `Locale.kt` bug làm 3 ngôn ngữ lớn fallback English (đã verify) — fix 1 dòng, ROI cực cao, không có trong doc.
4. `onDowngrade` thiếu → crash risk với minSdk 24 base user lâu năm.
5. Main-thread DB write rải rác — nợ kỹ thuật tích lũy thành ANR, không được nhắc trong task.md.
6. `ActionRegistry` dùng `Set` cho invariant thứ tự — đáng fix khi đụng file này làm F9.

---

*Minh bạch: 1 agent con lỡ chạy `rm -rf` trên thư mục tạm `/tmp/adwrapper_src` (tự tạo để giải nén source thư viện ad đọc), không đụng file nào trong repo, tự báo cáo ngay, không có thiệt hại.*
