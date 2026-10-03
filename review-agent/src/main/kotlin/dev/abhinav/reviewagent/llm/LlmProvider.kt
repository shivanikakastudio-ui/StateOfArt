package dev.abhinav.reviewagent.llm

import dev.abhinav.reviewagent.conversation.ChatMessage
import dev.abhinav.reviewagent.conversation.ModelTurn
import dev.abhinav.reviewagent.tools.ToolSpec

// The agent loop only ever sees our own types (conversation/, tools/), never an SDK type,
// so swapping Claude for another model means writing a new LlmProvider, nothing else.

/** Anything that can take a conversation plus tools and return the model's next turn. */
interface LlmProvider {
    fun complete(
        system: String?,
        messages: List<ChatMessage>,
        tools: List<ToolSpec> = emptyList(),
    ): ModelTurn
}
