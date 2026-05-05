package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.db.CatalogDao
import es.sebas1705.axiomnode.data.db.GameDao
import es.sebas1705.axiomnode.data.db.GameResultDao
import es.sebas1705.axiomnode.data.db.PlayedGameDao
import es.sebas1705.axiomnode.data.entities.CatalogCategoryEntity
import es.sebas1705.axiomnode.data.entities.CatalogLanguageEntity
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.PlayedGameEntity
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
import kotlin.time.Clock
import kotlin.math.abs

/**
 * Implementacion del caso de uso de juegos con cache local.
 * In dev mode, generates mock quiz data locally when the backend is unavailable.
 */
class GamesRepository(
    private val httpClient: GamesHttpClient,
    private val gameDao: GameDao,
    private val gameResultDao: GameResultDao,
    private val playedGameDao: PlayedGameDao,
    private val catalogDao: CatalogDao,
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

    private companion object {
        const val CATALOG_SYNC_MIN_INTERVAL_MS = 15 * 60 * 1000L
    }

    override suspend fun getGameCatalog(): Result<GameCatalog> {
        if (config.isDevAuth) {
            return Result.success(devCatalog)
        }

        return withContext(Dispatchers.IO) {
            val local = readCatalogFromLocal()
            val syncState = catalogDao.getSyncState()
            val now = Clock.System.now().toEpochMilliseconds()
            val shouldRefresh = local == null || syncState == null || (now - syncState.lastSyncAt) >= CATALOG_SYNC_MIN_INTERVAL_MS

            if (!shouldRefresh && local != null) {
                return@withContext Result.success(local)
            }

            val remote = httpClient.getGameCatalog()
            if (remote.isSuccess) {
                val remoteCatalog = remote.getOrNull()
                if (remoteCatalog != null) {
                    persistCatalog(remoteCatalog, now)
                    return@withContext Result.success(remoteCatalog)
                }
            }

            if (local != null) {
                Result.success(local)
            } else {
                remote
            }
        }
    }

    override suspend fun generateGame(
        categoryId: String,
        language: String,
        numQuestions: Int,
        difficultyPercentage: Int,
        gameType: GameType,
        letters: String?,
    ): Result<Game> = withContext(Dispatchers.IO) {
        if (config.isDevAuth) {
            val game = when (gameType) {
                GameType.QUIZ -> createDevQuizGame(categoryId, language, numQuestions)
                GameType.WORDPASS -> createDevWordpassGame(categoryId, language, numQuestions)
            }
            gameDao.insertGame(GameEntity.fromDomain(game))
            return@withContext Result.success(game)
        }
        val request = GameGenerateRequest(
            language = language,
            categoryId = categoryId,
            itemCount = numQuestions,
            numQuestions = numQuestions,
            difficultyPercentage = difficultyPercentage,
            letters = letters,
            requestedBy = "api",
        )
        val remote = httpClient.generateGame(request, authToken, gameType)
            .onSuccess { game ->
                gameDao.insertGame(GameEntity.fromDomain(game))
            }

        if (remote.isSuccess) return@withContext remote

        val cached = findCachedGameForGeneration(
            categoryId = categoryId,
            language = language,
            gameType = gameType,
            numQuestions = numQuestions,
        )
        if (cached != null) {
            Result.success(cached)
        } else {
            Result.failure(
                Exception("No hay contenido local suficiente para jugar sin conexión. Conéctate para descargar nuevas partidas."),
            )
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
                if (i % 2 == 0) {
                    createDevWordpassGame(cat.first, language, 5)
                } else {
                    createDevQuizGame(cat.first, language, 5)
                }
            }
            gameDao.insertGames(games.map { GameEntity.fromDomain(it) })
            return@withContext Result.success(games)
        }

        val remote = httpClient.getRandomGames(count, language, categoryId)
        if (remote.isSuccess) {
            val remoteGames = remote.getOrNull().orEmpty()
            if (remoteGames.isNotEmpty()) {
                gameDao.insertGames(remoteGames.map { GameEntity.fromDomain(it) })
                return@withContext Result.success(remoteGames)
            }
        }

        val localGames = getOfflinePlayableGames(
            count = count,
            language = language,
            categoryId = categoryId,
        )
        if (localGames.isNotEmpty()) {
            // Keep cache tables warm even when serving offline fallback content.
            gameDao.insertGames(localGames.map { GameEntity.fromDomain(it) })
            Result.success(localGames)
        } else {
            Result.failure(
                Exception("No hay contenido local suficiente para jugar sin conexión. Conéctate para descargar nuevas partidas."),
            )
        }
    }

    override suspend fun getCachedGameById(gameId: String): Result<Game?> = withContext(Dispatchers.IO) {
        try {
            val fromCache = gameDao.getGameById(gameId)?.toDomain()
            Result.success(fromCache)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlayedGamesHistory(limit: Int): Result<List<Game>> = withContext(Dispatchers.IO) {
        try {
            val playedEntries = playedGameDao.getRecentPlayedGames(limit)
            val history = resolveGamesFromPlayedEntries(playedEntries)
                .distinctBy { it.id }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordGameResult(result: GameResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = GameResultEntity.fromDomain(result)
            gameResultDao.insertResult(entity)

            val gameExists = gameDao.getGameById(result.gameId) != null
            if (gameExists) {
                playedGameDao.insertPlayedGame(
                    PlayedGameEntity.from(
                        gameId = result.gameId,
                        playedAt = entity.timestamp,
                        outcome = result.outcome,
                        score = result.score,
                    ),
                )
            }

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

    private suspend fun getOfflinePlayableGames(
        count: Int,
        language: String,
        categoryId: String?,
    ): List<Game> {
        val offlinePool = loadOfflinePool(limit = count * 12)

        val selectedPool = selectBestOfflinePool(
            pool = offlinePool,
            language = language,
            categoryId = categoryId,
        )

        if (selectedPool.isEmpty()) {
            return emptyList()
        }

        val quizGames = selectedPool.filter { it.gameType == GameType.QUIZ }.shuffled().toMutableList()
        val wordpassGames = selectedPool.filter { it.gameType == GameType.WORDPASS }.shuffled().toMutableList()
        val mixed = mutableListOf<Game>()

        // Interleave types to avoid same-mode streaks when offline content exists for both modes.
        while (quizGames.isNotEmpty() || wordpassGames.isNotEmpty()) {
            if (quizGames.isNotEmpty()) {
                mixed.add(quizGames.removeAt(0))
            }
            if (wordpassGames.isNotEmpty()) {
                mixed.add(wordpassGames.removeAt(0))
            }
            if (mixed.size >= count) {
                break
            }
        }

        return mixed.take(count)
    }

    private suspend fun findCachedGameForGeneration(
        categoryId: String,
        language: String,
        gameType: GameType,
        numQuestions: Int,
    ): Game? {
        val pool = loadOfflinePool(limit = 300)

        val candidatePool = sequenceOf(
            pool.filter { game ->
                game.gameType == gameType &&
                    game.categoryId == categoryId &&
                    game.language == language
            },
            pool.filter { game ->
                game.gameType == gameType &&
                    game.language == language
            },
            pool.filter { game ->
                game.gameType == gameType &&
                    game.categoryId == categoryId
            },
            pool.filter { game -> game.gameType == gameType },
        ).firstOrNull { it.isNotEmpty() }
            ?: return null

        val candidate = candidatePool
            .minByOrNull { game ->
                val missingPenalty = if (game.questions.size < numQuestions) 1_000 else 0
                missingPenalty + abs(game.questions.size - numQuestions)
            }
            ?: return null

        return if (candidate.questions.size <= numQuestions) {
            candidate
        } else {
            candidate.copy(questions = candidate.questions.take(numQuestions))
        }
    }

    private suspend fun loadOfflinePool(limit: Int): List<Game> {
        val cached = gameDao.getRecentGames(limit = limit)
            .map { it.toDomain() }
        val playedEntries = playedGameDao.getRecentPlayedGames(limit = limit)
        val played = resolveGamesFromPlayedEntries(playedEntries)

        return (played + cached)
            .asSequence()
            .filter { it.questions.isNotEmpty() }
            .distinctBy { it.id }
            .toList()
    }

    private fun selectBestOfflinePool(
        pool: List<Game>,
        language: String,
        categoryId: String?,
    ): List<Game> {
        val exact = pool.filter { game ->
            game.language == language && (categoryId == null || game.categoryId == categoryId)
        }
        if (exact.isNotEmpty()) return exact

        val byLanguage = pool.filter { it.language == language }
        if (byLanguage.isNotEmpty()) return byLanguage

        if (categoryId != null) {
            val byCategory = pool.filter { it.categoryId == categoryId }
            if (byCategory.isNotEmpty()) return byCategory
        }

        return pool
    }

    private suspend fun resolveGamesFromPlayedEntries(entries: List<PlayedGameEntity>): List<Game> {
        if (entries.isEmpty()) {
            return emptyList()
        }

        val ids = entries.map { it.gameId }.distinct()
        val gamesById = gameDao.getGamesByIds(ids).associateBy { it.id }
        return entries.mapNotNull { played ->
            gamesById[played.gameId]?.toDomain()
        }
    }

    private suspend fun readCatalogFromLocal(): GameCatalog? {
        val categories = catalogDao.getCategories()
        val languages = catalogDao.getLanguages()
        if (categories.isEmpty() || languages.isEmpty()) {
            return null
        }
        return GameCatalog(
            categories = categories.map { GameCategory(it.id, it.name) },
            languages = languages.map { GameLanguage(it.code, it.name) },
        )
    }

    private suspend fun persistCatalog(catalog: GameCatalog, timestampMs: Long) {
        val categories = catalog.categories.map { CatalogCategoryEntity(id = it.id, name = it.name) }
        val languages = catalog.languages.map { CatalogLanguageEntity(code = it.code, name = it.name) }
        val hash = buildCatalogHash(catalog)
        val previousHash = catalogDao.getSyncState()?.catalogHash
        if (previousHash == hash) {
            catalogDao.upsertSyncState(
                es.sebas1705.axiomnode.data.entities.CatalogSyncStateEntity(
                    id = 1,
                    lastSyncAt = timestampMs,
                    catalogHash = hash,
                ),
            )
            return
        }
        catalogDao.replaceCatalog(
            categories = categories,
            languages = languages,
            lastSyncAt = timestampMs,
            catalogHash = hash,
        )
    }

    private fun buildCatalogHash(catalog: GameCatalog): String {
        val categoryPart = catalog.categories
            .sortedBy { it.id }
            .joinToString("|") { "${it.id}:${it.name}" }
        val languagePart = catalog.languages
            .sortedBy { it.code }
            .joinToString("|") { "${it.code}:${it.name}" }
        return "$categoryPart##$languagePart"
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

    private fun createDevWordpassGame(categoryId: String, language: String, numQuestions: Int): Game {
        devGameCounter++
        val catName = devCategories.find { it.first == categoryId }?.second ?: categoryId
        val words = devWordpassBank[categoryId] ?: devWordpassBank["ciencia"]!!

        return Game(
            id = "dev-wordpass-$devGameCounter",
            gameType = GameType.WORDPASS,
            categoryId = categoryId,
            categoryName = catName,
            language = language,
            questions = words.shuffled().take(numQuestions),
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

    private val devWordpassBank: Map<String, List<Question>> = mapOf(
        "ciencia" to listOf(
            Question("w1", "Elemento quimico esencial para respirar", emptyList(), "Oxigeno"),
            Question("w2", "Planeta conocido como el planeta rojo", emptyList(), "Marte"),
            Question("w3", "Organo que bombea la sangre", emptyList(), "Corazon"),
            Question("w4", "Unidad basica de la vida", emptyList(), "Celula"),
            Question("w5", "Proceso por el que las plantas producen alimento", emptyList(), "Fotosintesis"),
        ),
        "historia" to listOf(
            Question("wh1", "Imperio de Roma", emptyList(), "Romano"),
            Question("wh2", "Anio de llegada de Colon a America", emptyList(), "1492"),
            Question("wh3", "Muro que cayo en 1989", emptyList(), "Berlin"),
            Question("wh4", "Civilizacion de Machu Picchu", emptyList(), "Inca"),
            Question("wh5", "Movimiento de 1789 en Francia", emptyList(), "Revolucion"),
        ),
        "geografia" to listOf(
            Question("wg1", "Oceano mas grande", emptyList(), "Pacifico"),
            Question("wg2", "Pais mas extenso", emptyList(), "Rusia"),
            Question("wg3", "Desierto mas grande", emptyList(), "Antartico"),
            Question("wg4", "Rio mas largo", emptyList(), "Amazonas"),
            Question("wg5", "Capital de Australia", emptyList(), "Canberra"),
        ),
        "tecnologia" to listOf(
            Question("wt1", "Lenguaje creado por Guido van Rossum", emptyList(), "Python"),
            Question("wt2", "Siglas de unidad central de proceso", emptyList(), "CPU"),
            Question("wt3", "Empresa creadora de Android", emptyList(), "Google"),
            Question("wt4", "Sistema operativo creado por Linus", emptyList(), "Linux"),
            Question("wt5", "Protocolo principal de la web", emptyList(), "HTTP"),
        ),
        "deportes" to listOf(
            Question("wd1", "Deporte con once jugadores por equipo", emptyList(), "Futbol"),
            Question("wd2", "Deporte del Tour de Francia", emptyList(), "Ciclismo"),
            Question("wd3", "Ciudad anfitriona JJOO 2020", emptyList(), "Tokio"),
            Question("wd4", "Deporte con raqueta y volante", emptyList(), "Badminton"),
            Question("wd5", "Liga donde se juega la NBA", emptyList(), "Baloncesto"),
        ),
    )
}

