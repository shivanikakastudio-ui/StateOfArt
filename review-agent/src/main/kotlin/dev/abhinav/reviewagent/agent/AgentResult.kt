package dev.abhinav.reviewagent.agent

import dev.abhinav.reviewagent.conversation.Usage

/** How a run ended. Only [COMPLETED] means [AgentResult.finalText] is a finished answer. */
enum class Outcome {
    COMPLETED,
    MAX_TURNS,
    TOKEN_BUDGET_EXCEEDED,
    /** The model's reply was cut off by maxTokens. */
    TRUNCATED,
    REFUSED,
    /** The model finished without calling the finish tool, even after a reminder. */
    NO_SUBMISSION,
    /** A stop reason the loop doesn't handle; see [AgentResult.detail]. */
    UNEXPECTED_STOP,
}

data class AgentResult(
    val outcome: Outcome,
    val finalText: String,
    val turns: Int,
    val totalUsage: Usage,
    val detail: String? = null,
    /** Every step of the run, in order. */
    val trace: List<TraceEntry> = emptyList(),
)
