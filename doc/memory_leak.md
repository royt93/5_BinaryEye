# Memory Leak Analysis — BinaryEye

> Phân tích thủ công toàn bộ source code Kotlin.
> Sắp xếp theo mức độ **tăng dần** từ thấp → cao.
> Cập nhật: 2026-03-16

---

## 🟢 THẤP (Low Severity)

---

### ✅ [L1] `ToneGenerator` — global file-level variables không được giải phóng đúng lúc

**File:** `view/media/Beeps.kt`
**Status:** ✅ **KHÔNG CẦN FIX** — `releaseToneGenerators()` đã được gọi trong `CameraActivity.onDestroy()`. Pattern hiện tại an toàn miễn là mọi caller phải gọi release.

```kotlin
// Đã có:
fun releaseToneGenerators() {
    confirmToneGenerator?.release()
    confirmToneGenerator = null
    errorToneGenerator?.release()
    errorToneGenerator = null
}
// Và được gọi trong ActivityCamera.onDestroy() ✅
```

---

### ✅ [L2] `window.decorView.postDelayed { finish() }` — Activity reference giữ sau khi navigate

**File:** `view/act/ActivitySplash.kt`
**Status:** ✅ **ĐÃ FIX**

**Trước:**
```kotlin
window.decorView.postDelayed({ finish() }, 300)
```

**Sau:**
```kotlin
// Field trong class:
private val handler = Handler(Looper.getMainLooper())
private val finishRunnable = Runnable { finish() }

// Trong goToMain():
handler.postDelayed(finishRunnable, 300)

// Trong onDestroy():
handler.removeCallbacks(finishRunnable)
```

---

### ✅ [L3] `Cursor` từ `db.getScans()` — không đảm bảo đóng trong mọi trường hợp

**File:** `database/Db.kt`, `frm/FHistory.kt`
**Status:** ✅ **KHÔNG CẦN FIX** — Cursor được đóng an toàn qua `scansAdapter.changeCursor(null)` trong `onDestroy()` của `FHistory`, và các query có giá trị (export/share) đều dùng `.use {}`. Pattern hiện tại đủ an toàn.

---

## 🟡 TRUNG BÌNH (Medium Severity)

---

### ✅ [M1] `Handler(Looper.getMainLooper()).postDelayed(...)` — không được cancel

**File:** `view/act/ActivityCamera.kt` (double-back handler)
**Status:** ✅ **ĐÃ FIX**

**Trước:**
```kotlin
Handler(Looper.getMainLooper()).postDelayed({
    doubleBackToExitPressedOnce = false
}, 2000)
```

**Sau:**
```kotlin
// Field trong class:
private val doubleBackHandler = Handler(Looper.getMainLooper())
private val resetDoubleBack = Runnable { doubleBackToExitPressedOnce = false }

// Trong onBackPressed():
doubleBackHandler.removeCallbacks(resetDoubleBack)
doubleBackHandler.postDelayed(resetDoubleBack, 2000)

// Trong onDestroy():
doubleBackHandler.removeCallbacks(resetDoubleBack)
```

---

### ✅ [M2] `AdMobManager.interstitialListener` — strong reference đến Activity

**File:** `view/act/ActivityCamera.kt`, `sdkadbmob/AdMobManager.kt`
**Status:** ✅ **ĐÃ FIX**

**Trước:** `onDestroy()` không null out listener  
**Sau:**
```kotlin
override fun onDestroy() {
    // ...
    // [FIX M2] Null out listener để singleton không giữ Activity reference
    AdMobManager.interstitialListener = null
}
```

---

### ✅ [M3] `Handler.postDelayed(...)` ẩn danh trong `AdMobManager` — nhiều instance, không cancel được

**File:** `sdkadbmob/AdMobManager.kt` — `loadAppOpenAd()`
**Status:** ✅ **ĐÃ FIX**

**Trước:** 5 chỗ tạo `Handler(Looper.getMainLooper())` mới mỗi lần gọi  
**Sau:**
```kotlin
// Single shared mainHandler trong object:
private val mainHandler = Handler(Looper.getMainLooper())

// Thay tất cả Handler(Looper.getMainLooper()).postDelayed {...}
// bằng mainHandler.postDelayed { ... }
```

---

## 🔴 CAO (High Severity)

---

### ✅ [H1] `CoroutineScope(Dispatchers.IO)` không có lifecycle — không thể cancel

**File:** `RApp.kt`
**Status:** ✅ **ĐÃ FIX**

**Trước:**
```kotlin
private fun setupAdmob() {
    CoroutineScope(Dispatchers.IO).launch { ... } // Không cancel được!
}
```

**Sau:**
```kotlin
// Field trong class:
private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

override fun onTerminate() {
    super.onTerminate()
    appScope.cancel() // Cancel scope khi app kết thúc
}

private fun setupAdmob() {
    appScope.launch { ... } // Scope có lifecycle
}
```

---

### ✅ [H2] `CoroutineScope` double trong `initSplashScreen` — Activity leak

**File:** `sdkadbmob/AdMobManager.kt`
**Status:** ✅ **ĐÃ FIX** — Lỗi nghiêm trọng nhất

**Trước:**
```kotlin
// Double scope không cancel được + giữ Activity strong reference!
CoroutineScope(Dispatchers.Default).launch {
    EventBus.eventFlow.collectLatest {
        CoroutineScope(Dispatchers.Main).launch {
            loadAppOpenAd(context = activity, ...) // Activity bị capture mãi!
        }
    }
}
```

**Sau:**
```kotlin
// [FIX H2] WeakReference + single cancellable scope
val weakActivity = WeakReference(activity)
splashScope?.cancel()
splashScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
    EventBus.eventFlow.collectLatest {
        val act = weakActivity.get() ?: return@collectLatest // Safe guard
        withContext(Dispatchers.Main) {
            loadAppOpenAd(context = act, ...)
        }
    }
}
```

---

## Tóm tắt

| ID | File | Mô tả | Mức độ | Status |
|----|------|--------|--------|--------|
| L1 | `Beeps.kt` | ToneGenerator global | 🟢 Thấp | ✅ OK (đã có release) |
| L2 | `ActivitySplash.kt` | decorView.postDelayed giữ Activity ref | 🟢 Thấp | ✅ FIXED |
| L3 | `Db.kt` / `FHistory.kt` | Cursor getScans không dùng .use{} | 🟢 Thấp | ✅ OK (pattern an toàn) |
| M1 | `ActivityCamera.kt` | Handler không cancel trong onDestroy | 🟡 Trung bình | ✅ FIXED |
| M2 | `ActivityCamera.kt` + `AdMobManager.kt` | Strong ref Activity trong singleton | 🟡 Trung bình | ✅ FIXED |
| M3 | `AdMobManager.kt` | Handler ẩn danh capture Activity | 🟡 Trung bình | ✅ FIXED |
| H1 | `RApp.kt` | CoroutineScope không có lifecycle | 🔴 Cao | ✅ FIXED |
| H2 | `AdMobManager.kt` | Double CoroutineScope + Activity captured | 🔴 Cao | ✅ FIXED |
