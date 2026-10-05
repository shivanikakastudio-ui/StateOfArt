package dev.abhinav.myapplication.quiz.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuestionRepository(private val api: QuizApi) {
    private var cache: Map<String, Question>? = null
    private val lock = Mutex()

    suspend fun questions(): Map<String, Question> {
        cache?.let { return it }
        return lock.withLock {
            // Check again under the lock: another caller may have filled the cache while we waited.
            cache ?: api.fetchQuestions().associateBy { it.id }.also { cache = it }
        }
    }
}
