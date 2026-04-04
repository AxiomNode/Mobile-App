# AxiomNode Mobile MVP - Final Delivery

## Delivery status

- Development session: 31 Mar 2026
- Scope delivered: cleanup + MVP app architecture (Auth and Games)
- Build verification: Android and JVM successful, iOS pending host validation

## Main deliverables

### 1. Repository cleanup
- Removed deprecated Country datasource files and references.
- Consolidated project around current MVP requirements.

### 2. Multiplatform architecture foundation
- Domain layer: models and use case interfaces.
- Data layer: Room, DAOs, repositories, and Ktor clients.
- Presentation layer: Auth and Games screens with stateful ViewModels.
- DI layer: Koin modules with platform-specific bindings.

### 3. Backend integration baseline
- Auth client integrated with microservice-users endpoints.
- Games client integrated with microservice-quizz endpoints.
- Typed DTO contracts via Kotlin serialization.

### 4. Persistence model
- Android: Room integration through AGP/KSP.
- iOS: Room with bundled SQLite driver.
- Desktop JVM: Room with bundled SQLite driver.

### 5. Build validation
- composeApp:compileDebugKotlinAndroid -> successful.
- composeApp:compileKotlinJvm -> successful.
- iOS build/test execution pending macOS environment.

## Delivered file groups

### Domain
- User model and roles.
- Game, Question, GameResult models.
- GameCatalog models.

### Use cases
- AuthUseCase.
- GamesUseCase.

### Data
- GameEntity and GameResultEntity.
- GameDao, GameResultDao, and AxiomNodeDatabase.
- AuthHttpClient and GamesHttpClient.
- AuthRepository and GamesRepository.

### Presentation
- AuthScreen and AuthViewModel.
- GamesScreen and GamesViewModel.
- AppNavigation wiring in App.kt.

### Platform DI
- DatabaseProvider actual implementations for Android, iOS, and JVM.
- Platform module definitions for ViewModel bindings.

### Build/config updates
- composeApp/build.gradle.kts.
- gradle/libs.versions.toml.
- gradle-wrapper.properties and related build settings.

## Functional flow summary

### Authentication flow
1. User enters sign-in path.
2. App exchanges token with microservice-users session endpoint.
3. Session is retained locally and app transitions to games area.

### Games flow
1. App loads games catalog.
2. User requests random/generated games.
3. App caches games locally.
4. App prepares result events for sync.

### Results flow
1. Gameplay creates result model.
2. Result is persisted in local DB with sync status.
3. Future sync stage posts events to microservice-users.

## Immediate roadmap

### Phase 1
- Implement full gameplay screen and scoring logic.

### Phase 2
- Complete production-ready Google Sign-In setup.

### Phase 3
- Implement resilient background sync for game results.

### Phase 4
- Add leaderboard and broader QA coverage.

## Security follow-up

### Implemented
- Token-based auth headers in clients.
- HTTPS-ready HTTP client architecture.
- Non-persistent token-first flow assumptions.

### Pending
- Certificate pinning strategy.
- Token refresh/expiry handling.
- Obfuscation/release hardening.

## Conclusion

The mobile MVP foundation is in place and technically validated on Android and Desktop JVM. The next major milestone is full gameplay implementation followed by production auth hardening and sync completion.
