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
composeApp/src/
├── commonMain/       ← shared logic, UI, domain, data interfaces
├── androidMain/      ← Room datasource, Android entry point
├── iosMain/          ← iOS entry point (Room datasource)
├── jvmMain/          ← Desktop entry point (Room datasource)
└── commonTest/       ← shared tests
```

* **[/composeApp](./composeApp/src)** — Compose Multiplatform shared code.
* **[/iosApp](./iosApp/iosApp)** — SwiftUI entry point for iOS.

---

## Build and Run

### Android

```shell
# macOS/Linux
./gradlew :composeApp:assembleDebug
# Windows
.\gradlew.bat :composeApp:assembleDebug
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

## ⚠️ Known deprecations (non-blocking)

| Warning | Cause | Fix |
|---|---|---|
| `android.builtInKotlin=false` / `android.newDsl=false` | AGP 9 + KMP in same module | See migration below |
| `KoinApplication(application=…)` deprecated | Koin 4.2 changed API | Update `App.kt` when starting |

### Future: separate Android app module (recommended by AGP 9+)

AGP 9.x recommends separating the `com.android.application` plugin into its own subproject:

```
:shared      ← KMP library (commonMain + iosMain + jvmMain + androidMain)
:androidApp  ← com.android.application, depends on :shared
```

This removes all deprecation warnings and is required for AGP 10. Plan this refactor before the first public release.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
and [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform).
