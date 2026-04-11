# Firebase & Google Sign-In Setup Guide

## Overview

The AxiomNode mobile app uses **Firebase Authentication** with **Google Sign-In** as the primary auth provider. The idToken obtained from Firebase is sent to the backend (`microservice-users`) for session management.

## Architecture

```
User → Google Sign-In → Firebase Auth → idToken
                                          ↓
                              Mobile App sends idToken
                                          ↓
                     api-gateway → bff-mobile → microservice-users
                                                (verifies with Firebase Admin SDK)
```

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
- Set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`
- Enable "Sign in with Apple" capability if desired (optional)
- Ensure the bundle ID matches what's registered in Firebase

### 6. Desktop (JVM)

Desktop does **not** use `google-services.json` or Firebase SDKs directly.

Strategy:
- Opens system browser for Google OAuth2 (PKCE flow)
- Receives callback with auth code
- Exchanges for token via backend
- Or: use Firebase REST API with the web API key

For MVP: Desktop can use the `dev` auth mode (bypass Firebase) during development.

## Environment files summary

| File                           | Purpose                          | Gitignored |
|--------------------------------|----------------------------------|------------|
| `composeApp/env/dev.properties`| Dev environment config           | ✅         |
| `composeApp/env/stg.properties`| Staging environment config       | ✅         |
| `composeApp/env/prod.properties`| Production environment config   | ✅         |
| `composeApp/env/env.properties.example` | Template (committed)    | ❌         |
| `composeApp/google-services.json` | Android Firebase config       | ✅         |
| `composeApp/google-services.json.example` | Template (committed)  | ❌         |
| `iosApp/iosApp/GoogleService-Info.plist` | iOS Firebase config    | ✅         |

## Build commands per environment

```bash
# Dev (default)
./gradlew :composeApp:assembleDevDebug

# Staging
./gradlew :composeApp:assembleStgDebug -Paxiomnode.env=stg

# Production release
./gradlew :composeApp:assembleProdRelease -Paxiomnode.env=prod

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

## Checklist

- [ ] Register all 3 Android package names in Firebase
- [ ] Add debug SHA-1 fingerprint to Firebase (for dev flavor)
- [ ] Store `google-services.json` in `secrets/runtime/private-files/mobile-app/shared/App/composeApp/`
- [ ] Register iOS app in Firebase
- [ ] Store `GoogleService-Info.plist` in `secrets/runtime/private-files/mobile-app/shared/App/iosApp/iosApp/`
- [ ] Copy Web Client ID to `secrets/runtime/repositories/mobile-app/*.env`
- [ ] Generate release keystore for production builds
- [ ] Re-run `node secrets/scripts/prepare-runtime-secrets.mjs <env> mobile-app`
- [ ] Set `TEAM_ID` in iOS `Config.xcconfig`

