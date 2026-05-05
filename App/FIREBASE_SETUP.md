# Firebase & Google Sign-In Setup Guide

Last updated: 2026-05-03.

## Purpose

Document the current Firebase and Google Sign-In setup for the mobile app module.

## Overview

The mobile app uses Firebase-backed Google Sign-In, but the implementation is not symmetric across platforms.

Current state:

- Android implements Google Sign-In through Credential Manager plus Firebase Auth.
- iOS currently uses a stub implementation and can only complete sign-in in `AUTH_MODE=dev`.
- Desktop JVM currently uses a stub implementation and can only complete sign-in in `AUTH_MODE=dev`.

When Firebase-backed sign-in succeeds, the app sends the resulting token through the mobile edge path to create or update the player profile.

## Runtime flow

```
User → Google Sign-In → Firebase Auth → idToken
                                          ↓
                              Mobile App sends token + profile hints
                                          ↓
                     api-gateway → bff-mobile → player profile upsert/read
```

Current mobile auth routes used by the app:

- `PUT /v1/mobile/player/profile`
- `GET /v1/mobile/player/profile`

## Step-by-step setup

### 1. Firebase Console – Register platforms

Go to [Firebase Console](https://console.firebase.google.com) → Project **axiomnode**.

#### 1a. Register Android app(s)

You need to register **3 Android apps** (one per product flavor):

| Flavor | Package name                    | Nickname       |
|--------|---------------------------------|----------------|
| dev    | `es.sebas1705.axiomnode.dev`    | AxiomNode Dev  |
| stg    | `es.sebas1705.axiomnode.stg`    | AxiomNode Stg  |
| prod   | `es.sebas1705.axiomnode`        | AxiomNode      |

For each, you need the **SHA-1 fingerprint** of the signing certificate.

##### Get debug SHA-1 (for dev/stg):
```bash
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# macOS/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

##### Get release SHA-1 (for prod):
```bash
keytool -list -v -keystore path/to/release.keystore -alias your_alias
```

> **Important**: Add BOTH SHA-1 AND SHA-256 fingerprints in Firebase for each app.

#### 1b. Register iOS app

| Field             | Value                                        |
|-------------------|----------------------------------------------|
| Bundle ID         | `es.sebas1705.axiomnode.AxiomNode`           |
| App nickname      | AxiomNode iOS                                |
| App Store ID      | (leave empty for now)                         |

#### 1c. Web client (already exists)

The backoffice web app is already registered. The **Web client ID** from this registration is what you use as `GOOGLE_WEB_CLIENT_ID` in the mobile app (for backend token verification).

### 2. Download config files

#### Android: `google-services.json`

1. In Firebase Console → Project Settings → General tab
2. Scroll to **Your apps** → Android app
3. Click **google-services.json** download
4. Store it in the private vault at: `secrets/runtime/private-files/mobile-app/shared/App/composeApp/google-services.json`
5. Run: `node secrets/scripts/prepare-runtime-secrets.mjs <dev|stg|pro> mobile-app`

> Since all 3 Android flavors use the same Firebase project, a single `google-services.json` with all 3 client entries works. Firebase generates it with all registered package names.

#### iOS: `GoogleService-Info.plist`

1. In Firebase Console → Project Settings → General tab
2. Scroll to **Your apps** → iOS app
3. Click **GoogleService-Info.plist** download
4. Store it in the private vault at: `secrets/runtime/private-files/mobile-app/shared/App/iosApp/iosApp/GoogleService-Info.plist`
5. Run: `node secrets/scripts/prepare-runtime-secrets.mjs <dev|stg|pro> mobile-app`
6. In Xcode, add the injected file to the iosApp target if the project has not referenced it yet.

At the moment this file is preparatory for the future iOS Firebase integration. The current iOS sign-in implementation still falls back to `AUTH_MODE=dev`.

### 3. Get the Web Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com) → APIs & Credentials
2. Select project **axiomnode**
3. Under **OAuth 2.0 Client IDs**, find the **Web client** (auto created by Firebase)
4. Copy the **Client ID** (looks like `909798677135-xxxxx.apps.googleusercontent.com`)
5. Set this as `MOBILE_GOOGLE_WEB_CLIENT_ID` in:
   - `secrets/runtime/repositories/mobile-app/dev.env`
   - `secrets/runtime/repositories/mobile-app/stg.env`
   - `secrets/runtime/repositories/mobile-app/pro.env`

### 4. Android signing setup

#### Debug keystore (automatic)
- Android Studio uses `~/.android/debug.keystore` automatically
- The debug SHA-1 must be registered in Firebase for Google Sign-In to work

#### Release keystore
1. Generate a keystore:
   ```bash
   keytool -genkeypair -v -keystore axiomnode-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias axiomnode
   ```
2. Store securely (do NOT commit to repo)
3. Set values in `secrets/runtime/repositories/mobile-app/pro.env`:
   ```properties
   MOBILE_ANDROID_KEYSTORE_FILE=path/to/axiomnode-release.jks
   MOBILE_ANDROID_KEYSTORE_PASSWORD=your_store_password
   MOBILE_ANDROID_KEY_ALIAS=axiomnode
   MOBILE_ANDROID_KEY_PASSWORD=your_key_password
   ```
4. Re-run `node secrets/scripts/prepare-runtime-secrets.mjs pro mobile-app`

### 5. iOS signing setup

- Configure in Xcode → Signing & Capabilities
- Ensure the bundle ID matches what's registered in Firebase
- Treat this as groundwork for future native iOS Firebase integration rather than a fully active runtime path today

### 6. Desktop (JVM)

Desktop does **not** use `google-services.json` or Firebase SDKs directly.

Current state:

- the desktop sign-in implementation is still a stub
- `AUTH_MODE=dev` is the supported local path for desktop validation
- production-grade desktop OAuth flow is not yet implemented in this module

## Environment files summary

| File                           | Purpose                          | Gitignored |
|--------------------------------|----------------------------------|------------|
| `composeApp/env/dev.properties`| Dev environment config           | ✅         |
| `composeApp/env/stg.properties`| Staging environment config       | ✅         |
| `composeApp/env/prod.properties`| Production environment config   | ✅         |
| `composeApp/env/env.properties.example` | Template (committed)    | ❌         |
| `composeApp/google-services.json` | Android Firebase config       | ✅         |
| `iosApp/iosApp/GoogleService-Info.plist` | iOS Firebase config    | ✅         |

## Build commands per environment

```bash
# Dev (default)
./gradlew :androidApp:assembleDevDebug

# Staging
./gradlew :androidApp:assembleStgDebug -Paxiomnode.env=stg

# Production release
./gradlew :androidApp:assembleProdRelease -Paxiomnode.env=prod

# Desktop
./gradlew :composeApp:run -Paxiomnode.env=dev
```

## Secrets repo integration

The `secrets` repo is the only vault for mobile runtime values and private files.

Canonical sources:

- `secrets/runtime/repositories/mobile-app/{dev,stg,pro}.env`
- `secrets/runtime/private-files/mobile-app/shared/App/composeApp/google-services.json`
- `secrets/runtime/private-files/mobile-app/shared/App/iosApp/iosApp/GoogleService-Info.plist`

To sync:

```bash
# From AxiomNode root (where secrets/ is)
node secrets/scripts/prepare-runtime-secrets.mjs dev mobile-app
```

The injector now:

- writes `mobile-app/App/composeApp/env/{dev,stg,prod}.properties`
- copies `google-services.json` into `mobile-app/App/composeApp/`
- copies `GoogleService-Info.plist` into `mobile-app/App/iosApp/iosApp/`

## Current implementation notes

- Android `GoogleSignInService` uses Credential Manager and exchanges the Google ID token with Firebase Auth before calling the mobile profile endpoints.
- iOS and JVM currently return dev-only stub users when `AUTH_MODE=dev` and otherwise fail with a not-implemented error.
- The app config is generated from `composeApp/env/*.properties` and includes `AUTH_MODE`, Firebase keys, and `GOOGLE_WEB_CLIENT_ID`.

## Checklist

- [ ] Register all 3 Android package names in Firebase
- [ ] Add debug SHA-1 fingerprint to Firebase (for dev flavor)
- [ ] Store `google-services.json` in `secrets/runtime/private-files/mobile-app/shared/App/composeApp/`
- [ ] Register iOS app in Firebase
- [ ] Store `GoogleService-Info.plist` in `secrets/runtime/private-files/mobile-app/shared/App/iosApp/iosApp/`
- [ ] Copy Web Client ID to `secrets/runtime/repositories/mobile-app/*.env`
- [ ] Generate release keystore for production builds
- [ ] Re-run `node secrets/scripts/prepare-runtime-secrets.mjs <env> mobile-app`
- [ ] Complete native iOS Firebase sign-in implementation if `AUTH_MODE=firebase` is required there
- [ ] Complete desktop OAuth flow if desktop needs non-dev authentication

