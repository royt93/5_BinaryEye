# Project Init Guide — BinaryEye / Cat Scanner

> Android Kotlin project. AGP 8.13.0, Kotlin 2.2.20, minSdk 24, targetSdk 36.

---

## Coding Rules (Android Kotlin)

- **KHÔNG** dùng `!!` (non-null assertion) → dùng `?: return` hoặc `?.let {}`
- **KHÔNG** tạo `CoroutineScope(Dispatchers.IO)` ẩn danh → dùng scope có lifecycle (`lifecycleScope`, `viewLifecycleOwner.lifecycleScope`, hoặc `appScope` từ `RApp`)
- **KHÔNG** giữ Activity/Fragment reference trong singleton → dùng `WeakReference`
- **KHÔNG** dùng `Handler(Looper.getMainLooper()).postDelayed` ẩn danh → khai báo field `handler` + `runnable` để `removeCallbacks` trong `onDestroy`/`onDestroyView`
- **KHÔNG** `println()` hoặc `Log.*` unconditional trong production → wrap `if (BuildConfig.DEBUG)`
- **KHÔNG** comment block dead code — xóa hẳn

## Naming Conventions

| Prefix | Dùng cho |
|---|---|
| `R*` | App-level class (`RApp`) |
| `Activity*` | Activity classes (file + class cùng tên) |
| `F*` | Fragment classes |
| `roy_*` | XML layout files |

## Globals (đã có sẵn — không tạo mới)

```kotlin
// Khai báo tại package level trong RApp.kt:
val db = Db()      // SQLiteOpenHelper wrapper
val prefs = Pref() // SharedPreferences wrapper

// Import bằng:
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.prefs
```

**KHÔNG** introduce DI framework — codebase dùng globals này.

## Build Commands

```bash
./gradlew assembleDevDebug       # Dev build
./gradlew assembleProductionRelease  # Play Store build (cần keystore.jks)
./gradlew installDevDebug        # Cài lên device
./gradlew lintDevDebug           # Android Lint
./gradlew clean
```

## Ad System

- Provider active: **AppLovin MAX** (`IS_ENABLE_ADMOB = false`)
- Lib: `com.github.royt93:AdmobApplovinWrapper:1.1.3`
- Config: `RApp.setupAdSystem()` → `AdManager.setConfig()` → `AdManager.initialize()`
- Xem chi tiết: `doc/AD.MD`

## Thêm preference mới

1. Thêm constant + accessor trong `pref/Pref.kt`
2. Thêm entry trong `res/xml/preferences.xml`
3. Thêm summary handling trong `FPreferences.changeListener` (nếu dynamic)

## Thêm action mới cho scan payload

1. Tạo `IAction` (hoặc subclass) trong `view/actions/<type>/`
2. Register trong `ActionRegistry.REGISTRY` — chú ý order (WebAction phải cuối)

## BaseActivity — bắt buộc extend

Mọi Activity phải extend `BaseActivity`. Nó xử lý:
- `attachBaseContext` → apply `prefs.customLocale`
- Font scale clamp = 1.0
- Adaptive refresh rate (API 30+)

## String resources

Khi thêm string user-facing:
- Update `values/strings.xml` (tiếng Anh)
- Nếu có bản dịch → update các `values-*/strings.xml` tương ứng
- `bundle { language { enableSplit = false } }` là cố ý — KHÔNG enable language splits
