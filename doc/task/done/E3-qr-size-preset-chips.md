# E3 — 🟡 Custom QR Size Preset Chips

**Status:** ✅ DONE — 2026-08-21, build+test pass (đã verify: phần lõi đã có sẵn, chỉ thiếu preset UI)

## Hiện trạng (đã verify bởi agent nội bộ)

`frm/FEncode.kt:430`: `getSize(power) = 128 * (power+1)`, SeekBar `android:max="7"` (`roy_f_encode.xml:182`) → đã cho phép chọn size liên tục 128/256/384/512/640/768/896/1024px qua kéo thanh trượt. Không có preset chip rời rạc nào (128/256/512/1024) như roadmap gốc mô tả.

## Việc cần làm

1. `frm/FEncode.kt`: thêm 4 `Chip` (hoặc `MaterialButton` nhóm) preset 128/256/512/1024, set thẳng `sizeBarView.progress` tương ứng (128→power 0, 256→power 1, 512→power 3, 1024→power 7).
2. Đồng bộ 2 chiều: kéo SeekBar tay → highlight đúng chip nếu trùng preset (hoặc bỏ highlight nếu giá trị custom).
3. Hiển thị label "Npx" cạnh SeekBar, cập nhật trong `updateSize()`.

## Test

- Chọn preset 512 → Encode → bitmap output đúng 512×512px.
- Kéo SeekBar tay tới giá trị không trùng preset nào → không chip nào bị highlight sai.
