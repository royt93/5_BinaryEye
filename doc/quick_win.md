# Quick Win — Implementation & Manual Test Guide

> Cập nhật: 2026-03-16 | Build: ✅ SUCCESSFUL

---

## ✅ Tính năng 2 — "Show as QR" từ History

### Files đã thay đổi
| File | Thay đổi |
|------|---------|
| `menu/menu_f_history_edit.xml` | Thêm item `showScanAsQr` với icon `ic_action_create` |
| `frm/FHistory.kt` | Xử lý `R.id.showScanAsQr` trong `onActionItemClicked` |
| `values/strings.xml` | Thêm `show_scan_as_qr`, `cannot_show_as_qr` |

### Logic
- Long-press 1 scan trong History → ActionMode hiện → chọn **"Show as QR"**
- Gọi `db.getScan(id)` lấy `content` + `format`
- Mở `FBarcode.newInstance(content, format, size=640)` để hiện barcode
- Nếu multi-select hoặc binary scan → toast "Cannot show as QR"

### Manual Test

| # | Bước | Expected |
|---|------|----------|
| T1 | Scan 1 QR code → vào History → long-press scan đó → chọn "Show as QR" | Màn hình FBarcode hiện đúng mã QR |
| T2 | Long-press 1 scan EAN-13 → "Show as QR" | Barcode EAN-13 hiện đúng |
| T3 | Long-press 2 scan cùng lúc → "Show as QR" | Toast "Cannot show as QR" |
| T4 | Tap vào QR hiện ra → có thể Share/Copy | Hoạt động bình thường |
| T5 | Long-press 1 scan binary → "Show as QR" | Toast "Cannot show as QR" (content rỗng) |

---

## ✅ Tính năng 3 — Paste Clipboard → Generate QR

### Files đã thay đổi
| File | Thay đổi |
|------|---------|
| `layout/roy_f_encode.xml` | Wrap EditText trong RelativeLayout + thêm `btnPaste` (ImageButton) |
| `frm/FEncode.kt` | Thêm `ClipboardManager` import + handler cho `btnPaste` |
| `values/strings.xml` | Thêm `paste_from_clipboard`, `clipboard_empty` |

### Logic
- Nút paste (icon copy 📋) nằm góc phải trên của ô nhập nội dung
- Tap → `ClipboardManager.primaryClip?.getItemAt(0)?.coerceToText(context)`
- Nếu có text → điền vào EditText, đặt cursor cuối
- Nếu clipboard trống → toast "Clipboard is empty"

### Manual Test

| # | Bước | Expected |
|---|------|----------|
| T1 | Copy 1 URL → mở app → tab Encode → tap nút paste | URL điền vào ô content |
| T2 | Clipboard trống → tap nút paste | Toast "Clipboard is empty" |
| T3 | Copy text dài → paste → tap ENCODE | QR generate đúng nội dung |
| T4 | Paste → sửa tay → ENCODE | Dùng text đã sửa, không dùng clipboard |
| T5 | Xoay màn hình sau paste | Text vẫn còn trong EditText |
| T6 | Copy từ màn hình Decode → vào Encode → paste | Barcode content được điền đúng |

---

## Build Status

```
BUILD SUCCESSFUL in 50s
74 actionable tasks: 56 executed, 18 up-to-date
```

> Chỉ có warnings về deprecated API đã có sẵn từ trước (Camera, Parcelable...), không liên quan đến code mới.
