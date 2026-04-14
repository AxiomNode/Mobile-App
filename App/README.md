# AxiomNode — Kotlin Multiplatform Template

Kotlin Multiplatform project targeting **Android**, **iOS**, and **Desktop (JVM)**.

---

## Toolchain

| Tool | Version |
|---|---|
| Kotlin | 2.3.20 |
| Android Gradle Plugin | 9.0.1 |
| Gradle Wrapper | 9.3.1 |
| Compose Multiplatform | 1.10.3 |
| Compose Material3 | 1.10.0-alpha05 |
| Ktor | 3.4.2 |
| Koin | 4.2.0 |
| Room (Android/iOS/Desktop) | 2.8.1 |
| Kamel | 1.0.9 |

---

## Local storage by platform

| Platform | Storage | Location |
|---|---|---|
| **Android** | Room (SQLite) via KSP | DB file managed by Android |
| **Desktop (JVM)** | Room (SQLite, bundled driver) | `~/.axiomnode/countries_room.db` |
| **iOS** | Room (SQLite, bundled driver) | App Documents directory (`countries_room.db`) |

---

## Project structure

```
androidApp/src/main/  ← Android launcher activity + app manifest
composeApp/src/
├── commonMain/       ← shared logic, UI, domain, data interfaces
├── androidMain/      ← Android actuals, Room datasource, platform services
├── iosMain/          ← iOS entry point (Room datasource)
├── jvmMain/          ← Desktop entry point (Room datasource)
└── commonTest/       ← shared tests
```

* **[/composeApp](./composeApp/src)** — Compose Multiplatform shared code.
* **[/androidApp](./androidApp/src/main)** — Android application module.
* **[/iosApp](./iosApp/iosApp)** — SwiftUI entry point for iOS.

---

## Build and Run

### Android

```shell
# macOS/Linux
./gradlew :androidApp:assembleDebug
# Windows
.\gradlew.bat :androidApp:assembleDebug
```

### Desktop (JVM)

```shell
# macOS/Linux
./gradlew :composeApp:run
# Windows
.\gradlew.bat :composeApp:run
```

### iOS

Open **[/iosApp](./iosApp)** in Xcode and run from there, or use the IDE run configuration.

---

## Architecture notes

- **DI** — Koin; `platformModule` provides the platform-specific `CountryDatasource`.
- **Networking** — Ktor with platform engines (OkHttp / Darwin / Java).
- **Image loading** — Kamel.
- **Cache strategy** — sync-if-stale (24 h TTL) with `forceRefresh` option.

---

## Current migration status

- The Android launcher has been split into `:androidApp`, so `:composeApp` now acts as the shared KMP module.
- `:composeApp` now uses `com.android.kotlin.multiplatform.library` instead of the legacy `com.android.library` plugin.
- `:composeApp:jvmTest` still fails in `compileTestKotlinJvm` with a Gradle variant-selection NPE under the current AGP/Kotlin/Compose stack.

## ⚠️ Known deprecations (non-blocking)

| Warning | Cause | Fix |
|---|---|---|
| `android.builtInKotlin=false` / `android.newDsl=false` | Legacy AGP 9 compatibility toggles | Migrate to AGP built-in Kotlin |
| `org.jetbrains.kotlin.android` in `:androidApp` | AGP 9 now prefers built-in Kotlin support for Android-only modules | Remove legacy flags and migrate androidApp to built-in Kotlin |
| `KoinApplication(application=…)` deprecated | Koin 4.2 changed API | Update `App.kt` when starting |

### Next platform step

The Android app split and shared-module plugin migration are now in place. The next compatibility step is migrating the project off the AGP legacy Kotlin flags so `:androidApp` can use built-in Kotlin support cleanly:

```
:composeApp  ← KMP library (commonMain + iosMain + jvmMain + androidMain)
:androidApp  ← com.android.application, depends on :shared
```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
and [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform).
