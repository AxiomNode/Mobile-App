# AxiomNode Mobile MVP Snapshot

Last updated: 2026-05-03.

## Purpose

Summarize the current functional MVP surface of the mobile client without turning this file into a roadmap.

## MVP features

### Authentication

- `AuthUseCase` defines sign-in and session APIs.
- `AuthViewModel` manages startup auth checks and sign-in state.
- `AuthScreen` provides the entry surface before the main app shell.
- In non-dev auth mode, the mobile app syncs player state through `PUT /v1/mobile/player/profile` and later reads `GET /v1/mobile/player/profile`.
- Android supports Firebase-backed Google Sign-In; iOS and desktop remain in dev-mode stub state.

### Games

- `GamesUseCase` defines catalog, generation, random fetch, and result sync APIs.
- The app fetches categories through `GET /v1/mobile/games/categories`.
- Random discovery uses `GET /v1/mobile/games/quiz/random` and `GET /v1/mobile/games/wordpass/random`.
- Generated gameplay uses `POST /v1/mobile/games/quiz/generate` and `POST /v1/mobile/games/wordpass/generate`.
- Game results sync through `POST /v1/mobile/games/events`.

### Local persistence

- Room-backed persistence is shared across targets through the KMP setup.
- Cached data supports startup bootstrap, catalog reuse, and previously fetched gameplay.
- The app keeps local user profile and game-related data so the UI remains usable when backend calls partially fail.

### Navigation and product surface

Current top-level app destinations:

- Home
- Catalog
- History
- Stats
- Profile

Current supporting flows:

- authentication
- game lobby by type
- active gameplay
- history detail
- settings
- about

## Code layout

composeApp/src/
- commonMain: domain, data, presentation, DI expect declarations.
- androidMain: Android actual database provider + DI bindings.
- iosMain: iOS actual database provider + DI bindings.
- jvmMain: JVM actual database provider + DI bindings.

## Architecture

### DI modules
- dataModule (common): http client, clients, database, repositories.
- platformModule (per target): ViewModel registrations and target-specific wiring.

### Data flow
1. UI triggers ViewModel actions.
2. ViewModel calls use case interface.
3. Repository orchestrates HTTP + Room access.
4. StateFlow updates UI.

## Primary integrated endpoints

### Mobile edge
- PUT /v1/mobile/player/profile
- GET /v1/mobile/player/profile
- GET /v1/mobile/games/categories
- GET /v1/mobile/games/quiz/random
- GET /v1/mobile/games/wordpass/random
- POST /v1/mobile/games/quiz/generate
- POST /v1/mobile/games/wordpass/generate
- POST /v1/mobile/games/events

## Build commands

### Android

./gradlew :androidApp:assembleDebug

### Desktop JVM

./gradlew :composeApp:run

### iOS

Open iosApp/iosApp.xcodeproj in Xcode and run.

## Current scope boundaries

The current MVP description in this file is limited to:

1. authentication and session bootstrap
2. game catalog, random fetch, and generation flows
3. local persistence and result synchronization

Gameplay completeness, release hardening, and broader quality coverage are tracked in the implementation surface of the app itself rather than maintained here as roadmap text.

## Current status

- Android is the most complete authentication target.
- iOS and desktop still rely on `AUTH_MODE=dev` for usable sign-in.
- The client already includes profile, history, stats, and gameplay-facing screens beyond the earliest catalog-only baseline.
