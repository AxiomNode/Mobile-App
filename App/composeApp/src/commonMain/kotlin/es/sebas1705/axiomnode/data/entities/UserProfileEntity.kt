package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.models.UserRole
import kotlin.time.Clock

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val firebaseUid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val role: String,
    val lastUpdatedAt: Long,
) {
    fun toDomain(): User = User(
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        role = try {
            UserRole.valueOf(role)
        } catch (_: Exception) {
            UserRole.GAMER
        },
    )

    companion object {
        fun fromDomain(user: User): UserProfileEntity =
            UserProfileEntity(
                firebaseUid = user.firebaseUid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                role = user.role.name,
                lastUpdatedAt = Clock.System.now().toEpochMilliseconds(),
            )
    }
}
