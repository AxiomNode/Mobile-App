# AxiomNode Mobile MVP - Current Implementation Summary

## Current implemented scope

### 1. Legacy cleanup
- Removed the old Country datasource implementation and related files.
- Removed `CountriesApi.kt` and `data/remote/` package.
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
  - AuthHttpClient (routes through api-gateway `/mobile/users/...`)
  - GamesHttpClient (routes through api-gateway `/mobile/games/...`)
- Added repositories:
  - AuthRepository
  - GamesRepository

### 5. Dependency injection
- Refactored DI to a Koin module setup based on expect/actual database providers.
- Registered AuthViewModel and GamesViewModel per platform module.
- AppConfig injected as singleton via Koin, consumed by HTTP clients.

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

### 8. Environment & secrets system
- Created `composeApp/env/{dev,stg,prod}.properties` config files (gitignored).
- Added `env.properties.example` template for reference.
- Gradle generates `GeneratedConfig.kt` at build time from active environment.
- Select environment via `axiomnode.env` property (default: `dev`).
- Edge API token injected into all HTTP requests via Ktor `defaultRequest`.
- Logging level adapts per environment (ALL for dev, INFO for stg/prod).

### 9. Android product flavors & signing
- Added product flavors: `dev` (`.dev` suffix), `stg` (`.stg` suffix), `prod`.
- Each flavor provides its own `app_name` resource value.
- Release signing config reads keystore credentials from env properties.
- ProGuard rules added for Kotlin Serialization, Ktor, Room, Firebase.

### 10. Google Sign-In implementation
- Implemented full Credential Manager + Firebase Auth flow on Android.
- Uses `GetSignInWithGoogleOption` (full-screen dialog) as primary, `GetGoogleIdOption` (One-Tap) as fallback.
- Activity context validation with descriptive error messages.
- iOS and Desktop stubs with dev-mode bypass (`AUTH_MODE=dev`).
- Registered 3 Firebase Android apps (prod, dev, stg package names).
- Configured `google-services.json` with all 3 clients and debug SHA-1.
- Created `network_security_config.xml` for cleartext HTTP to local dev server.

### 11. Build variant environment auto-detection
- `resolveEnv()` detects environment from Gradle task graph (e.g., `assembleStgDebug` → `stg`).
- `generateAppConfig` task resolves env at execution time, not config time.
- Supports explicit override via `-Paxiomnode.env=stg`.
- Commented out hardcoded `axiomnode.env=dev` from `gradle.properties`.

### 12. Material 3 theme system
- Imported full color token set from Material Theme Builder (seed `#0043F4`).
- 226 color tokens across 6 schemes (light/dark × Normal/Medium/High contrast).
- Added `AxiomPalette` with tonal palettes for custom component theming.
- `ContrastLevel` enum for accessibility support.
- Typography: Display = Syne (SansSerif fallback), Body = Inter (Default fallback).

### 13. UI redesign with Material 3
- **AuthScreen**: Surface background, branded header, ElevatedButton with primaryContainer, AnimatedVisibility, WelcomeCard, error Card with errorContainer.
- **GamesScreen**: Scaffold + LargeTopAppBar with exitUntilCollapsed scroll, GameCard with type badges (Quiz=primaryContainer, Wordpass=tertiaryContainer), DetailChip, EmptyGamesView.
- **ProfileScreen** (new): User avatar initials, info card, app info, sign-out button.
- **App.kt navigation**: Proper auth gate → MainScaffold with NavigationBar (Games/Profile tabs), Crossfade transitions.

### 14. Code quality improvements
- Added `-Xexpect-actual-classes` compiler flag to suppress beta warnings.
- Suppressed deprecated `KoinApplication` API warning (Koin compose 4.x).
- Fixed KMP compatibility: replaced `System.currentTimeMillis()` with `0L` defaults.
- Added `modifier` parameter to all public composables.
- Fixed `LaunchedEffect` key to include lambda parameter.
- Koin DI fix: removed `authToken: String` from `GamesRepository` constructor.
- Moved database creation from `DataModule` to each platform's `platformModule`.

### 15. Quiz gameplay system
- Created `GamePlayViewModel` — state machine with timer, answer selection, scoring, result persistence via `GamesUseCase.recordGameResult()`.
- Created `GamePlayScreen` — question progression, animated transitions, answer cards with color feedback (green=correct, red=wrong), result screen with emoji/stats/replay.
- Scoring: ≥70% = WON, ≥40% = DRAW, <40% = LOST.
- Gameplay navigation: selecting a game from `GamesScreen` opens `GamePlayScreen` full-screen (hides bottom nav). `activeGame` state in `App.kt MainScaffold`.
- Registered `GamePlayViewModel` in all 3 platform DI modules.

### 16. Backend API alignment
- **Root cause**: `AuthHttpClient` was calling wrong URL (`/mobile/users/firebase/session` — doesn't exist) and response model didn't match actual backend fields.
- Investigated full backend chain: `api-gateway proxy.ts` → `bff-backoffice backoffice.ts` → `microservice-users users.ts`.
- **AuthHttpClient rewritten**:
  - Session sync: `POST /v1/backoffice/auth/session` (was `/mobile/users/firebase/session`).
  - Profile fetch: `GET /v1/backoffice/auth/me` (was `/mobile/users/me/profile`).
  - `SessionSyncResponse`: `{ message, userId, firebaseUid, provider, role }`.
  - `UserProfileResponse`: `{ profile: { firebaseUid, email, displayName, photoUrl }, role }`.
- **Firebase metadata pass-through**: Backend session endpoint doesn't return email/displayName, so these are carried from Firebase Auth user via `GoogleSignInResult.Success` → `AuthUseCase` → `AuthRepository` merge.
- Updated `GoogleSignInResult.Success` with `email`, `displayName`, `photoUrl` fields.
- Updated Android `GoogleSignInService` to extract Firebase user metadata.
- Updated `AuthUseCase.signInWithGoogle()` signature with metadata params.
- Updated `AuthRepository.signInWithGoogle()` to merge Firebase metadata with backend response.
- Updated `AuthViewModel.launchGoogleSignIn()` to pass all metadata fields.
- Updated iOS/JVM dev stubs with test metadata.

### 17. GamesRepository completion
- Implemented `getRecentResults()` and `getGameStats()` methods in `GamesRepository`.
- `getGameStats()` aggregates results from local Room database (totalGames, wins, losses, draws, averageScore, totalPlayTimeSeconds).

### 18. Backend sync engine for game results
- **GameResultSyncEngine**: Background sync engine that pushes unsynced game results to the backend.
  - Queries unsynced results via `GameResultDao.getUnSyncedResults()`.
  - Posts results in configurable batches (default 20) to `POST /mobile/games/events`.
  - Marks results as synced locally via `GameResultDao.markAsSynced()` on success.
  - Exponential backoff retry (base 2s, max 60s, 3 attempts per batch).
  - Partial sync support: continues with next batch even if one fails.
  - Observable `SyncState` (isSyncing, pendingCount, lastSyncedCount, lastError).
- **GamesHttpClient.syncGameResults()**: New endpoint method for POSTing `GameResultSyncRequest` with bearer auth.
- **DTOs**: `GameResultSyncRequest`, `GameEventDto`, `SyncResponse` for serialization.
- **GamesUseCase**: Added `syncPendingResults()` and `getPendingSyncCount()` to interface.
- **GamesRepository**: Implemented sync methods — delegates to `GameResultSyncEngine` in non-dev mode; in dev mode, auto-marks results as synced locally.
- **DI**: `GameResultSyncEngine` registered as singleton in `DataModule`, injected into `GamesRepository`.
- **HistoryScreen**: Sync banner with pending count indicator and "Sincronizar" button. Shows progress spinner during sync and success/error feedback message.

## Build validation

- Android Kotlin compile: successful (devDebug).
- No compilation warnings in application code.
- Desktop JVM Kotlin compile: successful.
- iOS build validation pending macOS/Xcode execution.

## Integration notes

### Auth path
1. App uses Firebase/Google Sign-In to obtain idToken + user metadata (email, displayName).
2. App calls api-gateway `POST /v1/backoffice/auth/session` → bff-backoffice → microservice-users for session sync.
3. Backend returns `{ firebaseUid, role }` (no email/displayName).
4. App merges Firebase metadata with backend role data.
5. Session data is cached locally.

### Games path
1. App requests categories from api-gateway → bff-mobile → microservice-quizz.
2. User requests random/generated game models.
3. Games are cached locally.
4. Results are saved locally with `synced = false`.
5. Sync engine posts unsynced results via `POST /mobile/games/events` → bff-mobile → microservice-users.
6. On success, results are marked `synced = true` in local DB.
7. User can manually trigger sync from HistoryScreen banner.

### Environment distribution
| Environment | API URL                    | Auth mode | Use case                   |
|-------------|----------------------------|-----------|----------------------------|
| dev         | http://10.0.2.2:7005       | dev       | Local development          |
| stg         | http://localhost:7005      | firebase  | VPS staging / real backend |
| prod        | https://api.axiomnode.com  | firebase  | Production (future)        |

## Current non-finalized areas

The application surface summarized here is already implemented, but these points are still not closed as a fully validated cross-platform baseline:

1. iOS Google Sign-In and Desktop OAuth sign-in remain incomplete.
2. Release keystore creation and bundled font assets are still pending.
3. Real stg/prod verification of some game HTTP paths remains outstanding.
4. Some quality gates are still open, especially ViewModel and repository automated coverage.

## MVP checklist

- [x] KMP base structure (Android, iOS, Desktop)
- [x] Multiplatform Room setup
- [x] Auth scaffolding
- [x] Games list scaffolding
- [x] expect/actual DI strategy
- [x] Android/JVM compile validation
- [x] Environment config system (dev/stg/prod)
- [x] Android product flavors & signing config
- [x] Google Sign-In (Android Credential Manager + Firebase Auth)
- [x] Edge API token integration in HTTP layer
- [x] Material 3 theme system (6 color schemes, typography)
- [x] UI redesign (AuthScreen, GamesScreen, ProfileScreen)
- [x] Bottom navigation (Games + History + Profile tabs)
- [x] Network security config for local dev
- [x] Build variant → environment auto-detection
- [x] Quiz gameplay (GamePlayScreen + GamePlayViewModel)
- [x] Wordpass gameplay (text-input mode in GamePlayScreen)
- [x] Backend API alignment (correct URLs + response models)
- [x] GamesRepository full implementation
- [x] Dev-mode offline bypass (auth + games mock data)
- [x] Game history screen with results list
- [x] Profile screen with game statistics
- [x] GamesScreen category filter chips
- [ ] GamesHttpClient URL verification (stg/prod)
- [x] Backend sync completion (game results sync engine)
- [ ] iOS Google Sign-In (Firebase Auth iOS SDK)
- [ ] Desktop OAuth Sign-In (PKCE browser flow)
- [ ] Release signing keystore creation
- [ ] Bundle Syne + Inter font .ttf files
- [ ] Leaderboard UI

## Status

- Current status: Fully functional MVP with dev-mode offline support. Auth (Google Sign-In + Firebase), Quiz/Wordpass gameplay, game history, profile stats, game results sync engine, Material 3 UI, 3-tab navigation.
- Last updated: 11 Apr 2026.
