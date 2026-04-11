package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.db.GameDao
import es.sebas1705.axiomnode.data.db.GameResultDao
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.GameResultEntity
import es.sebas1705.axiomnode.data.network.GameGenerateRequest
import es.sebas1705.axiomnode.data.network.GameResultSyncEngine
import es.sebas1705.axiomnode.data.network.GamesHttpClient
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameCategory
import es.sebas1705.axiomnode.domain.models.GameLanguage
import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.models.GameStats
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.models.Question
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Implementacion del caso de uso de juegos con cache local.
 * In dev mode, generates mock quiz data locally when the backend is unavailable.
 */
class GamesRepository(
    private val httpClient: GamesHttpClient,
    private val gameDao: GameDao,
    private val gameResultDao: GameResultDao,
    private val syncEngine: GameResultSyncEngine,
    private val config: AppConfig,
) : GamesUseCase {
    // Auth token managed externally – injected when available
    private var authToken: String = ""

    fun updateAuthToken(token: String) {
        authToken = token
    }

    /** Expose sync engine state for UI observation. */
    val syncState get() = syncEngine.state

    override suspend fun getGameCatalog(): Result<GameCatalog> {
        if (config.isDevAuth) {
            return Result.success(devCatalog)
        }
        return httpClient.getGameCatalog()
    }

    override suspend fun generateGame(
        categoryId: String,
        language: String,
        numQuestions: Int,
        difficultyPercentage: Int,
    ): Result<Game> = withContext(Dispatchers.IO) {
        if (config.isDevAuth) {
            return@withContext Result.success(createDevQuizGame(categoryId, language, numQuestions))
        }
        val request = GameGenerateRequest(
            language = language,
            categoryId = categoryId,
            numQuestions = numQuestions,
            difficultyPercentage = difficultyPercentage,
        )
        httpClient.generateGame(request, authToken)
            .onSuccess { game ->
                gameDao.insertGame(GameEntity.fromDomain(game))
            }
    }

    override suspend fun getRandomGames(
        count: Int,
        language: String,
        categoryId: String?,
    ): Result<List<Game>> = withContext(Dispatchers.IO) {
        if (config.isDevAuth) {
            val games = (1..count).map { i ->
                val cat = devCategories.random()
                createDevQuizGame(cat.first, language, 5)
            }
            return@withContext Result.success(games)
        }
        httpClient.getRandomGames(count, language, categoryId)
            .onSuccess { games ->
                gameDao.insertGames(games.map { GameEntity.fromDomain(it) })
            }
    }

    override suspend fun recordGameResult(result: GameResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = GameResultEntity.fromDomain(result)
            gameResultDao.insertResult(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentResults(limit: Int): Result<List<GameResult>> = withContext(Dispatchers.IO) {
        try {
            val entities = gameResultDao.getRecentResults(limit)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameStats(): Result<GameStats> = withContext(Dispatchers.IO) {
        try {
            val all = gameResultDao.getRecentResults(limit = Int.MAX_VALUE)
            val results = all.map { it.toDomain() }
            val stats = GameStats(
                totalGames = results.size,
                wins = results.count { it.outcome == GameOutcome.WON },
                losses = results.count { it.outcome == GameOutcome.LOST },
                draws = results.count { it.outcome == GameOutcome.DRAW },
                averageScore = if (results.isNotEmpty()) results.map { it.score }.average().toInt() else 0,
                totalPlayTimeSeconds = results.sumOf { it.durationSeconds },
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingResults(): Result<Int> = withContext(Dispatchers.IO) {
        if (config.isDevAuth) {
            // In dev mode, just mark everything as synced locally
            val unsynced = gameResultDao.getUnSyncedResults()
            if (unsynced.isNotEmpty()) {
                gameResultDao.markAsSynced(unsynced.map { it.id })
            }
            return@withContext Result.success(unsynced.size)
        }
        syncEngine.syncPendingResults(authToken)
    }

    override suspend fun getPendingSyncCount(): Int = withContext(Dispatchers.IO) {
        syncEngine.getPendingCount()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dev mock data
    // ─────────────────────────────────────────────────────────────────────────

    private val devCategories = listOf(
        "ciencia" to "Ciencia",
        "historia" to "Historia",
        "geografia" to "Geografía",
        "tecnologia" to "Tecnología",
        "deportes" to "Deportes",
    )

    private val devCatalog = GameCatalog(
        categories = devCategories.map { GameCategory(it.first, it.second) },
        languages = listOf(
            GameLanguage("es", "Español"),
            GameLanguage("en", "English"),
        ),
    )

    private var devGameCounter = 0

    private fun createDevQuizGame(categoryId: String, language: String, numQuestions: Int): Game {
        devGameCounter++
        val catName = devCategories.find { it.first == categoryId }?.second ?: categoryId

        val questionPool = devQuestionBank[categoryId] ?: devQuestionBank["ciencia"]!!
        val questions = questionPool.shuffled().take(numQuestions)

        return Game(
            id = "dev-game-$devGameCounter",
            gameType = GameType.QUIZ,
            categoryId = categoryId,
            categoryName = catName,
            language = language,
            questions = questions,
        )
    }

    private val devQuestionBank: Map<String, List<Question>> = mapOf(
        "ciencia" to listOf(
            Question("q1", "¿Cuál es el planeta más grande del sistema solar?", listOf("Marte", "Júpiter", "Saturno", "Neptuno"), "Júpiter"),
            Question("q2", "¿Qué gas es más abundante en la atmósfera terrestre?", listOf("Oxígeno", "Nitrógeno", "CO₂", "Argón"), "Nitrógeno"),
            Question("q3", "¿Cuántos huesos tiene el cuerpo humano adulto?", listOf("196", "206", "216", "186"), "206"),
            Question("q4", "¿Qué elemento químico tiene el símbolo 'Au'?", listOf("Plata", "Aluminio", "Oro", "Argón"), "Oro"),
            Question("q5", "¿Cuál es la velocidad de la luz en km/s?", listOf("200.000", "300.000", "400.000", "150.000"), "300.000"),
            Question("q6", "¿Qué órgano produce la insulina?", listOf("Hígado", "Riñón", "Páncreas", "Estómago"), "Páncreas"),
            Question("q7", "¿Cuál es el elemento más ligero de la tabla periódica?", listOf("Helio", "Hidrógeno", "Litio", "Carbono"), "Hidrógeno"),
        ),
        "historia" to listOf(
            Question("h1", "¿En qué año cayó el Muro de Berlín?", listOf("1987", "1989", "1991", "1985"), "1989"),
            Question("h2", "¿Quién pintó la Mona Lisa?", listOf("Miguel Ángel", "Rafael", "Leonardo da Vinci", "Botticelli"), "Leonardo da Vinci"),
            Question("h3", "¿En qué año llegó Colón a América?", listOf("1490", "1492", "1498", "1500"), "1492"),
            Question("h4", "¿Cuál fue la primera civilización de Mesopotamia?", listOf("Babilonia", "Asiria", "Sumeria", "Persia"), "Sumeria"),
            Question("h5", "¿En qué siglo se produjo la Revolución Francesa?", listOf("XVII", "XVIII", "XIX", "XVI"), "XVIII"),
            Question("h6", "¿Quién fue el primer presidente de EE.UU.?", listOf("Lincoln", "Jefferson", "Washington", "Adams"), "Washington"),
            Question("h7", "¿Qué imperio construyó Machu Picchu?", listOf("Azteca", "Maya", "Inca", "Olmeca"), "Inca"),
        ),
        "geografia" to listOf(
            Question("g1", "¿Cuál es el río más largo del mundo?", listOf("Nilo", "Amazonas", "Misisipi", "Yangtsé"), "Amazonas"),
            Question("g2", "¿Cuál es el país más grande del mundo?", listOf("China", "EE.UU.", "Canadá", "Rusia"), "Rusia"),
            Question("g3", "¿En qué continente está Egipto?", listOf("Asia", "Europa", "África", "Oceanía"), "África"),
            Question("g4", "¿Cuál es la capital de Australia?", listOf("Sídney", "Melbourne", "Canberra", "Brisbane"), "Canberra"),
            Question("g5", "¿Cuál es el océano más grande?", listOf("Atlántico", "Índico", "Pacífico", "Ártico"), "Pacífico"),
            Question("g6", "¿Cuál es el desierto más grande del mundo?", listOf("Sahara", "Gobi", "Antártico", "Kalahari"), "Antártico"),
            Question("g7", "¿Cuántos continentes hay?", listOf("5", "6", "7", "8"), "7"),
        ),
        "tecnologia" to listOf(
            Question("t1", "¿En qué año se fundó Apple?", listOf("1974", "1976", "1978", "1980"), "1976"),
            Question("t2", "¿Qué lenguaje creó Guido van Rossum?", listOf("Java", "C++", "Python", "Ruby"), "Python"),
            Question("t3", "¿Qué significa 'HTTP'?", listOf("HyperText Transfer Protocol", "High Tech Transfer Protocol", "HyperText Transmission Process", "High Transfer Text Protocol"), "HyperText Transfer Protocol"),
            Question("t4", "¿Quién es el creador de Linux?", listOf("Bill Gates", "Steve Jobs", "Linus Torvalds", "Dennis Ritchie"), "Linus Torvalds"),
            Question("t5", "¿Qué empresa desarrolla Android?", listOf("Apple", "Microsoft", "Google", "Samsung"), "Google"),
            Question("t6", "¿Cuántos bits tiene un byte?", listOf("4", "8", "16", "32"), "8"),
            Question("t7", "¿Qué significan las siglas 'CPU'?", listOf("Central Processing Unit", "Computer Personal Unit", "Central Program Utility", "Core Processing Unit"), "Central Processing Unit"),
        ),
        "deportes" to listOf(
            Question("d1", "¿Cuántos jugadores tiene un equipo de fútbol?", listOf("9", "10", "11", "12"), "11"),
            Question("d2", "¿En qué país se celebraron los JJ.OO. de 2020?", listOf("China", "Brasil", "Japón", "Corea"), "Japón"),
            Question("d3", "¿Cuántos sets se necesitan para ganar un partido de tenis (Grand Slam masculino)?", listOf("2", "3", "4", "5"), "3"),
            Question("d4", "¿Qué deporte se practica en el Tour de Francia?", listOf("Atletismo", "Natación", "Ciclismo", "Esquí"), "Ciclismo"),
            Question("d5", "¿Cuál es el deporte más popular del mundo?", listOf("Baloncesto", "Cricket", "Fútbol", "Tenis"), "Fútbol"),
            Question("d6", "¿Cuánto dura un partido de baloncesto NBA?", listOf("40 min", "48 min", "60 min", "36 min"), "48 min"),
            Question("d7", "¿En qué deporte se usa un 'shuttlecock'?", listOf("Tenis", "Bádminton", "Squash", "Ping-pong"), "Bádminton"),
        ),
    )
}

