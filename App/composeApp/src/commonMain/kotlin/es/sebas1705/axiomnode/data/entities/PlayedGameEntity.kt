package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import es.sebas1705.axiomnode.domain.models.GameOutcome

@Entity(
    tableName = "played_games",
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["playedAt"]),
    ],
)
data class PlayedGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: String,
    val playedAt: Long,
    val outcome: String,
    val score: Int,
    val playerFirebaseUid: String? = null,
) {
    companion object {
        fun from(gameId: String, playedAt: Long, outcome: GameOutcome, score: Int, playerFirebaseUid: String? = null): PlayedGameEntity =
            PlayedGameEntity(
                gameId = gameId,
                playedAt = playedAt,
                outcome = outcome.name,
                score = score,
                playerFirebaseUid = playerFirebaseUid,
            )
    }
}
