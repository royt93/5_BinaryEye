# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android barcode scanner ("Cat Scanner with history", marketed under app_name resource). Fork of BinaryEye, package `com.mckimquyen.binaryeye`, Gradle root `Smart_QR_Tool`. Kotlin-only sources under `app/src/main/kotlin` (note: `srcDirs += 'src/main/kotlin'`, not the default `java`).

- AGP 8.13.0, Kotlin 2.2.20, Java/JVM target 17
- minSdk 24, compile/targetSdk 36
- Application class: `com.mckimquyen.binaryeye.RApp` (declared in manifest as `.RApp`)

## Build & run

The wrapper is `./gradlew`. There are two flavor dimensions × two build types, so always pick a variant when building:

- `./gradlew assembleDevDebug` — local development build
- `./gradlew assembleProductionRelease` — signed Play Store build (release uses `app/keystore.jks` with `KS_ALIAS` / `KS_PW` from `gradle.properties`; without the keystore present, the `release` variants fail to assemble. Release is `minifyEnabled true` with R8 + `proguard-rules.pro`)
- `./gradlew installDevDebug` — install to a connected device
- `./gradlew lintDevDebug` — Android Lint (config in `app/lint.xml`)
- `./gradlew clean`

Output APKs are renamed via `applicationVariants.configureEach` to `com.mckimquyen.binaryeye<buildType>_<versionName>_<versionCode>.apk`.

`app/src/test/kotlin` and `app/src/androidTest/kotlin` are wired in `sourceSets` but currently empty — there is no JVM/instrumented test suite to run. Don't fabricate test commands.

## Flavors and ad SDK config

Two product flavors under dimension `type`: `dev`, `production`. Both ship the same code; flavor only changes `app_name` and `FLAVOR_buildEnv`. Ad unit IDs are *not* swapped per flavor — they're swapped per **build type** via `buildConfigField`:

- `release` build type uses real AdMob unit IDs but ships with `IS_ENABLE_ADMOB = false`
- `debug` build type uses Google's test AdMob IDs, also with `IS_ENABLE_ADMOB = false`

So in practice the app currently runs on **AppLovin MAX** only (Admob is disabled by `IS_ENABLE_ADMOB`). The AppLovin SDK key and ad unit IDs live in `defaultConfig` `buildConfigField`s and are read from `BuildConfig.APPLOVIN_*` in `RApp.setupAdSystem()`.

## Architecture

### Application bootstrap

`RApp.onCreate` constructs two top-level singletons declared as package-level `val`s in `RApp.kt`:

- `val db = Db()` — opened with `db.open(context)` and closed in `onTerminate`
- `val prefs = Pref()` — initialized via `prefs.init(context)`

These are imported elsewhere as `import com.mckimquyen.binaryeye.db` / `prefs` and used directly. Do not introduce DI — the codebase relies on these globals.

`RApp.setupAdSystem()` then configures `com.roy.sdkadbmob.AdManager` (from external lib `com.github.royt93:AdmobApplovinWrapper:1.1.3`) with `AdSdkConfig` and calls `AdManager.initialize`. The old in-tree `sdkadbmob/AdMobManager.kt` has been deleted in favor of this wrapper (deletion currently staged on `dev`) — see `doc/AD.MD` for the migration plan and VIP-key scheme. The 30-day VIP secret (`vipKeySecret`) is hardcoded in `RApp.setupAdSystem()` and feeds the wrapper's VIP-key validation; changing it invalidates all previously issued keys.

### Activity flow

- `ActivitySplash` (LAUNCHER, `SplashTheme`) → handles UMP consent + App Open ad warmup, then routes to `ActivityMain`
- `ActivityMain` — fragment host (`R.layout.roy_a_main`, toolbar at `R.id.toolbar`) for `FDecode`, `FEncode`, `FHistory`, `FPreferences`, `FVipManagement`, etc.
- `ActivityCamera` — the actual scanner. Owns the AppLovin banner and the post-scan interstitial. Also handles the SEND/VIEW intent filters and the `binaryeye://scan` deep link.
- `ActivityPick` — image-pick entry point.
- `BaseActivity` — every activity must extend this. It (1) applies `prefs.customLocale` in `attachBaseContext`, (2) clamps `fontScale` to `1.0`, (3) opts into the highest-refresh-rate display mode on API 30+. New activities that bypass `BaseActivity` will silently break locale overrides.

### Naming conventions

These prefixes are load-bearing — match them when adding new files:

- `R*` — app-level (`RApp`)
- `Activity*` — activities (under `view/act/`)
- `F*` — fragments (under `frm/`, e.g. `FPreferences`, `FVipManagement`)
- `roy_*` — XML layout files (`roy_a_main.xml`, `roy_f_decode.xml`, `roy_dlg_*.xml`, `roy_frm_*.xml`)

### Scan-action dispatch

When a barcode is decoded, `view/actions/ActionRegistry.getAction(data)` walks an ordered `Set<IAction>` and returns the first action whose `canExecuteOn(data)` matches; `OpenOrSearchAction` is the fallback. To support a new payload type, add an `IAction` (or `IntentAction`/`SchemeAction` subclass) under `view/actions/<type>/` and register it in `ActionRegistry.REGISTRY`. **Order matters** — `WebAction` is intentionally last because URL detection is greedy.

### Persistence

- `database/Db.kt` — direct `SQLiteOpenHelper`, no Room. Schema constants (`SCANS_*`) live in the same file. Cursor-returning methods like `getScans` / `getScansDetailed` are consumed by `adapter/ScansAdapter`.
- `pref/Pref.kt` — typed wrapper around `SharedPreferences`. Settings UI is `FPreferences` driven by `res/xml/preferences.xml`; new prefs must be added in **all three** places (constant + accessor in `Pref`, entry in `preferences.xml`, summary handling in `FPreferences.changeListener` if dynamic).

### Localization

The app ships with ~20 `values-*` locales and a runtime locale override (`prefs.customLocale`, applied via `BaseActivity.attachBaseContext` and `ext.app.applyLocale`). When adding user-facing strings, update `values/strings.xml` and be aware that `FLanguageDialog` is shown on first launch (gated by `HAS_SHOWN_LANGUAGE_DIALOG` pref). `bundle { language { enableSplit = false } }` is intentional — do not enable language splits or runtime locale switching breaks.

## External library notes

- Barcode engine: `com.github.markusfisch:zxing-cpp:v2.3.0.4` (16KB page-size build — keep this version or newer for Android 15+ compatibility).
- Camera: `com.github.markusfisch:CameraView` (legacy `android.hardware.Camera` API, not CameraX — `ActivityCamera` is built around it).
- Ads: `com.github.royt93:AdmobApplovinWrapper:1.1.3` exposes `AdManager`, `AdSdkConfig`, `AdSafetyLimits`. Lifecycle/App-Open/frequency-cap is handled by the SDK; do not re-implement.

## Reference docs in repo

- `doc/AD.MD` — current ad-SDK + VIP migration plan (touchpoints, before/after, VIP key encoding).
- `doc/code_review.md`, `doc/memory_leak.md`, `doc/quick_win.md` — historical review notes; treat as context, not as todos unless the user asks.
