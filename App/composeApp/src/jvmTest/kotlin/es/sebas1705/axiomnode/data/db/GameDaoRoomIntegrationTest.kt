package es.sebas1705.axiomnode.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.QuizGameEntity
import es.sebas1705.axiomnode.data.entities.WordpassGameEntity
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.models.Question
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GameDaoRoomIntegrationTest {

    @Test
    fun `insertGame routes each mode to its cache table and clearQuizGames keeps wordpass`() = runTest {
        val dbFile = File.createTempFile("axiomnode-room-", ".db")
        dbFile.deleteOnExit()

        val db = Room.databaseBuilder<AxiomNodeDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6)
            .build()

        try {
            val dao = db.gameDao()

            dao.insertGame(
                GameEntity.fromDomain(
                    sampleGame(
                        id = "quiz-1",
                        type = GameType.QUIZ,
                    ),
                ),
            )
            dao.insertGame(
                GameEntity.fromDomain(
                    sampleGame(
                        id = "wordpass-1",
                        type = GameType.WORDPASS,
                    ),
                ),
            )

            val stored = dao.getRecentGames(limit = 10)
            assertEquals(2, stored.size)
            assertTrue(stored.any { it.gameType == "QUIZ" })
            assertTrue(stored.any { it.gameType == "WORDPASS" })

            dao.clearQuizGames()

            val remaining = dao.getRecentGames(limit = 10)
            assertEquals(1, remaining.size)
            assertEquals("WORDPASS", remaining.single().gameType)
        } finally {
            db.close()
            dbFile.delete()
        }
    }

    @Test
    fun `getGameById returns most recent row when id exists in both cache tables`() = runTest {
        val dbFile = File.createTempFile("axiomnode-room-", ".db")
        dbFile.deleteOnExit()

        val db = Room.databaseBuilder<AxiomNodeDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6)
            .build()

        try {
            val dao = db.gameDao()
            dao.insertQuizGameRow(
                QuizGameEntity(
                    id = "shared-id",
                    categoryId = "science",
                    categoryName = "Science",
                    language = "es",
                    questionsJson = "[]",
                    createdAt = 1000L,
                ),
            )
            dao.insertWordpassGameRow(
                WordpassGameEntity(
                    id = "shared-id",
                    categoryId = "history",
                    categoryName = "History",
                    language = "es",
                    questionsJson = "[]",
                    createdAt = 5000L,
                ),
            )

            val selected = dao.getGameById("shared-id")
            assertEquals("WORDPASS", selected?.gameType)
            assertEquals(5000L, selected?.createdAt)
        } finally {
            db.close()
            dbFile.delete()
        }
    }

    private fun sampleGame(
        id: String,
        type: GameType,
    ): Game {
        val base = Game(
            id = id,
            gameType = type,
            categoryId = "science",
            categoryName = "Science",
            language = "es",
            questions = listOf(
                Question(
                    id = "q-$id-1",
                    text = "Pregunta",
                    options = listOf("A", "B"),
                    correctAnswer = "A",
                ),
            ),
        )
        return base
    }
}
