package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.domain.models.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseSessionRequest(
    val idToken: String,
)

@Serializable
data class UserProfileResponse(
    val firebaseUid: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val role: String = "GAMER",
) {
    fun toDomain(): User = User(
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        role = es.sebas1705.axiomnode.domain.models.UserRole.valueOf(role),
    )
}

/**
 * Cliente HTTP para autenticacion con el microservicio de usuarios.
 */
class AuthHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://microservice-users:7102",
) {
    suspend fun syncSessionFromFirebase(idToken: String): Result<User> = try {
        val response = httpClient.post("$baseUrl/users/firebase/session") {
            contentType(ContentType.Application.Json)
            setBody(FirebaseSessionRequest(idToken))
        }
        val profile = response.body<UserProfileResponse>()
        Result.success(profile.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserProfile(authToken: String): Result<User> = try {
        val response = httpClient.get("$baseUrl/users/me/profile") {
            bearerAuth(authToken)
        }
        val profile = response.body<UserProfileResponse>()
        Result.success(profile.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

