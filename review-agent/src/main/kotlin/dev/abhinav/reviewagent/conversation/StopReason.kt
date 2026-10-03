package dev.abhinav.reviewagent.conversation

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
