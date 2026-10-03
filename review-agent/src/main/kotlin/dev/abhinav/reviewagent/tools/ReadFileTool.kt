package dev.abhinav.reviewagent.tools

import dev.abhinav.reviewagent.process.CommandResult
import dev.abhinav.reviewagent.process.runCommand
import java.io.File

/**
 * Reads a range of lines from a file tracked by git, with line numbers. Untracked and
 * ignored files (local.properties, .env, keystores, .git/) are refused: the model reads
 * untrusted PR content and could be steered into quoting secrets in its review.
 */
class ReadFileTool(
    private val repoRoot: File,
    private val maxLines: Int = 300,
    private val maxFileBytes: Long = 2_000_000,
) : Tool {

    override val spec = ToolSpec(
        name = "read_file",
        description = "Read lines from a file tracked in the repository, with line numbers. " +
            "Use it to see the code around a change or a search match. Reads at most $maxLines lines per call.",
        properties = mapOf(
            "path" to mapOf(
                "type" to "string",
                "description" to "Path relative to the repo root, e.g. review-agent/src/main/kotlin/.../Main.kt",
            ),
            "start_line" to mapOf("type" to "integer", "description" to "First line to read (1-based). Default 1"),
            "end_line" to mapOf("type" to "integer", "description" to "Last line to read, inclusive"),
        ),
        required = listOf("path"),
    )

    override fun execute(call: ToolCall): ToolResult {
        val path = call.input["path"] as? String
            ?: return error(call, "path must be a string")
        val file = resolveInRepo(repoRoot, path)
            ?: return error(call, "path must be a relative path inside the repository: $path")
        if (!file.isFile) return error(call, "No such file: $path")
        if (!isTracked(path)) return error(call, "Only files tracked by git can be read: $path")
        if (file.length() > maxFileBytes) return error(call, "$path is too large to read (${file.length()} bytes)")

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return error(call, "Could not read $path: ${e.message}")
        }
        if (lines.isEmpty()) return ToolResult(call.id, "$path is empty.")

        val start = (call.intInput("start_line").getOrElse { return error(call, it.message!!) } ?: 1).coerceAtLeast(1)
        if (start > lines.size) return error(call, "start_line $start is past the end of $path (${lines.size} lines)")
        val requestedEnd = call.intInput("end_line").getOrElse { return error(call, it.message!!) } ?: lines.size
        val end = minOf(requestedEnd, lines.size, start + maxLines - 1)
        if (end < start) return error(call, "end_line must be >= start_line")

        val body = (start..end).joinToString("\n") { "$it: ${lines[it - 1]}" }
        val note = if (end < minOf(requestedEnd, lines.size)) {
            "\n\n[Stopped at line $end (limit $maxLines lines per call). ${path} has ${lines.size} lines.]"
        } else ""
        return ToolResult(call.id, "$path (lines $start-$end of ${lines.size})\n$body$note")
    }

    private fun isTracked(path: String): Boolean =
        when (val result = runCommand(listOf("git", "ls-files", "--error-unmatch", "--", path), repoRoot, timeoutSeconds = 10)) {
            is CommandResult.Finished -> result.exitCode == 0
            else -> false
        }

    private fun error(call: ToolCall, message: String) = ToolResult(call.id, message, isError = true)
}
