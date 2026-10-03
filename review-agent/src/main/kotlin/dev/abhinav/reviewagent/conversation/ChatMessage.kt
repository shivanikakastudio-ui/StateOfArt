package dev.abhinav.reviewagent.conversation

import dev.abhinav.reviewagent.tools.ToolResult

/** The conversation history the agent builds up and sends on every turn. */
sealed interface ChatMessage {
    data class User(val text: String) : ChatMessage
    data class Assistant(val turn: ModelTurn) : ChatMessage
    data class ToolResults(val results: List<ToolResult>) : ChatMessage
}
