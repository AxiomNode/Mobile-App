# mobile-app

Mobile client application workspace for AxiomNode.

## Scope

- Contains mobile client source and platform-specific app modules.
- Integrates with edge APIs exposed by `api-gateway`.
- Shares authentication and contract conventions with the backend stack.

## Current repository status

- This repository includes a baseline CI workflow in `.github/workflows/ci.yml`.
- The workflow compiles shared JVM sources and builds the Android debug artifact on pushes, pull requests, and manual dispatches.
- CI/CD automation for backend deployment is managed in service repositories and `platform-infra`.

## Recommended next additions

1. Fix the current `:composeApp:jvmTest` Gradle failure and promote JVM tests into CI.
2. Add lint/static analysis coverage for Kotlin and Android resources.
3. Add optional preview/distribution workflow for development builds.
4. Document runtime environment variables and API base URL strategy.
