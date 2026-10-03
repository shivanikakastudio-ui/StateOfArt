package dev.abhinav.reviewagent.llm

/** What we send back after running a tool. */
data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean = false,
)
