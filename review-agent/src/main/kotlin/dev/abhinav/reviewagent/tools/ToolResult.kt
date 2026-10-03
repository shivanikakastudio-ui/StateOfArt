package dev.abhinav.reviewagent.tools

/** What we send back after running a tool. */
data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean = false,
)
