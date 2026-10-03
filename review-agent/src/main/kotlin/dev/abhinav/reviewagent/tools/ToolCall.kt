package dev.abhinav.reviewagent.tools

/** The model asking us to run a tool. [id] links the call to its result. */
data class ToolCall(
    val id: String,
    val name: String,
    val input: Map<String, Any?>,
)
