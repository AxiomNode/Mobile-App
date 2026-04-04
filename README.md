# mobile-app

Mobile client application workspace for AxiomNode.

## Scope

- Contains mobile client source and platform-specific app modules.
- Integrates with edge APIs exposed by `api-gateway`.
- Shares authentication and contract conventions with the backend stack.

## Current repository status

- This repository currently has no dedicated GitHub Actions workflow in `.github/workflows`.
- CI/CD automation for backend deployment is managed in service repositories and `platform-infra`.

## Recommended next additions

1. Add a mobile CI workflow (build + unit tests + lint).
2. Add optional preview/distribution workflow for development builds.
3. Document runtime environment variables and API base URL strategy.
