package es.sebas1705.axiomnode.testsupport

import es.sebas1705.axiomnode.auth.GoogleSignInClient
import es.sebas1705.axiomnode.auth.GoogleSignInResult

class FakeGoogleSignInClient : GoogleSignInClient {
    var signInResult: GoogleSignInResult = GoogleSignInResult.Cancelled

    var signInCalls: Int = 0
        private set
    var signOutCalls: Int = 0
        private set

    override suspend fun signIn(): GoogleSignInResult {
        signInCalls++
        return signInResult
    }

    override suspend fun signOut() {
        signOutCalls++
    }
}
