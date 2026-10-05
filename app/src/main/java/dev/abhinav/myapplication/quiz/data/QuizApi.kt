package dev.abhinav.myapplication.quiz.data

import kotlin.time.Duration.Companion.milliseconds

interface QuizApi {
    suspend fun fetchQuestions(): List<Question>
}

/** Stand-in for a network API until the app has a real backend. */
class FakeQuizApi : QuizApi {
    var requestCount = 0
        private set

    override suspend fun fetchQuestions(): List<Question> {
        requestCount++
        kotlinx.coroutines.delay(500.milliseconds) // simulated network latency
        return listOf(
            Question("q1", "What is 15% of 1,000,000?", listOf("15,000", "150,000", "1,500,000", "1,500"), 1),
            Question("q2", "Which planet has the most moons?", listOf("Earth", "Mars", "Saturn", "Venus"), 2),
            Question("q3", "How many bits are in a byte?", listOf("4", "8", "16", "32"), 1),
        )
    }
}
