package es.sebas1705.axiomnode.testsupport

import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.models.UserRole
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase

class FakeAuthUseCase : AuthUseCase {
    var signInWithGoogleResult: Result<User> = Result.success(sampleUser())
    var currentUserResult: Result<User?> = Result.success(null)
    var signOutResult: Result<Unit> = Result.success(Unit)

    var signInWithGoogleCalls: Int = 0
        private set
    var getCurrentUserCalls: Int = 0
        private set
    var signOutCalls: Int = 0
        private set

    var lastIdToken: String? = null
        private set
    var lastEmail: String? = null
        private set
    var lastDisplayName: String? = null
        private set
    var lastPhotoUrl: String? = null
        private set

    override suspend fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String?,
        photoUrl: String?,
    ): Result<User> {
        signInWithGoogleCalls++
        lastIdToken = idToken
        lastEmail = email
        lastDisplayName = displayName
        lastPhotoUrl = photoUrl
        return signInWithGoogleResult
    }

    override suspend fun getCurrentUser(): Result<User?> {
        getCurrentUserCalls++
        return currentUserResult
    }

    override suspend fun signOut(): Result<Unit> {
        signOutCalls++
        return signOutResult
    }
}

fun sampleUser(
    firebaseUid: String = "uid-1",
    email: String = "test@axiomnode.es",
    displayName: String? = "Test User",
    photoUrl: String? = null,
    role: UserRole = UserRole.GAMER,
): User = User(
    firebaseUid = firebaseUid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    role = role,
)
