# mobile-app

Mobile client application workspace for AxiomNode.

## Scope

- Contains mobile client source and platform-specific app modules.
- Integrates with edge APIs exposed by `api-gateway`.
- Shares authentication and contract conventions with the backend stack.

## Current repository status

- This repository includes a baseline CI workflow in `.github/workflows/ci.yml`.
- The Android client is now split between `App/composeApp` (shared KMP code) and `App/androidApp` (Android application entrypoint).
- The workflow runs `:composeApp:jvmTest`, builds the Android debug artifact from `:androidApp`, and links the iOS simulator framework on macOS runners for baseline Apple validation.
- iOS targets are registered only on macOS hosts so Windows and Linux validation can run shared JVM tests without pulling native Apple variants.
- CI/CD automation for backend deployment is managed in service repositories and `platform-infra`.

## Recommended next additions

1. Add lint/static analysis coverage for Kotlin and Android resources.
2. Add optional preview/distribution workflow for development builds.
3. Document runtime environment variables and API base URL strategy.
