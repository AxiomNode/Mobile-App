# AxiomNode Mobile MVP - Change Summary

## Completed work

### 1. Legacy cleanup
- Removed the old Country datasource implementation and related files.
- Kept the project structure focused on the current MVP scope.

### 2. Multiplatform local storage
- Android: Room via AGP/KSP.
- iOS: Room with bundled SQLite driver in App Documents.
- Desktop: Room with bundled SQLite driver in ~/.axiomnode/.
- DAO APIs are suspend-based to support KMP targets consistently.

### 3. Domain architecture
- Added core models:
  - User
  - Game / Question / GameResult
  - GameCatalog / GameCategory / GameLanguage
- Added use case interfaces:
  - AuthUseCase
  - GamesUseCase

### 4. Data layer
- Added Room entities:
  - GameEntity
  - GameResultEntity
- Added DAOs:
  - GameDao
  - GameResultDao
- Added Ktor clients:
  - AuthHttpClient
  - GamesHttpClient
- Added repositories:
  - AuthRepository
  - GamesRepository

### 5. Dependency injection
- Refactored DI to a Koin module setup based on expect/actual database providers.
- Registered AuthViewModel and GamesViewModel per platform module.

### 6. UI and navigation
- Added auth flow UI:
  - AuthScreen
  - AuthViewModel
- Added games list UI:
  - GamesScreen
  - GamesViewModel
- Added AppNavigation for Auth -> Games state flow.

### 7. Build system updates
- Upgraded Gradle and dependency coordinates.
- Added Room compiler wiring for Android, iOS, and JVM KSP targets.
- Removed obsolete SQLDelight and web-target-specific configuration.

## Build validation

- Android Kotlin compile: successful.
- Desktop JVM Kotlin compile: successful.
- iOS build validation pending macOS/Xcode execution.

## Integration notes

### Auth path
1. App receives Firebase token (placeholder flow in current UI).
2. App calls microservice-users session endpoint.
3. Session data is cached locally.

### Games path
1. App requests categories from microservice-quizz.
2. User requests random/generated game models.
3. Games are cached locally.
4. Results are prepared for future sync to microservice-users.

## Recommended next steps

### Phase 1 - Gameplay
1. Implement GamePlayScreen.
2. Render question progression and answer flow.
3. Add score calculation and result persistence.

### Phase 2 - Google Sign-In hardening
1. Configure Firebase projects and credentials.
2. Integrate platform-specific sign-in SDK pieces.
3. Add token lifecycle handling.

### Phase 3 - Sync engine
1. Add background sync for unsynced game results.
2. Add retry/backoff strategy.
3. Add sync state indicators in UI.

### Phase 4 - Quality pass
1. Add ViewModel unit tests.
2. Add repository integration tests.
3. Improve error and loading UX.

## MVP checklist

- [x] KMP base structure (Android, iOS, Desktop)
- [x] Multiplatform Room setup
- [x] Auth scaffolding
- [x] Games list scaffolding
- [x] expect/actual DI strategy
- [x] Android/JVM compile validation
- [ ] Functional Google Sign-In flow
- [ ] Gameplay implementation
- [ ] Backend sync completion
- [ ] Leaderboard UI

## Status

- Current status: MVP foundation in place, gameplay phase pending.
- Last updated: 31 Mar 2026.
