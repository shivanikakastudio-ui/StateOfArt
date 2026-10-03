package dev.abhinav.reviewagent.llm

// Provider-agnostic types. The agent loop only ever sees these, never an SDK type,
// so swapping Claude for another model means writing a new LlmProvider, nothing else.

/** A tool the model may call. [properties] is a JSON Schema "properties" object. */
data class ToolSpec(
    val name: String,
    val description: String,
    val properties: Map<String, Map<String, Any>>,
    val required: List<String> = emptyList(),
)

/** The model asking us to run a tool. [id] links the call to its result. */
data class ToolCall(
    val id: String,
    val name: String,
    val input: Map<String, Any?>,
)

/** What we send back after running a tool. */
data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean = false,
)

enum class StopReason {
    /** The model finished its answer. */
    END_TURN,

    /** The model wants one or more tools run before it continues. */
    TOOL_USE,

    /** The answer was cut off by maxTokens. */
    MAX_TOKENS,

    /** The model declined the request. */
    REFUSAL,

    OTHER,
}

data class Usage(val inputTokens: Long, val outputTokens: Long)

/** One model response. */
data class ModelTurn(
    val text: String,
    val toolCalls: List<ToolCall>,
    val stopReason: StopReason,
    val usage: Usage,
    /**
     * The provider's raw response, opaque to the agent. Only the provider that produced it
     * reads it back, to replay the turn exactly (e.g. Claude's thinking blocks must be sent
     * back unchanged during tool use).
     */
    val providerPayload: Any? = null,
)

/** The conversation history the agent builds up and sends on every turn. */
sealed interface ChatMessage {
    data class User(val text: String) : ChatMessage
    data class Assistant(val turn: ModelTurn) : ChatMessage
    data class ToolResults(val results: List<ToolResult>) : ChatMessage
}
