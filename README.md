# mobile-app

Last updated: 2026-05-08.

Mobile client application workspace for AxiomNode.

## Responsibility

- Contains mobile client source and platform-specific app modules.
- Integrates with edge APIs exposed by `api-gateway`.
- Shares authentication and contract conventions with the backend stack.

## Runtime role

### Runtime position

`mobile-app` is a client repository, not part of the Kubernetes runtime plane.

Its stable backend dependency is the public edge contract exposed by `api-gateway`. It should not depend on direct knowledge of BFF or microservice hostnames.

Current platform status: Android/shared KMP validation is covered by repository CI, while backend runtime rollout remains outside this repository and is handled by the service repos plus `platform-infra`.

## Runtime surface

### Current repository status

- This repository includes a baseline CI workflow in `.github/workflows/ci.yml`.
- The Android client is now split between `App/composeApp` (shared KMP code) and `App/androidApp` (Android application entrypoint).
- The workflow runs `:composeApp:jvmTest`, executes `:androidApp:lint`, builds the Android debug artifact from `:androidApp`, and links the iOS simulator framework on macOS runners for baseline Apple validation.
- iOS targets are registered only on macOS hosts so Windows and Linux validation can run shared JVM tests without pulling native Apple variants.
- CI/CD automation for backend deployment is managed in service repositories and `platform-infra`.

Build, platform, and toolchain detail belongs in `App/README.md` and the other local mobile documents so this root README stays focused on repository role.

## Dependencies and contracts

### Integration contract

The mobile client should treat the backend as:

1. a stable public gateway contract
2. environment-specific auth configuration
3. versioned shared contract semantics sourced from `contracts-and-schemas`

## Documentation

- `App/README.md`
- `App/QUICK_REFERENCE.md`
- `App/FIREBASE_SETUP.md`
- `App/MVP_README.md`

## Deployment and operations notes

- this repository is validated in its own CI but is outside the centralized GHCR-to-k3s runtime rollout chain
- backend deployment automation remains owned by service repositories and `platform-infra`

## References

- `../docs/guides/experience/mobile-app-development-firebase.md`
