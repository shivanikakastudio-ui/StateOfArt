package dev.abhinav.reviewagent.tools

import java.io.File

/** Reads a range of lines from a file in the repository, with line numbers. */
class ReadFileTool(
    private val repoRoot: File,
    private val maxLines: Int = 300,
) : Tool {

    override val spec = ToolSpec(
        name = "read_file",
        description = "Read lines from a file in the repository, with line numbers. " +
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

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            return error(call, "Could not read $path: ${e.message}")
        }
        if (lines.isEmpty()) return ToolResult(call.id, "$path is empty.")

        val start = ((call.input["start_line"] as? Number)?.toInt() ?: 1).coerceAtLeast(1)
        if (start > lines.size) return error(call, "start_line $start is past the end of $path (${lines.size} lines)")
        val requestedEnd = (call.input["end_line"] as? Number)?.toInt() ?: lines.size
        val end = minOf(requestedEnd, lines.size, start + maxLines - 1)
        if (end < start) return error(call, "end_line must be >= start_line")

        val body = (start..end).joinToString("\n") { "$it: ${lines[it - 1]}" }
        val note = if (end < minOf(requestedEnd, lines.size)) {
            "\n\n[Stopped at line $end (limit $maxLines lines per call). ${path} has ${lines.size} lines.]"
        } else ""
        return ToolResult(call.id, "$path (lines $start-$end of ${lines.size})\n$body$note")
    }

    private fun error(call: ToolCall, message: String) = ToolResult(call.id, message, isError = true)
}
