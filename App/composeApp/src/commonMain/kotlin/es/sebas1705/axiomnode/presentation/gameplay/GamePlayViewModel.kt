package es.sebas1705.axiomnode.presentation.gameplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.models.Question
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

data class GamePlayState(
    val game: Game? = null,
    val currentQuestionIndex: Int = 0,
    val selectedAnswer: String? = null,
    val typedAnswer: String = "",
    val isAnswerRevealed: Boolean = false,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val elapsedSeconds: Int = 0,
    val isFinished: Boolean = false,
    val resultSaved: Boolean = false,
) {
    val currentQuestion: Question?
        get() = game?.questions?.getOrNull(currentQuestionIndex)

    val totalQuestions: Int
        get() = game?.questions?.size ?: 0

    val progress: Float
        get() = if (totalQuestions > 0) (currentQuestionIndex + 1).toFloat() / totalQuestions else 0f

    val isLastQuestion: Boolean
        get() = currentQuestionIndex >= totalQuestions - 1

    val isSelectedCorrect: Boolean
        get() = selectedAnswer == currentQuestion?.correctAnswer

    val scorePercentage: Int
        get() = if (totalQuestions > 0) (correctCount * 100) / totalQuestions else 0
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class GamePlayViewModel(
    private val gamesUseCase: GamesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(GamePlayState())
    val state: StateFlow<GamePlayState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun startGame(game: Game) {
        _state.value = GamePlayState(game = game)
        startTimer()
    }

    fun selectAnswer(answer: String) {
        val current = _state.value
        if (current.isAnswerRevealed || current.isFinished) return

        val isCorrect = answer == current.currentQuestion?.correctAnswer
        _state.value = current.copy(
            selectedAnswer = answer,
            isAnswerRevealed = true,
            correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
            wrongCount = if (!isCorrect) current.wrongCount + 1 else current.wrongCount,
        )
    }

    fun updateTypedAnswer(text: String) {
        val current = _state.value
        if (current.isAnswerRevealed || current.isFinished) return
        _state.value = current.copy(typedAnswer = text)
    }

    fun submitTypedAnswer() {
        val current = _state.value
        if (current.isAnswerRevealed || current.isFinished) return
        val correctAnswer = current.currentQuestion?.correctAnswer ?: return
        val isCorrect = current.typedAnswer.trim().equals(correctAnswer.trim(), ignoreCase = true)
        _state.value = current.copy(
            selectedAnswer = current.typedAnswer.trim(),
            isAnswerRevealed = true,
            correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
            wrongCount = if (!isCorrect) current.wrongCount + 1 else current.wrongCount,
        )
    }

    fun nextQuestion() {
        val current = _state.value
        if (!current.isAnswerRevealed) return

        if (current.isLastQuestion) {
            finishGame()
        } else {
            _state.value = current.copy(
                currentQuestionIndex = current.currentQuestionIndex + 1,
                selectedAnswer = null,
                typedAnswer = "",
                isAnswerRevealed = false,
            )
        }
    }

    private fun finishGame() {
        timerJob?.cancel()
        val current = _state.value
        _state.value = current.copy(isFinished = true)
        saveResult()
    }

    private fun saveResult() {
        val current = _state.value
        val game = current.game ?: return

        viewModelScope.launch {
            val outcome = when {
                current.scorePercentage >= 70 -> GameOutcome.WON
                current.scorePercentage >= 40 -> GameOutcome.DRAW
                else -> GameOutcome.LOST
            }
            val result = GameResult(
                gameId = game.id,
                gameType = game.gameType,
                categoryId = game.categoryId,
                categoryName = game.categoryName,
                language = game.language,
                outcome = outcome,
                score = current.correctCount,
                durationSeconds = current.elapsedSeconds,
            )
            gamesUseCase.recordGameResult(result)
                .onSuccess {
                    _state.value = _state.value.copy(resultSaved = true)
                }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.isFinished) break
                _state.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

