package dev.abhinav.reviewagent.llm

/** Anything that can take a conversation plus tools and return the model's next turn. */
interface LlmProvider {
    fun complete(
        system: String?,
        messages: List<ChatMessage>,
        tools: List<ToolSpec> = emptyList(),
    ): ModelTurn
}
