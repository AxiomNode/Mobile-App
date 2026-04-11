package es.sebas1705.axiomnode.auth

/**
 * Result of a Google Sign-In attempt.
 */
sealed class GoogleSignInResult {
    /** Sign-in succeeded. Contains the Firebase idToken and user metadata. */
    data class Success(
        val idToken: String,
        val email: String = "",
        val displayName: String? = null,
        val photoUrl: String? = null,
    ) : GoogleSignInResult()

    /** Sign-in was cancelled by the user. */
    data object Cancelled : GoogleSignInResult()

    /** Sign-in failed with an error. */
    data class Error(val message: String) : GoogleSignInResult()
}

