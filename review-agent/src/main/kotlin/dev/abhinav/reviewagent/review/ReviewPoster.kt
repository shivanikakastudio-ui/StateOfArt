package dev.abhinav.reviewagent.review

import com.fasterxml.jackson.databind.ObjectMapper
import dev.abhinav.reviewagent.tools.CommandResult
import dev.abhinav.reviewagent.tools.runCommand
import java.io.File

/**
 * Posts accepted findings as one GitHub review with inline comments, via `gh api`.
 * Only our code calls this, after validation; the model never can.
 */
class ReviewPoster(private val repoRoot: File, private val mapper: ObjectMapper = ObjectMapper()) {

    fun post(prNumber: Int, summary: String, findings: List<Finding>): Result<Unit> {
        val body = mapOf(
            "event" to "COMMENT",
            "body" to "**AI review** (${findings.size} finding(s))\n\n$summary",
            "comments" to findings.map {
                mapOf(
                    "path" to it.file,
                    "line" to it.line,
                    "side" to "RIGHT",
                    "body" to "**${it.severity.label}**: ${it.message}",
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
