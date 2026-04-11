package es.sebas1705.axiomnode.auth

import es.sebas1705.axiomnode.config.AppConfig

/**
 * Desktop/JVM Google Sign-In stub.
 * TODO: Implement OAuth2 PKCE browser flow for production.
 * For now, uses dev-mode bypass when AUTH_MODE=dev.
 */
actual class GoogleSignInService(
    private val config: AppConfig,
) {
    actual suspend fun signIn(): GoogleSignInResult {
        if (config.isDevAuth) {
            // Dev mode: return a fake token for local testing
            return GoogleSignInResult.Success(
                idToken = "dev-desktop-token",
                email = "dev@axiomnode.es",
                displayName = "Desktop Dev User",
            )
        }
        return GoogleSignInResult.Error("Desktop OAuth Sign-In not yet implemented. Set AUTH_MODE=dev for testing.")
    }

    actual suspend fun signOut() {
        // No-op for now
    }
}

