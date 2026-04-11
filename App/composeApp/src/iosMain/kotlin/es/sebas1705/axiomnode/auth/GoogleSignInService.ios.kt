package es.sebas1705.axiomnode.auth

import es.sebas1705.axiomnode.config.AppConfig

/**
 * iOS Google Sign-In stub.
 * TODO: Integrate Firebase Auth iOS SDK via CocoaPods/SPM.
 * For now, uses dev-mode bypass when AUTH_MODE=dev.
 */
actual class GoogleSignInService(
    private val config: AppConfig,
) {
    actual suspend fun signIn(): GoogleSignInResult {
        if (config.isDevAuth) {
            // Dev mode: return a fake token for local testing
            return GoogleSignInResult.Success(
                idToken = "dev-ios-token",
                email = "dev@axiomnode.es",
                displayName = "iOS Dev User",
            )
        }
        return GoogleSignInResult.Error("Firebase iOS Sign-In not yet implemented. Set AUTH_MODE=dev for testing.")
    }

    actual suspend fun signOut() {
        // No-op for now
    }
}

