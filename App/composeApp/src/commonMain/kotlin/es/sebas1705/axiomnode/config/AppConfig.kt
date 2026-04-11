package es.sebas1705.axiomnode.config

/**
 * Runtime configuration for the AxiomNode mobile app.
 * Values are injected at build time from environment property files.
 *
 * @see AppEnvironment
 */
data class AppConfig(
    val environment: AppEnvironment,
    val apiBaseUrl: String,
    val authMode: String,
    val firebaseApiKey: String,
    val firebaseAuthDomain: String,
    val firebaseProjectId: String,
    val firebaseStorageBucket: String,
    val firebaseMessagingSenderId: String,
    val firebaseAppId: String,
    val firebaseMeasurementId: String,
    val googleWebClientId: String,
) {
    val isDevAuth: Boolean get() = authMode == "dev"
    val isFirebaseAuth: Boolean get() = authMode == "firebase"
}

