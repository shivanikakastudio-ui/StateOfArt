package dev.abhinav.reviewagent.agent

/**
 * One step of a run, in the order it happened: what the model was told, what it
 * thought and said, which tools it called with what input, and what came back.
 * Recorded so a run can be explained afterwards, e.g. why a finding wasn't raised.
 */
data class TraceEntry(
    /** system, user, assistant, tool_call or tool_result. */
    val role: String,
    val content: String,
    /** Model turn this entry belongs to; 0 for the setup before the first call. */
    val turn: Int,
    /** Tool name, for tool_call and tool_result. */
    val name: String? = null,
    /** Tool input, for tool_call. */
    val input: Map<String, Any?>? = null,
    /** The model's reasoning summary, on the assistant entry of each turn. */
    val thinking: String? = null,
    val isError: Boolean? = null,
    /** On the assistant entry: stop reason, tokens and duration of that model call. */
    val stopReason: String? = null,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val durationMs: Long? = null,
)
