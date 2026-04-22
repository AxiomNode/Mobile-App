# AxiomNode Mobile MVP (Kotlin Multiplatform)

This MVP targets Android, iOS, and Desktop JVM, with a first functional scope focused on authentication and game listing workflows.

## MVP features

### Authentication
- AuthUseCase defines sign-in/session APIs.
- AuthViewModel manages state and session checks.
- AuthScreen provides initial login UI scaffold.
- Session sync path points to microservice-users.

### Games
- GamesUseCase defines catalog/generation/fetch/result APIs.
- GamesViewModel manages catalog and games state.
- GamesScreen renders available games list and actions.
- Data sources point to microservice-quizz and microservice-users.

### Local persistence
- Room database shared across targets using KMP setup.
- Android uses standard Room builder.
- iOS and JVM use bundled SQLite driver.
- Cached entities:
  - GameEntity
  - GameResultEntity

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

### microservice-users
- POST /users/firebase/session
- GET /users/me/profile
- POST /users/me/games/events

### microservice-quizz
- GET /games/categories
- POST /games/generate
- GET /games/models/random

## Build commands

### Android
./gradlew :composeApp:assembleDebug

### Desktop JVM
./gradlew :composeApp:run

### iOS
Open iosApp/iosApp.xcodeproj in Xcode and run.

## Current scope boundaries

The current MVP description in this file is limited to:

1. authentication and session bootstrap
2. game catalog and fetch flows
3. local persistence for cached games and results

Gameplay completeness, release hardening, and broader quality coverage are tracked in the implementation surface of the app itself rather than maintained here as roadmap text.

## Current status

- Foundation architecture completed.
- Android and JVM compilation validated.
- iOS execution validation pending macOS build host.
