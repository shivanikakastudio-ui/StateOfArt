package dev.abhinav.reviewagent.tools

/** A tool the model may call. [properties] is a JSON Schema "properties" object. */
data class ToolSpec(
    val name: String,
    val description: String,
    val properties: Map<String, Map<String, Any>>,
    val required: List<String> = emptyList(),
)
