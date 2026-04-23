package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.db.UserProfileDao
import es.sebas1705.axiomnode.data.entities.UserProfileEntity
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
    private val userProfileDao: UserProfileDao,
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
            userProfileDao.upsertProfile(UserProfileEntity.fromDomain(user))
            return Result.success(user)
        }

        return httpClient.syncSessionFromFirebase(idToken)
            .map { user ->
                user.copy(
                    email = email.ifEmpty { user.email },
                    displayName = displayName ?: user.displayName,
                    photoUrl = photoUrl ?: user.photoUrl,
                )
            }
            .mapCatching { sessionUser ->
                val enriched = httpClient.getUserProfile(idToken).getOrNull()
                val cached = userProfileDao.getLastProfile()?.toDomain()
                val enrichedUid = enriched?.firebaseUid
                val enrichedEmail = enriched?.email

                // Prefer newest backend profile. If it fails, keep usable data from cache/session.
                val resolved = (enriched ?: cached ?: sessionUser).copy(
                    firebaseUid = if (enrichedUid.isNullOrBlank()) sessionUser.firebaseUid else enrichedUid,
                    email = when {
                        !email.isBlank() -> email
                        !enrichedEmail.isNullOrBlank() -> enrichedEmail
                        !cached?.email.isNullOrBlank() -> cached.email
                        else -> sessionUser.email
                    },
                    displayName = displayName ?: enriched?.displayName ?: cached?.displayName ?: sessionUser.displayName,
                    photoUrl = photoUrl ?: enriched?.photoUrl ?: cached?.photoUrl ?: sessionUser.photoUrl,
                    role = enriched?.role ?: cached?.role ?: sessionUser.role,
                )

                userProfileDao.upsertProfile(UserProfileEntity.fromDomain(resolved))
                resolved
            }
            .onSuccess { user ->
                currentUser = user
                authToken = idToken
            }
    }

    override suspend fun getCurrentUser(): Result<User?> {
        currentUser?.let { return Result.success(it) }

        val cachedProfile = userProfileDao.getLastProfile()?.toDomain()
        val token = authToken.orEmpty()

        if (token.isBlank()) {
            currentUser = cachedProfile
            return Result.success(cachedProfile)
        }

        return httpClient.getUserProfile(token)
            .onSuccess { remote ->
                currentUser = remote
                userProfileDao.upsertProfile(UserProfileEntity.fromDomain(remote))
            }
            .map { it as User? }
            .recoverCatching {
                cachedProfile
            }
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        authToken = null
        return Result.success(Unit)
    }
}

