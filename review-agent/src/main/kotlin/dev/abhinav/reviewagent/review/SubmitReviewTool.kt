package dev.abhinav.reviewagent.review

import dev.abhinav.reviewagent.tools.Tool
import dev.abhinav.reviewagent.tools.ToolCall
import dev.abhinav.reviewagent.tools.ToolResult
import dev.abhinav.reviewagent.tools.ToolSpec
import dev.abhinav.reviewagent.tools.intInput

/**
 * How the model hands its findings back. It only records them: our code decides
 * what, if anything, gets posted. Never posts to GitHub itself.
 */
class SubmitReviewTool : Tool {

    /** Set once the model submits a well-formed review. */
    var submitted: SubmittedReview? = null
        private set

    override val spec = ToolSpec(
        name = NAME,
        description = "Submit your finished review. Call this exactly once, at the end. " +
            "Use an empty findings list if nothing is worth raising.",
        properties = mapOf(
            "summary" to mapOf(
                "type" to "string",
                "description" to "Two or three sentences on what the PR does and your overall assessment",
            ),
            "findings" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "file" to mapOf("type" to "string", "description" to "Path from the repo root, as in the diff"),
                        "line" to mapOf("type" to "integer", "description" to "Line number in the new version of the file"),
                        "severity" to mapOf("type" to "string", "enum" to Severity.entries.map { it.label }),
                        "message" to mapOf("type" to "string", "description" to "The problem, why it's wrong, and a fix"),
                    ),
                    "required" to listOf("file", "line", "severity", "message"),
                    "additionalProperties" to false,
                ),
            ),
        ),
        required = listOf("summary", "findings"),
        strict = true,
    )

    override fun execute(call: ToolCall): ToolResult {
        // strict = true should guarantee this shape; check anyway and let the model resubmit.
        val summary = call.input["summary"] as? String
            ?: return error(call, "summary must be a string")
        val rawFindings = call.input["findings"] as? List<*>
            ?: return error(call, "findings must be an array")

        val findings = rawFindings.mapIndexed { i, raw ->
            val item = raw as? Map<*, *> ?: return error(call, "findings[$i] must be an object")
            @Suppress("UNCHECKED_CAST")
            val fields = ToolCall(call.id, call.name, item as Map<String, Any?>)
            val line = fields.intInput("line").getOrElse { return error(call, "findings[$i].${it.message}") }
                ?: return error(call, "findings[$i].line is required")
            val severityLabel = item["severity"] as? String
            Finding(
                file = item["file"] as? String ?: return error(call, "findings[$i].file must be a string"),
                line = line,
                severity = severityLabel?.let(Severity::fromLabel)
                    ?: return error(call, "findings[$i].severity must be one of ${Severity.entries.map { it.label }}"),
                message = item["message"] as? String ?: return error(call, "findings[$i].message must be a string"),
            )
        }

        submitted = SubmittedReview(summary, findings)
        return ToolResult(call.id, "Review received with ${findings.size} finding(s).")
    }

    private fun error(call: ToolCall, message: String) = ToolResult(call.id, message, isError = true)

    companion object {
        const val NAME = "submit_review"
    }
}
