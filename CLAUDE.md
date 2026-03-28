# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eva2z Connect is a **Kotlin Multiplatform (KMP)** app targeting **Android** (API 29-36) and **iOS** (arm64 + simulator). It's a GPS vehicle tracking platform with features including live location, geo-fencing, history playback, driver scores, payments, community posts, and device management.

## Build Commands

```bash
# Android debug build
./gradlew :composeApp:assembleDebug

# Android release build (requires keystore.properties with signing credentials)
./gradlew :composeApp:assembleRelease

# Run tests
./gradlew :composeApp:allTests

# iOS builds are done via Xcode (iosApp/iosApp.xcodeproj) after Gradle generates the shared framework
```

## Architecture

**Stack**: Kotlin 2.2.20, Compose Multiplatform 1.9.3, AGP 8.13.2, JVM target 11

**Pattern**: Repository → ViewModel (CoroutineScope + StateFlow) → Composable Screens

**No DI framework** — dependencies are manually created in `App.kt` and screen composables using `remember {}`.

### Key layers

- **`network/ApiService.kt`** — Central Ktor HTTP client. Base URL: `https://gps.eva2z.in/api/`. All API calls go through here with Bearer token auth.
- **`network/KtorClient.kt`** — Platform-specific HTTP client factory (OkHttp on Android, Darwin on iOS).
- **`repository/`** — Business logic layer wrapping ApiService calls (AuthRepository, HomeRepository, DeviceReportRepository, etc.).
- **`data/local/StorageManager.kt`** — Platform-specific persistent storage (SharedPreferences on Android, UserDefaults on iOS). `UserPrefs.kt` wraps token, name, phone, email.
- **`navigation/HomeNavGraph.kt`** — Main tab navigation using **Voyager** (tab + stack navigation, not Jetpack Navigation).
- **`data/local/DeepLinkManager.kt`** — Deep link state persistence and routing.

### Platform source sets

- **`commonMain`** — Shared Kotlin code (screens, ViewModels, networking, models)
- **`androidMain`** — Android implementations (MainActivity, Firebase services, Camera X, Maps, PhonePe payments)
- **`iosMain`** — iOS implementations (Darwin networking, platform storage)
- **`iosApp/`** — Swift entry point (iOSApp.swift, AppDelegate.swift, PaymentBridge.swift)

### Entry points

- **Android**: `MainActivity.kt` → initializes Storage → DeepLinkManager → Context → `App()`
- **iOS**: `iOSApp.swift` → `AppDelegate` → `ContentView` (ComposeView wrapper) → `App()`
- **Shared**: `App.kt` is the root `@Composable` function

## Key Libraries

| Purpose | Library |
|---------|---------|
| Navigation | Voyager 1.0.1 (tab + stack) |
| Networking | Ktor Client 3.3.3 |
| Serialization | kotlinx-serialization-json 1.9.0 |
| Image loading | Coil |
| Maps | Google Maps + Maps Compose (Android only) |
| Payments | PhonePe Intent SDK (Android), PaymentBridge (iOS) |
| Push notifications | Firebase Cloud Messaging |
| Analytics | Firebase Analytics + Meta/Facebook SDK |
| Camera/Barcode | Camera X + ML Kit (Android only) |

## Conventions

- Request/response models live in `request/` and `response/` packages with `@Serializable` annotations.
- Screen directories under `ui/screens/` typically contain a Screen composable, a ViewModel, and related UI components.
- Platform-specific implementations use `expect`/`actual` declarations (e.g., `Platform.kt`, `PlatFormContext.kt`, `StorageManager.kt`).
- ViewModels use `CoroutineScope(Dispatchers.Main)` with `StateFlow` for state — not Android's `androidx.lifecycle.ViewModel` in common code.
- Deep links use scheme `ev2z://` (e.g., `ev2z://payment/callback`) and HTTPS links via `go.eva2z.com`.

## Android-Specific Notes

- Signing config reads from `keystore.properties` (not committed) pointing to `keystores/evatozkey.jks`.
- `AndroidManifest.xml` declares deep link intent filters, Firebase services, Google Maps API key, and Facebook SDK metadata.
- Uses `singleTop` launch mode for MainActivity.

## Dependency Versions

Centralized in `gradle/libs.versions.toml`. Update versions there, not in individual `build.gradle.kts` files.
