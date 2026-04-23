package es.sebas1705.axiomnode.auth

/**
 * Common contract used by AuthViewModel so it can be faked in commonTest.
 */
interface GoogleSignInClient {
    suspend fun signIn(): GoogleSignInResult

    suspend fun signOut()
}

/**
 * Platform-specific Google Sign-In launcher.
 * Each platform provides its own implementation.
 */
expect class GoogleSignInService : GoogleSignInClient {
    /**
     * Launches the Google Sign-In flow and returns the result.
     * On Android this uses Credential Manager + Firebase Auth.
     * On iOS this will use Firebase Auth iOS SDK.
     * On Desktop this uses dev-mode bypass.
     */
    override suspend fun signIn(): GoogleSignInResult

    /**
     * Signs out the current user from Firebase/Google.
     */
    override suspend fun signOut()
}

