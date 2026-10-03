package dev.abhinav.reviewagent.review

import com.fasterxml.jackson.databind.ObjectMapper
import dev.abhinav.reviewagent.process.CommandResult
import dev.abhinav.reviewagent.process.runCommand
import java.io.File

/**
 * Posts accepted findings as one GitHub review with inline comments, via `gh api`.
 * Only our code calls this, after validation; the model never can.
 */
class ReviewPoster(private val repoRoot: File, private val mapper: ObjectMapper = ObjectMapper()) {

    /**
     * The comment as a reader sees it: the bug in one line, then what, why, and the fix.
     * Only the model's text is trimmed, field by field. Trimming the assembled string
     * (e.g. trimMargin) would also strip leading "|" from the model's own lines, such as
     * markdown table rows or code.
     */
    internal fun commentBody(f: Finding): String = listOf(
        "**${f.severity.label.replaceFirstChar { it.uppercase() }}: ${f.title.trim()}**",
        "**What goes wrong:** ${f.whatGoesWrong.trim()}",
        "**Why it matters:** ${f.whyItMatters.trim()}",
        "**How to fix:** ${f.howToFix.trim()}",
    ).joinToString("\n\n")

    fun post(prNumber: Int, summary: String, findings: List<Finding>): Result<Unit> {
        val body = mapOf(
            "event" to "COMMENT",
            "body" to "**AI review** (${findings.size} finding(s))\n\n$summary",
            "comments" to findings.map {
                mapOf(
                    "path" to it.file,
                    "line" to it.line,
                    "side" to "RIGHT",
                    "body" to commentBody(it),
                )
            },
        )
        val payload = File.createTempFile("review-", ".json").apply {
            deleteOnExit()
            writeText(mapper.writeValueAsString(body))
        }
        val command = listOf(
            "gh", "api", "--method", "POST",
            "repos/{owner}/{repo}/pulls/$prNumber/reviews", "--input", payload.path,
        )
        return when (val result = runCommand(command, repoRoot, timeoutSeconds = 60)) {
            is CommandResult.FailedToStart -> Result.failure(IllegalStateException("Could not run gh: ${result.message}"))
            is CommandResult.TimedOut -> Result.failure(IllegalStateException("gh api timed out after ${result.seconds}s"))
            is CommandResult.Finished ->
                if (result.exitCode == 0) Result.success(Unit)
                else Result.failure(IllegalStateException("gh api failed (exit ${result.exitCode}): ${result.output.trim()}"))
        }
    }
}
