# AGENTS

## Repo purpose
Kotlin Multiplatform mobile workspace for AxiomNode clients (Android, iOS, Desktop JVM).

## Key paths
- App/composeApp/src/commonMain: shared domain/data/presentation logic
- App/composeApp/src/androidMain|iosMain|jvmMain: platform actual providers
- App/README.md and App/*.md: app-focused documentation

## Local commands
- cd App && ./gradlew :composeApp:assembleDebug
- cd App && ./gradlew :composeApp:run
- cd App && ./gradlew :composeApp:compileKotlinJvm

## CI/CD notes
- No dedicated repo workflow currently; backend deployment is handled elsewhere.

## LLM editing rules
- Keep expect/actual contracts synchronized across targets.
- Prefer minimal cross-platform abstractions over platform-specific duplication.
- Document build/toolchain changes in App docs.
