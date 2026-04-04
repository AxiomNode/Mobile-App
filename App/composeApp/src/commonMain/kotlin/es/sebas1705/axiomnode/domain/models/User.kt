package es.sebas1705.axiomnode.domain.models

/**
 * Representa un usuario autenticado en la aplicacion.
 */
data class User(
    val firebaseUid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val role: UserRole = UserRole.GAMER,
)

enum class UserRole {
    SUPER_ADMIN, ADMIN, VIEWER, GAMER
}

