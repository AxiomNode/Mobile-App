package es.sebas1705.axiomnode.presentation.gameplay

import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.Question
import es.sebas1705.axiomnode.testsupport.FakeGamesUseCase
import es.sebas1705.axiomnode.testsupport.MainDispatcherRule
import es.sebas1705.axiomnode.testsupport.sampleGame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Uses a StandardTestDispatcher as Main so we control the timer manually.
 * Tests that start a game must either finish it (so the timer cancels) or
 * avoid calling runCurrent after startGame — otherwise the while-true timer
 * would loop indefinitely.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GamePlayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val mainRule = MainDispatcherRule(dispatcher)
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

    private val twoQuestions = listOf(
        Question("q1", "2 + 2", listOf("3", "4"), correctAnswer = "4"),
        Question("q2", "5 + 5", listOf("10", "11"), correctAnswer = "10"),
    )

    @Test
    fun `initial state is empty`() {
        val viewModel = GamePlayViewModel(useCase)

        val state = viewModel.state.value
        assertEquals(null, state.game)
        assertEquals(0, state.currentQuestionIndex)
        assertEquals(0, state.correctCount)
        assertFalse(state.isFinished)
        assertEquals(0f, state.progress)
    }

    @Test
    fun `startGame seeds state with the provided game`() {
        val viewModel = GamePlayViewModel(useCase)

        viewModel.startGame(sampleGame(questions = twoQuestions))

        val state = viewModel.state.value
        assertEquals(2, state.totalQuestions)
        assertEquals("2 + 2", state.currentQuestion?.text)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `selectAnswer increments correct count and reveals answer`() {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.selectAnswer("4")

        val state = viewModel.state.value
        assertTrue(state.isAnswerRevealed)
        assertEquals(1, state.correctCount)
        assertEquals(0, state.wrongCount)
        assertTrue(state.isSelectedCorrect)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `selectAnswer wrong increments wrong count`() {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.selectAnswer("3")

        val state = viewModel.state.value
        assertEquals(0, state.correctCount)
        assertEquals(1, state.wrongCount)
        assertFalse(state.isSelectedCorrect)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `second selectAnswer after reveal is ignored`() {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.selectAnswer("4") // correct
        viewModel.selectAnswer("3") // should be ignored

        val state = viewModel.state.value
        assertEquals(1, state.correctCount)
        assertEquals(0, state.wrongCount)
        assertEquals("4", state.selectedAnswer)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `nextQuestion before reveal is ignored`() {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.nextQuestion()

        assertEquals(0, viewModel.state.value.currentQuestionIndex)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `nextQuestion advances index and resets per-question fields`() {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.selectAnswer("4")
        viewModel.nextQuestion()

        val state = viewModel.state.value
        assertEquals(1, state.currentQuestionIndex)
        assertEquals(null, state.selectedAnswer)
        assertFalse(state.isAnswerRevealed)
        assertEquals("5 + 5", state.currentQuestion?.text)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `submitTypedAnswer trims and ignores case`() {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.updateTypedAnswer("  4 ")
        viewModel.submitTypedAnswer()

        val state = viewModel.state.value
        assertTrue(state.isAnswerRevealed)
        assertEquals(1, state.correctCount)
        assertEquals("4", state.selectedAnswer)
        finishAllAnswering(viewModel)
    }

    @Test
    fun `submitTypedAnswer ignores case accents and punctuation in wordpass`() {
        val viewModel = GamePlayViewModel(useCase)
        val wordpassQuestion = Question(
            id = "w1",
            text = "Define la palabra",
            options = emptyList(),
            correctAnswer = "dígales",
        )
        viewModel.startGame(sampleGame(questions = listOf(wordpassQuestion)))

        viewModel.updateTypedAnswer("  DIGALES!!!  ")
        viewModel.submitTypedAnswer()

        val state = viewModel.state.value
        assertTrue(state.isAnswerRevealed)
        assertEquals(1, state.correctCount)
        assertEquals(0, state.wrongCount)
    }

    @Test
    fun `answering all correctly finishes with WON outcome and records result`() = runTest {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.selectAnswer("4")
        viewModel.nextQuestion()
        viewModel.selectAnswer("10")
        viewModel.nextQuestion()

        runCurrent()

        val state = viewModel.state.value
        assertTrue(state.isFinished)
        assertEquals(2, state.correctCount)
        assertEquals(100, state.scorePercentage)
        assertEquals(1, useCase.recordGameResultCalls)
        val recorded = useCase.lastRecordedResult
        assertNotNull(recorded)
        assertEquals(GameOutcome.WON, recorded.outcome)
        assertEquals(2, recorded.score)
    }

    @Test
    fun `all wrong answers finishes with LOST outcome`() = runTest {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        viewModel.selectAnswer("3")
        viewModel.nextQuestion()
        viewModel.selectAnswer("11")
        viewModel.nextQuestion()

        runCurrent()

        assertEquals(GameOutcome.LOST, useCase.lastRecordedResult?.outcome)
    }

    @Test
    fun `timer increments elapsedSeconds as virtual time advances`() = runTest {
        val viewModel = GamePlayViewModel(useCase)
        viewModel.startGame(sampleGame(questions = twoQuestions))

        runCurrent() // let the timer coroutine hit its first delay(1000)
        advanceTimeBy(3_500)
        runCurrent()

        assertEquals(3, viewModel.state.value.elapsedSeconds)

        // Finish so the while-true timer breaks before runTest tears down.
        viewModel.selectAnswer("4")
        viewModel.nextQuestion()
        viewModel.selectAnswer("10")
        viewModel.nextQuestion()
        runCurrent()
    }

    /**
     * Drives the VM to completion (or swallows the reveal step) purely via
     * synchronous state mutations. With a StandardTestDispatcher that we never
     * advance, any coroutines launched from `viewModelScope` stay queued and
     * the test finishes cleanly when the dispatcher is discarded in teardown.
     */
    private fun finishAllAnswering(viewModel: GamePlayViewModel) {
        while (!viewModel.state.value.isFinished) {
            val current = viewModel.state.value
            if (!current.isAnswerRevealed) {
                val correct = current.currentQuestion?.correctAnswer ?: return
                viewModel.selectAnswer(correct)
            }
            viewModel.nextQuestion()
        }
    }
}
