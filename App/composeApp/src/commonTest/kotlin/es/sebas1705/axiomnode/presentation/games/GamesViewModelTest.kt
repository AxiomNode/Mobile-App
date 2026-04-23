package es.sebas1705.axiomnode.presentation.games

import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.testsupport.FakeGamesUseCase
import es.sebas1705.axiomnode.testsupport.MainDispatcherRule
import es.sebas1705.axiomnode.testsupport.sampleGame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GamesViewModelTest {

    private val mainRule = MainDispatcherRule()
    private lateinit var useCase: FakeGamesUseCase

    @BeforeTest
    fun setUp() {
        mainRule.install()
        useCase = FakeGamesUseCase()
    }

    @AfterTest
    fun tearDown() {
        mainRule.uninstall()
    }

    @Test
    fun `init loads catalog into state`() = runTest {
        val viewModel = GamesViewModel(useCase)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.catalog)
        assertEquals(2, state.catalog.categories.size)
        assertEquals(1, useCase.getCatalogCalls)
    }

    @Test
    fun `catalog failure surfaces error message`() = runTest {
        useCase.catalogResult = Result.failure(IllegalStateException("boom"))

        val viewModel = GamesViewModel(useCase)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("boom", state.error)
        assertNull(state.catalog)
    }

    @Test
    fun `catalog failure without message falls back to default`() = runTest {
        useCase.catalogResult = Result.failure(RuntimeException())

        val viewModel = GamesViewModel(useCase)

        assertEquals("Error al cargar catalogo", viewModel.state.value.error)
    }

    @Test
    fun `loadRandomGames replaces games list and honors selected filters`() = runTest {
        val viewModel = GamesViewModel(useCase)
        viewModel.setSelectedCategory("history")
        viewModel.setSelectedLanguage("en")

        useCase.randomGamesResult = Result.success(listOf(sampleGame("only")))
        viewModel.loadRandomGames(count = 3)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.games.size)
        assertEquals("only", state.games.first().id)
        assertEquals(1, useCase.randomGamesCalls)
        assertEquals("history", state.selectedCategoryId)
        assertEquals("en", state.selectedLanguage)
    }

    @Test
    fun `loadRandomGames failure sets error and keeps previous games empty`() = runTest {
        useCase.randomGamesResult = Result.failure(IllegalStateException("net"))
        val viewModel = GamesViewModel(useCase)

        viewModel.loadRandomGames()

        val state = viewModel.state.value
        assertEquals("net", state.error)
        assertTrue(state.games.isEmpty())
    }

    @Test
    fun `generateGame appends to existing games list`() = runTest {
        val viewModel = GamesViewModel(useCase)
        useCase.randomGamesResult = Result.success(listOf(sampleGame("a"), sampleGame("b")))
        viewModel.loadRandomGames()

        useCase.generateGameResult = Result.success(sampleGame("c"))
        viewModel.generateGame(categoryId = "math", language = "es")

        val ids = viewModel.state.value.games.map { it.id }
        assertEquals(listOf("a", "b", "c"), ids)
        assertEquals(1, useCase.generateGameCalls)
    }

    @Test
    fun `generateGame failure keeps prior games untouched`() = runTest {
        val viewModel = GamesViewModel(useCase)
        useCase.randomGamesResult = Result.success(listOf(sampleGame("a")))
        viewModel.loadRandomGames()

        useCase.generateGameResult = Result.failure(RuntimeException("quota"))
        viewModel.generateGame(categoryId = "math", language = "es")

        val state = viewModel.state.value
        assertEquals(listOf("a"), state.games.map { it.id })
        assertEquals("quota", state.error)
    }

    @Test
    fun `generateQuizGame sends quiz mode to use case`() = runTest {
        val viewModel = GamesViewModel(useCase)
        useCase.generateGameResult = Result.success(sampleGame("quiz-generated", gameType = GameType.QUIZ))

        viewModel.generateQuizGame(categoryId = "math", language = "es")

        assertEquals(GameType.QUIZ, useCase.lastGeneratedGameType)
        assertNull(useCase.lastGeneratedLetters)
        assertTrue(viewModel.state.value.games.any { it.id == "quiz-generated" })
    }

    @Test
    fun `generateWordpassGame sends wordpass mode and letters to use case`() = runTest {
        val viewModel = GamesViewModel(useCase)
        useCase.generateGameResult = Result.success(sampleGame("wordpass-generated", gameType = GameType.WORDPASS))

        viewModel.generateWordpassGame(categoryId = "history", language = "es", letters = "ABC")

        assertEquals(GameType.WORDPASS, useCase.lastGeneratedGameType)
        assertEquals("ABC", useCase.lastGeneratedLetters)
        assertTrue(viewModel.state.value.games.any { it.id == "wordpass-generated" })
    }

    @Test
    fun `loadRandomGames sets advisory when content is repetitive`() = runTest {
        val repeated = sampleGame(
            id = "rep",
            questions = List(12) {
                es.sebas1705.axiomnode.domain.models.Question(
                    id = "q$it",
                    text = "Pregunta repetida",
                    options = listOf("a", "b"),
                    correctAnswer = "a",
                )
            },
        )
        useCase.randomGamesResult = Result.success(listOf(repeated))
        val viewModel = GamesViewModel(useCase)

        viewModel.loadRandomGames(count = 1)

        assertNotNull(viewModel.state.value.contentAdvice)
    }

    @Test
    fun `resolveGameForPlay returns cached game when not in current state`() = runTest {
        val cached = sampleGame("cached-only")
        useCase.cachedGameResult = Result.success(cached)
        val viewModel = GamesViewModel(useCase)
        var resolvedId: String? = null

        viewModel.resolveGameForPlay("cached-only") { game ->
            resolvedId = game?.id
        }

        assertEquals("cached-only", resolvedId)
        assertEquals(1, useCase.cachedGameByIdCalls)
    }
}
