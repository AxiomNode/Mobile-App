package es.sebas1705.axiomnode.config

/**
 * Desktop/JVM [AppConfig] provider.
 * Reads values from the build-time generated [GeneratedConfig].
 */
actual fun createAppConfig(): AppConfig = AppConfig(
    environment = AppEnvironment.valueOf(GeneratedConfig.ENVIRONMENT),
    apiBaseUrl = GeneratedConfig.API_BASE_URL,
    authMode = GeneratedConfig.AUTH_MODE,
    firebaseApiKey = GeneratedConfig.FIREBASE_API_KEY,
    firebaseAuthDomain = GeneratedConfig.FIREBASE_AUTH_DOMAIN,
    firebaseProjectId = GeneratedConfig.FIREBASE_PROJECT_ID,
    firebaseStorageBucket = GeneratedConfig.FIREBASE_STORAGE_BUCKET,
    firebaseMessagingSenderId = GeneratedConfig.FIREBASE_MESSAGING_SENDER_ID,
    firebaseAppId = GeneratedConfig.FIREBASE_APP_ID,
    firebaseMeasurementId = GeneratedConfig.FIREBASE_MEASUREMENT_ID,
    googleWebClientId = GeneratedConfig.GOOGLE_WEB_CLIENT_ID,
)

