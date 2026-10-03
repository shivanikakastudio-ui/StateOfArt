package dev.abhinav.reviewagent.llm

// The agent loop only ever sees the types in this package, never an SDK type,
// so swapping Claude for another model means writing a new LlmProvider, nothing else.

/** Anything that can take a conversation plus tools and return the model's next turn. */
interface LlmProvider {
    fun complete(
        system: String?,
        messages: List<ChatMessage>,
        tools: List<ToolSpec> = emptyList(),
    ): ModelTurn
}
