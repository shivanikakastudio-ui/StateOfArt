package dev.abhinav.reviewagent.tools

import java.io.File
import java.util.concurrent.TimeUnit

/** Fetches a pull request's unified diff with the GitHub CLI (`gh pr diff`). */
class GetPrDiffTool(
    private val repoDir: File = File("."),
    private val maxChars: Int = 100_000,
    private val timeoutSeconds: Long = 30,
) : Tool {

    override val spec = ToolSpec(
        name = "get_pr_diff",
        description = "Get the unified diff of a pull request in this repository. " +
            "Call this before commenting on a PR's changes.",
        properties = mapOf(
            "pr_number" to mapOf(
                "type" to "integer",
                "description" to "The pull request number, e.g. 9",
            ),
        ),
        required = listOf("pr_number"),
    )

    override fun execute(call: ToolCall): ToolResult {
        // The model usually follows the schema, but validate anyway.
        val prNumber = (call.input["pr_number"] as? Number)?.toInt()
            ?: return error(call, "pr_number must be an integer, got: ${call.input["pr_number"]}")
        if (prNumber <= 0) return error(call, "pr_number must be positive, got: $prNumber")

        val process = try {
            ProcessBuilder("gh", "pr", "diff", prNumber.toString())
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            return error(call, "Could not run the GitHub CLI (gh): ${e.message}")
        }

        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return error(call, "gh pr diff timed out after ${timeoutSeconds}s")
        }
        if (process.exitValue() != 0) {
            return error(call, "gh pr diff failed (exit ${process.exitValue()}): ${output.trim()}")
        }

        // Say so explicitly when the diff is cut, so the model doesn't review half a change unawares.
        val content = if (output.length <= maxChars) output else
            output.take(maxChars) + "\n\n[Diff truncated: showing the first $maxChars of ${output.length} characters.]"
        return ToolResult(toolCallId = call.id, content = content)
    }

    private fun error(call: ToolCall, message: String) =
        ToolResult(toolCallId = call.id, content = message, isError = true)
}
