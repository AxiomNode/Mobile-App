# AxiomNode Mobile Quick Reference

Last updated: 2026-05-03.

## Purpose

Fast command and troubleshooting reference for the current `App` module layout.

## Build commands

### Android

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleStgDebug
./gradlew :androidApp:assembleProdRelease
```

### Shared module compilation

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinJvm
```

### Desktop JVM

```bash
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS
```

### iOS

```bash
./gradlew :composeApp:linkDebugFrameworkIosArm64
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Run the iOS host app from `iosApp` in Xcode on macOS.

## Validation commands

```bash
./gradlew :composeApp:jvmTest
./gradlew :androidApp:lint
./gradlew :androidApp:assembleDebug
```

## Environment selection

The generated app config resolves environment from the task name or from an explicit property:

```bash
./gradlew :androidApp:assembleStgDebug
./gradlew :composeApp:run -Paxiomnode.env=dev
```

Available Android flavor names are `dev`, `stg`, and `prod`.

## Clean commands

```bash
./gradlew clean
./gradlew :androidApp:clean
./gradlew :composeApp:clean
```

## Troubleshooting

### expect/actual mismatch

- Verify the declaration exists in `commonMain` and in each required platform source set.
- Re-run one Android compile task and one JVM or iOS link task.

### Generated config or env issues

- Check `composeApp/env/*.properties` inputs.
- Confirm `API_BASE_URL` points to the public edge and not to an internal service hostname.
- Re-run a task with `-Paxiomnode.env=<env>` if task-name inference is ambiguous.

### Room or KSP issues

- Re-run the affected compile task after cleaning the module.
- Verify the dependency change was added through `gradle/libs.versions.toml` and referenced from `composeApp/build.gradle.kts`.

### Gradle cache issues

```bash
./gradlew clean --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
```

## Related documents

- [README.md](README.md)
- [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
- [MVP_README.md](MVP_README.md)
