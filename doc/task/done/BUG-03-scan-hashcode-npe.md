# BUG-03 — 🔴 P1 — NPE trong Scan.hashCode()

**Status:** ✅ DONE — 2026-08-21, build+test pass
**Nguồn:** agy — đã tự verify

## Hiện trạng (đã verify)

`database/Scan.kt`:
```kotlin
val version: String? = null,   // line 17, nullable

override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + dateTime.hashCode()
    result = 31 * result + content.hashCode()
    result = 31 * result + format.hashCode()
    result = 31 * result + (errorCorrectionLevel?.hashCode() ?: 0)   // line 65 — safe-call đúng
    result = 31 * result + version.hashCode()                        // line 66 — THIẾU safe-call
    result = 31 * result + sequenceId.hashCode()
    result = 31 * result + (country?.hashCode() ?: 0)                // line 70 — safe-call đúng
    result = 31 * result + (addOn?.hashCode() ?: 0)                  // line 71 — safe-call đúng
    result = 31 * result + (price?.hashCode() ?: 0)                  // line 72 — safe-call đúng
    result = 31 * result + (issueNumber?.hashCode() ?: 0)            // line 73 — safe-call đúng
    ...
}
```
Mọi field nullable khác đều dùng `?.hashCode() ?: 0`, chỉ riêng `version` (dòng 66) gọi thẳng `.hashCode()` — ném `NullPointerException` nếu `version == null` và object `Scan` được đưa vào `HashSet`/`HashMap`/so sánh data class ở bất kỳ đâu trong code (kể cả gián tiếp qua thư viện/collection framework).

## Fix

```kotlin
result = 31 * result + (version?.hashCode() ?: 0)
```

## Test

- Unit test JVM thuần (Scan là data class, không cần Android): tạo `Scan(version = null, ...)`, gọi `.hashCode()`, assert không throw.
- Thêm case vào test suite hiện có nếu có file test cho `Scan` (hiện chưa có — xem Epic G, cân nhắc tạo `ScanTest.kt` luôn trong lúc fix).
