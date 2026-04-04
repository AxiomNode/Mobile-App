package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.data.network.AuthHttpClient
import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase

/**
 * Implementacion del caso de uso de autenticacion.
 */
class AuthRepository(
    private val httpClient: AuthHttpClient,
) : AuthUseCase {
    private var currentUser: User? = null
    private var authToken: String? = null

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return httpClient.syncSessionFromFirebase(idToken)
            .onSuccess { user ->
                currentUser = user
                authToken = idToken // En produccion, usar JWT del backend
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

