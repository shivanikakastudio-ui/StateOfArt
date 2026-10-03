package dev.abhinav.reviewagent.conversation

import dev.abhinav.reviewagent.tools.ToolCall

/** One model response. */
data class ModelTurn(
    val text: String,
    val toolCalls: List<ToolCall>,
    val stopReason: StopReason,
    /** The provider's original stop reason string, kept so OTHER can say what actually happened. */
    val rawStopReason: String?,
    val usage: Usage,
    /** The provider's summary of the model's reasoning for this turn, if it returns one. */
    val reasoning: String = "",
    /**
     * The provider's raw response, opaque to the agent. Only the provider that produced it
     * reads it back, to replay the turn exactly (e.g. Claude's thinking blocks must be sent
     * back unchanged during tool use).
     */
    val providerPayload: Any? = null,
)
