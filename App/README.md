# AxiomNode Mobile App Module

Last updated: 2026-05-03.

## Purpose

Describe the current mobile application module that ships the shared Kotlin Multiplatform client for Android, iOS, and desktop-oriented development flows.

## Runtime role

This module is the executable client surface for end users. It consumes the public edge contract exposed by `api-gateway` through mobile-facing routes and should not embed infrastructure-only secrets.

## Current platform shape

- `composeApp`: shared Kotlin Multiplatform application code, including UI, domain, data, configuration, DI, and networking.
- `androidApp`: Android launcher application.
- `iosApp`: iOS host application.
- desktop JVM support remains available for local validation and development workflows.

## Product navigation surface

The shared app currently exposes these top-level destinations:

- Home
- Catalog
- History
- Stats
- Profile

Additional in-app flows include:

- authentication
- game lobby by game type
- active gameplay
- history detail
- settings
- about

## Architecture notes

Current implementation signals from the module:

- Compose Multiplatform for shared UI
- Koin for dependency injection
- Ktor client for network access
- Room/SQLite for local persistence
- build-time environment configuration via generated app config

At startup, the app performs an initial auth and content sync before entering the main navigation flow.

## Configuration model

Runtime values are injected from environment property files into generated configuration code.

Current config surface includes:

- `API_BASE_URL`
- `AUTH_MODE`
- `FIREBASE_*`
- `GOOGLE_WEB_CLIENT_ID`

The mobile app must use public edge URLs and must not embed infrastructure tokens such as `EDGE_API_TOKEN`.

## Module structure

```text
App/
	androidApp/        Android launcher
	composeApp/        shared app code and build logic
	iosApp/            iOS host application
	QUICK_REFERENCE.md build and troubleshooting commands
	FIREBASE_SETUP.md  Firebase integration setup
	MVP_README.md      functional MVP context
```

## Build and run

Android debug build:

```bash
./gradlew :androidApp:assembleDebug
```

Desktop JVM run:

```bash
./gradlew :composeApp:run
```

iOS execution requires Xcode on macOS through the `iosApp` project.

## Validation

Baseline validation in current workflows includes:

- `:composeApp:jvmTest`
- `:androidApp:lint`
- Android debug build from `:androidApp`
- iOS simulator framework linking on macOS runners

## Related documents

- [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
- [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
- [MVP_README.md](MVP_README.md)
- [../README.md](../README.md)
- [../../docs/guides/experience/mobile-app-development-firebase.md](../../docs/guides/experience/mobile-app-development-firebase.md)
