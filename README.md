# mobile-app

Mobile client application workspace for AxiomNode.

## Scope

- Contains mobile client source and platform-specific app modules.
- Integrates with edge APIs exposed by `api-gateway`.
- Shares authentication and contract conventions with the backend stack.

## Runtime position

`mobile-app` is a client repository, not part of the Kubernetes runtime plane.

Its stable backend dependency is the public edge contract exposed by `api-gateway`. It should not depend on direct knowledge of BFF or microservice hostnames.

## Current repository status

- This repository includes a baseline CI workflow in `.github/workflows/ci.yml`.
- The Android client is now split between `App/composeApp` (shared KMP code) and `App/androidApp` (Android application entrypoint).
- The workflow runs `:composeApp:jvmTest`, executes `:androidApp:lint`, builds the Android debug artifact from `:androidApp`, and links the iOS simulator framework on macOS runners for baseline Apple validation.
- iOS targets are registered only on macOS hosts so Windows and Linux validation can run shared JVM tests without pulling native Apple variants.
- CI/CD automation for backend deployment is managed in service repositories and `platform-infra`.

## Integration contract

The mobile client should treat the backend as:

1. a stable public gateway contract
2. environment-specific auth configuration
3. versioned shared contract semantics sourced from `contracts-and-schemas`

## Recommended next additions

1. Add optional preview/distribution workflow for development builds.
2. Document runtime environment variables and API base URL strategy.
