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
                        "title" to mapOf(
                            "type" to "string",
                            "description" to "One short sentence naming the bug in plain words, " +
                                "e.g. \"Secret files can be read through a shortcut file\"",
                        ),
                        "what_goes_wrong" to mapOf(
                            "type" to "string",
                            "description" to "A concrete scenario a reader can picture: what someone does, " +
                                "and what then goes wrong. Two or three short sentences",
                        ),
                        "why_it_matters" to mapOf(
                            "type" to "string",
                            "description" to "The consequence for users, data, security or cost. One or two sentences",
                        ),
                        "how_to_fix" to mapOf(
                            "type" to "string",
                            "description" to "The change to make. Name code only where needed to act on it",
                        ),
                    ),
                    "required" to TEXT_FIELDS.keys.toList().let { listOf("file", "line", "severity") + it },
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
            val text = TEXT_FIELDS.keys.associateWith { key ->
                (item[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return error(call, "findings[$i].$key must be a non-empty string")
            }
            Finding(
                file = item["file"] as? String ?: return error(call, "findings[$i].file must be a string"),
                line = line,
                severity = severityLabel?.let(Severity::fromLabel)
                    ?: return error(call, "findings[$i].severity must be one of ${Severity.entries.map { it.label }}"),
                title = text.getValue("title"),
                whatGoesWrong = text.getValue("what_goes_wrong"),
                whyItMatters = text.getValue("why_it_matters"),
                howToFix = text.getValue("how_to_fix"),
            )
        }

        submitted = SubmittedReview(summary, findings)
        return ToolResult(call.id, "Review received with ${findings.size} finding(s).")
    }

    private fun error(call: ToolCall, message: String) = ToolResult(call.id, message, isError = true)

    companion object {
        const val NAME = "submit_review"

        /** The finding's text fields, keyed by their name in the schema. */
        private val TEXT_FIELDS = linkedMapOf(
            "title" to "Title",
            "what_goes_wrong" to "What goes wrong",
            "why_it_matters" to "Why it matters",
            "how_to_fix" to "How to fix",
        )
    }
}
