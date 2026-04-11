package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.network.AuthHttpClient
import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.models.UserRole
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase

/**
 * Implementacion del caso de uso de autenticacion.
 * In dev mode, skips backend calls and creates a local-only session.
 */
class AuthRepository(
    private val httpClient: AuthHttpClient,
    private val config: AppConfig,
) : AuthUseCase {
    private var currentUser: User? = null
    private var authToken: String? = null

    override suspend fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String?,
        photoUrl: String?,
    ): Result<User> {
        // Dev mode: skip backend entirely, create mock user from Firebase metadata
        if (config.isDevAuth) {
            val user = User(
                firebaseUid = "dev-uid",
                email = email.ifEmpty { "dev@axiomnode.es" },
                displayName = displayName ?: "Dev User",
                photoUrl = photoUrl,
                role = UserRole.GAMER,
            )
            currentUser = user
            authToken = idToken
            return Result.success(user)
        }

        return httpClient.syncSessionFromFirebase(idToken)
            .map { user ->
                // Merge Google metadata when backend returns empty values
                user.copy(
                    email = email.ifEmpty { user.email },
                    displayName = displayName ?: user.displayName,
                    photoUrl = photoUrl ?: user.photoUrl,
                )
            }
            .onSuccess { user ->
                currentUser = user
                authToken = idToken
            }
    }

    override suspend fun getCurrentUser(): Result<User?> {
        return if (currentUser != null && authToken != null) {
            Result.success(currentUser)
        } else {
            Result.success(null)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        authToken = null
        return Result.success(Unit)
    }
}

