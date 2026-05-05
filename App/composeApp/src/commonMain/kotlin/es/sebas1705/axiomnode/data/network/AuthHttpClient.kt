package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfileUpsertRequest(
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val preferredLanguage: String? = null,
)

/**
 * Response from bff-mobile player profile endpoints.
 * (api-gateway -> bff-mobile -> player store)
 */
@Serializable
data class PlayerProfileResponse(
    val profile: ProfileData? = null,
) {
    @Serializable
    data class ProfileData(
        val playerId: String? = null,
        val email: String? = null,
        val displayName: String? = null,
        val photoUrl: String? = null,
        val preferredLanguage: String? = null,
    )

    fun toDomain(): User = User(
        firebaseUid = profile?.playerId ?: "",
        email = profile?.email ?: "",
        displayName = profile?.displayName,
        photoUrl = profile?.photoUrl,
        role = UserRole.GAMER,
    )
}

/**
 * HTTP client for auth/player operations via api-gateway -> bff-mobile.
 *
 * @param baseUrl The api-gateway edge URL (e.g. http://10.0.2.2:7005).
 */
class AuthHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    /**
     * Ensures player profile exists in bff-mobile using the Firebase token claims.
     * Route: PUT /v1/mobile/player/profile
     */
    suspend fun syncSessionFromFirebase(
        idToken: String,
        email: String? = null,
        displayName: String? = null,
        photoUrl: String? = null,
    ): Result<User> = try {
        val response = httpClient.put("$baseUrl/v1/mobile/player/profile") {
            bearerAuth(idToken)
            contentType(ContentType.Application.Json)
            setBody(
                PlayerProfileUpsertRequest(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                ),
            )
        }
        val session = response.body<PlayerProfileResponse>()
        Result.success(session.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get the current player's profile from bff-mobile.
     * Route: GET /v1/mobile/player/profile
     */
    suspend fun getUserProfile(authToken: String): Result<User> = try {
        val response = httpClient.get("$baseUrl/v1/mobile/player/profile") {
            bearerAuth(authToken)
        }
        val profile = response.body<PlayerProfileResponse>()
        Result.success(profile.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
