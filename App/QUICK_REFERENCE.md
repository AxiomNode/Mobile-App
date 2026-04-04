# AxiomNode Mobile - Quick Reference

## Build commands

### Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:installDebug

### Desktop JVM
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS
./gradlew :composeApp:compileKotlinJvm

### iOS
./gradlew :composeApp:linkDebugFrameworkIosArm64
./gradlew :composeApp:compileDebugKotlinIosArm64
open iosApp/iosApp.xcodeproj

## Test commands

### Unit tests
./gradlew :composeApp:test
./gradlew :composeApp:testDebugUnitTest

### Instrumented tests
./gradlew :composeApp:connectedAndroidTest

## Lint and dependency checks

./gradlew :composeApp:lintDebug
./gradlew dependencies
./gradlew :composeApp:dependencies

## Clean commands

./gradlew clean
./gradlew :composeApp:clean

## Continuous development helpers

./gradlew :composeApp:compileDebugKotlinAndroid --continuous
./gradlew :composeApp:run --continuous

## Debugging helpers

./gradlew :composeApp:run --debug-jvm

## Dependency edit pattern

1. Add version and library in gradle/libs.versions.toml.
2. Reference the alias in composeApp/build.gradle.kts.
3. Re-run a target compile command.

## Common troubleshooting

### expect/actual mismatch
- Verify the symbol exists in commonMain and all platform source sets.
- Re-run Android and JVM compilation tasks.

### Room symbol or generated code issues
- Run KSP tasks for the impacted targets.

### Gradle cache corruption
./gradlew clean --no-daemon
rm -rf .gradle
./gradlew :composeApp:build --no-daemon

## Git essentials

git status
git diff
git add .
git commit -m "feat: ..."
git push origin main

## Useful references

- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
- Compose Multiplatform: https://github.com/JetBrains/compose-multiplatform
- Room docs: https://developer.android.com/training/data-storage/room
- Ktor docs: https://ktor.io/docs/client.html
- Koin docs: https://insert-koin.io/

## Notes

- Hot reload is limited in KMP; rebuild cycles are expected.
- iOS build/run requires a macOS/Xcode environment.
- Keep expect/actual declarations synchronized across targets.
