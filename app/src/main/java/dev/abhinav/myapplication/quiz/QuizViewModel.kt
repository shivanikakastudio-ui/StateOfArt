package dev.abhinav.myapplication.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhinav.myapplication.quiz.data.FakeQuizApi
import dev.abhinav.myapplication.quiz.data.Question
import dev.abhinav.myapplication.quiz.data.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val questionCount: Int? = null,
    /** How many times the API was called; shown on screen while we work on the data layer. */
    val apiRequests: Int = 0,
)

class QuizViewModel(
    private val api: FakeQuizApi = FakeQuizApi(),
    private val repository: QuestionRepository = QuestionRepository(api),
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init {
        // The question list and the header load independently, as two parts of a screen would.
        viewModelScope.launch {
            val questions = repository.questions().values.toList()
            _state.update { it.copy(questions = questions, apiRequests = api.requestCount) }
        }
        viewModelScope.launch {
            val count = repository.questions().size
            _state.update { it.copy(questionCount = count, apiRequests = api.requestCount) }
        }
    }
}
