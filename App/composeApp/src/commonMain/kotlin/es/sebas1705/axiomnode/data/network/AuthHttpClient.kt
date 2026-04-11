package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseSessionRequest(
    val idToken: String,
)

/**
 * Response from POST /v1/backoffice/auth/session
 * (api-gateway → bff-backoffice → microservice-users /users/firebase/session)
 */
@Serializable
data class SessionSyncResponse(
    val message: String? = null,
    val userId: String? = null,
    val firebaseUid: String? = null,
    val provider: String? = null,
    val role: String? = null,
) {
    fun toDomain(): User = User(
        firebaseUid = firebaseUid ?: userId ?: "",
        email = "", // Session endpoint doesn't return email; filled from Firebase Auth
        displayName = null,
        photoUrl = null,
        role = try {
            UserRole.valueOf(role?.uppercase() ?: "GAMER")
        } catch (_: Exception) {
            UserRole.GAMER
        },
    )
}

/**
 * Response from GET /v1/backoffice/auth/me
 * (api-gateway → bff-backoffice → microservice-users /users/me/profile)
 * Returns full profile with stats.
 */
@Serializable
data class UserProfileResponse(
    val profile: ProfileData? = null,
    val role: String? = null,
) {
    @Serializable
    data class ProfileData(
        val firebaseUid: String? = null,
        val email: String? = null,
        val displayName: String? = null,
        val photoUrl: String? = null,
    )

    fun toDomain(): User = User(
        firebaseUid = profile?.firebaseUid ?: "",
        email = profile?.email ?: "",
        displayName = profile?.displayName,
        photoUrl = profile?.photoUrl,
        role = try {
            UserRole.valueOf(role?.uppercase() ?: "GAMER")
        } catch (_: Exception) {
            UserRole.GAMER
        },
    )
}

/**
 * HTTP client for auth operations via api-gateway → bff-backoffice → microservice-users.
 *
 * @param baseUrl The api-gateway edge URL (e.g. http://10.0.2.2:7005).
 */
class AuthHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    /**
     * Sync Firebase session with backend.
     * Route: POST /v1/backoffice/auth/session
     */
    suspend fun syncSessionFromFirebase(idToken: String): Result<User> = try {
        val response = httpClient.post("$baseUrl/v1/backoffice/auth/session") {
            contentType(ContentType.Application.Json)
            setBody(FirebaseSessionRequest(idToken))
        }
        val session = response.body<SessionSyncResponse>()
        Result.success(session.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get the current user's full profile.
     * Route: GET /v1/backoffice/auth/me
     */
    suspend fun getUserProfile(authToken: String): Result<User> = try {
        val response = httpClient.get("$baseUrl/v1/backoffice/auth/me") {
            bearerAuth(authToken)
        }
        val profile = response.body<UserProfileResponse>()
        Result.success(profile.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
