package es.sebas1705.axiomnode.testsupport

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

/**
 * Hand-written KMP-safe fake for [GamesUseCase].
 *
 * Behaviour is controlled by mutable `*Result` fields so a test can swap in a
 * [Result.success] or a [Result.failure] per call. All invocations are recorded
 * in the `*Calls` counters for assertions.
 */
class FakeGamesUseCase : GamesUseCase {
    var catalogResult: Result<GameCatalog> = Result.success(
        GameCatalog(
            categories = listOf(
                GameCategory("math", "Matemáticas"),
                GameCategory("history", "Historia"),
            ),
            languages = listOf(
                GameLanguage("es", "Español"),
                GameLanguage("en", "English"),
            ),
        ),
    )
    var generateGameResult: Result<Game> = Result.success(sampleGame(id = "generated"))
    var randomGamesResult: Result<List<Game>> = Result.success(listOf(sampleGame("r1"), sampleGame("r2")))
    var cachedGameResult: Result<Game?> = Result.success(null)
    var playedGamesHistoryResult: Result<List<Game>> = Result.success(emptyList())
    var recordGameResultResult: Result<Unit> = Result.success(Unit)
    var recentResultsResult: Result<List<GameResult>> = Result.success(emptyList())
    var gameStatsResult: Result<GameStats> = Result.success(
        GameStats(totalGames = 0, wins = 0, losses = 0, draws = 0, averageScore = 0, totalPlayTimeSeconds = 0),
    )
    var syncPendingResult: Result<Int> = Result.success(0)
    var pendingSyncCountValue: Int = 0

    var getCatalogCalls: Int = 0
        private set
    var generateGameCalls: Int = 0
        private set
    var randomGamesCalls: Int = 0
        private set
    var cachedGameByIdCalls: Int = 0
        private set
    var playedGamesHistoryCalls: Int = 0
        private set
    var recordGameResultCalls: Int = 0
        private set
    var lastRecordedResult: GameResult? = null
        private set
    var lastGeneratedGameType: GameType? = null
        private set
    var lastGeneratedLetters: String? = null
        private set
    var recentResultsCalls: Int = 0
        private set
    var gameStatsCalls: Int = 0
        private set
    var syncPendingCalls: Int = 0
        private set
    var pendingSyncCountCalls: Int = 0
        private set

    override suspend fun getGameCatalog(): Result<GameCatalog> {
        getCatalogCalls++
        return catalogResult
    }

    override suspend fun generateGame(
        categoryId: String,
        language: String,
        numQuestions: Int,
        difficultyPercentage: Int,
        gameType: GameType,
        letters: String?,
    ): Result<Game> {
        generateGameCalls++
        lastGeneratedGameType = gameType
        lastGeneratedLetters = letters
        return generateGameResult
    }

    override suspend fun getRandomGames(
        count: Int,
        language: String,
        categoryId: String?,
    ): Result<List<Game>> {
        randomGamesCalls++
        return randomGamesResult
    }

    override suspend fun getCachedGameById(gameId: String): Result<Game?> {
        cachedGameByIdCalls++
        return cachedGameResult
    }

    override suspend fun getPlayedGamesHistory(limit: Int): Result<List<Game>> {
        playedGamesHistoryCalls++
        return playedGamesHistoryResult
    }

    override suspend fun recordGameResult(result: GameResult): Result<Unit> {
        recordGameResultCalls++
        lastRecordedResult = result
        return recordGameResultResult
    }

    override suspend fun getRecentResults(limit: Int): Result<List<GameResult>> {
        recentResultsCalls++
        return recentResultsResult
    }

    override suspend fun getGameStats(): Result<GameStats> {
        gameStatsCalls++
        return gameStatsResult
    }

    override suspend fun syncPendingResults(): Result<Int> {
        syncPendingCalls++
        return syncPendingResult
    }

    override suspend fun getPendingSyncCount(): Int {
        pendingSyncCountCalls++
        return pendingSyncCountValue
    }
}

fun sampleGame(
    id: String = "g1",
    questions: List<Question> = listOf(
        Question(id = "q1", text = "2 + 2", options = listOf("3", "4", "5"), correctAnswer = "4"),
        Question(id = "q2", text = "Capital of France", options = listOf("Madrid", "Paris", "Rome"), correctAnswer = "Paris"),
    ),
    gameType: GameType = GameType.QUIZ,
    categoryId: String = "math",
    categoryName: String = "Matemáticas",
    language: String = "es",
): Game = Game(
    id = id,
    gameType = gameType,
    categoryId = categoryId,
    categoryName = categoryName,
    language = language,
    questions = questions,
)

fun sampleGameResult(
    gameId: String = "g1",
    outcome: GameOutcome = GameOutcome.WON,
    score: Int = 8,
    durationSeconds: Int = 42,
): GameResult = GameResult(
    gameId = gameId,
    gameType = GameType.QUIZ,
    categoryId = "math",
    categoryName = "Matemáticas",
    language = "es",
    outcome = outcome,
    score = score,
    durationSeconds = durationSeconds,
)
