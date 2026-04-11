package es.sebas1705.axiomnode.domain.usecases

import es.sebas1705.axiomnode.domain.models.User

/**
 * Caso de uso para autenticacion con Google y sincronizacion en backend.
 */
interface AuthUseCase {
    suspend fun signInWithGoogle(
        idToken: String,
        email: String = "",
        displayName: String? = null,
        photoUrl: String? = null,
    ): Result<User>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun signOut(): Result<Unit>
}

