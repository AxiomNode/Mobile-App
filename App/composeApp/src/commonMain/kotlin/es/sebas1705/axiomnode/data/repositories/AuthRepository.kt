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

        return httpClient.syncSessionFromFirebase(
            idToken = idToken,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
        )
            .map { user ->
                user.copy(
                    email = email.ifEmpty { user.email },
                    displayName = displayName ?: user.displayName,
                    photoUrl = photoUrl ?: user.photoUrl,
                )
            }
            .mapCatching { sessionUser ->
                val enriched = httpClient.getUserProfile(idToken).getOrNull()
                val cached = userProfileDao.getProfileByUid(sessionUser.firebaseUid)?.toDomain()
                    ?: userProfileDao.getLastProfile()?.toDomain()
                val enrichedUid = enriched?.firebaseUid
                val enrichedEmail = enriched?.email
                val resolvedUid = sequenceOf(
                    enrichedUid,
                    sessionUser.firebaseUid,
                    cached?.firebaseUid,
                ).firstOrNull { !it.isNullOrBlank() }
                    ?: email.ifBlank { "anonymous-player" }

                // Prefer newest backend profile. If it fails, keep usable data from cache/session.
                val resolved = (enriched ?: cached ?: sessionUser).copy(
                    firebaseUid = resolvedUid,
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
        val cachedProfile = userProfileDao.getLastProfile()?.toDomain()
        val localSnapshot = currentUser ?: cachedProfile
        val token = authToken.orEmpty()

        if (token.isBlank()) {
            currentUser = localSnapshot
            return Result.success(localSnapshot)
        }

        return httpClient.getUserProfile(token)
            .onSuccess { remote ->
                val fallbackUid = sequenceOf(
                    remote.firebaseUid,
                    currentUser?.firebaseUid,
                    cachedProfile?.firebaseUid,
                ).firstOrNull { !it.isNullOrBlank() }
                    ?: remote.email.ifBlank { "anonymous-player" }
                val normalized = remote.copy(firebaseUid = fallbackUid)
                currentUser = normalized
                userProfileDao.upsertProfile(UserProfileEntity.fromDomain(normalized))
            }
            .map { it as User? }
            .recoverCatching {
                currentUser = localSnapshot
                localSnapshot
            }
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        authToken = null
        return Result.success(Unit)
    }
}

