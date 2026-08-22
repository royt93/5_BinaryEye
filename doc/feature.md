# Feature Tracker — BinaryEye / Cat Scanner

> Cập nhật: 2026-06-24
> Status: ✅ Implemented | 🟡 In progress | 📋 Picked | ⏸️ Deferred | ❌ Skipped | 💭 Ideas

---

## Core Scanning

| Status | Feature |
|--------|---------|
| ✅ | Camera scan với multi-format (17+ formats: QR, EAN, Code128, PDF417...) |
| ✅ | Pinch zoom + swipe zoom |
| ✅ | Flash / torch toggle |
| ✅ | Front camera support |
| ✅ | Auto-rotate |
| ✅ | Try harder mode |
| ✅ | Bulk mode (quét liên tục) + delay giữa các lần |
| ✅ | ROI crop frame (drag handle) |
| ✅ | Scan từ image (ActivityPick) |
| ✅ | Intent filter: VIEW/SEND + deep link `binaryeye://scan` |

## History

| Status | Feature |
|--------|---------|
| ✅ | Lưu lịch sử scan vào SQLite |
| ✅ | Xem chi tiết scan (content, format, timestamp) |
| ✅ | Long-press multi-select trong History |
| ✅ | Show scan as QR từ History |
| ✅ | Share / Copy từ History |
| ✅ | Export CSV / JSON / Database |
| ✅ | Delete scan(s) |
| ✅ | **F1** — Filter chips: theo ngày (All/Today/Week/Month) + nhóm format (QR/1D/2D) — `ScanFilter`, `Db.buildWhereClause` |
| ✅ | **F3** — Scan Tags: gán 4 preset (Work/Personal/Shopping/Travel) + tag tuỳ chỉnh (VIP), filter chip theo tag — `Db` (DB v7→v8), `TagUtils`, `FHistory.manageTags` |

## Encode / Generate

| Status | Feature |
|--------|---------|
| ✅ | Generate barcode từ text input |
| ✅ | Paste từ clipboard |
| ✅ | Chọn format (multi-format picker) |
| ✅ | Error correction level |
| ✅ | Share barcode image |
| ✅ | **F2** — QR styling: màu foreground/background (preset picker) + logo overlay (tự ép EC=H) — `FEncode`, `FBarcode.overlayLogo` |
| ✅ | **F6** — Batch QR: 1 dòng = 1 QR, preview thumbnail, export ZIP (cap 200 dòng) — `FBatchEncode` |

## Ad & Monetization

| Status | Feature |
|--------|---------|
| ✅ | AppLovin MAX banner (ActivityCamera) |
| ✅ | AppLovin MAX interstitial (sau scan) |
| ✅ | AppLovin MAX App Open (Splash) |
| ✅ | AppLovin MAX Rewarded (VIP 3 ngày) |
| ✅ | VIP Management screen (FVipManagement) |
| ✅ | Nhập key VIP thủ công |
| ✅ | Watch ad → 3 ngày VIP |
| ⏸️ | In-App Purchase VIP 30 ngày ($0.50) |
| ⏸️ | In-App Purchase VIP 90 ngày ($1.00) |
| ⏸️ | In-App Purchase VIP 1 năm ($2.00) |
| ⏸️ | In-App Purchase VIP Lifetime ($3.00) |

## UI / UX

| Status | Feature |
|--------|---------|
| ✅ | Material You dialog system |
| ✅ | Language selection dialog (FLanguageDialog, 20+ ngôn ngữ) |
| ✅ | Custom locale override (runtime) |
| ✅ | Adaptive refresh rate |
| ✅ | Window insets / edge-to-edge |
| ✅ | VIP animations (stagger, glow, pulse, shimmer, confetti) |
| ✅ | Splash animations |
| ✅ | **E6** — Bottom sheet kết quả scan: smart action + Copy/Share/Save/Details (thay vì mở thẳng màn detail) — `ActivityCamera.showScanBottomSheet` |

## Settings

| Status | Feature |
|--------|---------|
| ✅ | Vibrate on scan |
| ✅ | Beep on scan (configurable tone) |
| ✅ | Show metadata (position, angle...) |
| ✅ | Show hex dump |
| ✅ | Show recreation (re-encoded barcode) |
| ✅ | Auto-close after result |
| ✅ | Open immediately (no confirmation) |
| ✅ | Copy immediately to clipboard |
| ✅ | Custom search URL |
| ✅ | Send scan to URL (webhook) |
| ✅ | Send scan via Bluetooth |
| ✅ | Ignore consecutive duplicates |
| ✅ | Free rotation (image pick) |
| ✅ | Expand escape sequences |

---

## 💭 Ideas — Tính Năng Mới Tiềm Năng

Xem `doc/task.md` để biết chi tiết và priority.
