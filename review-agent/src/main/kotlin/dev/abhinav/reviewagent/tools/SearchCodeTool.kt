package dev.abhinav.reviewagent.tools

import java.io.File
import java.util.concurrent.TimeUnit

/** Searches tracked files in the repository with `git grep`. */
class SearchCodeTool(
    private val repoRoot: File,
    private val maxMatches: Int = 50,
    private val timeoutSeconds: Long = 15,
) : Tool {

    override val spec = ToolSpec(
        name = "search_code",
        description = "Search the repository's tracked files for a regular expression (git grep). " +
            "Use it to find callers of changed functions, other implementations of an interface, " +
            "or similar code. Returns matching lines as path:line:text.",
        properties = mapOf(
            "pattern" to mapOf(
                "type" to "string",
                "description" to "Extended regular expression, e.g. \"complete\\\\(\" or \"class .*Provider\"",
            ),
            "path" to mapOf(
                "type" to "string",
                "description" to "Optional folder or file to limit the search, relative to the repo root, " +
                    "e.g. review-agent/src",
            ),
        ),
        required = listOf("pattern"),
    )

    override fun execute(call: ToolCall): ToolResult {
        val pattern = (call.input["pattern"] as? String)?.takeIf { it.isNotBlank() }
            ?: return error(call, "pattern must be a non-empty string")
        val path = call.input["path"] as? String

        val command = mutableListOf("git", "grep", "-n", "-I", "--no-color", "-E", "-e", pattern)
        if (!path.isNullOrBlank()) {
            resolveInRepo(repoRoot, path) ?: return error(call, "path must be inside the repository: $path")
            command += listOf("--", path)
        }

        val process = try {
            ProcessBuilder(command).directory(repoRoot).redirectErrorStream(true).start()
        } catch (e: Exception) {
            return error(call, "Could not run git grep: ${e.message}")
        }
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return error(call, "git grep timed out after ${timeoutSeconds}s; try a narrower pattern or path")
        }

        return when (process.exitValue()) {
            0 -> {
                val lines = output.lines().filter { it.isNotBlank() }
                val shown = lines.take(maxMatches).joinToString("\n")
                val note = if (lines.size > maxMatches) {
                    "\n\n[Showing $maxMatches of ${lines.size} matches. Narrow the pattern or path to see the rest.]"
                } else ""
                ToolResult(call.id, shown + note)
            }
            1 -> ToolResult(call.id, "No matches for: $pattern")  // git grep exits 1 when nothing matches
            else -> error(call, "git grep failed (exit ${process.exitValue()}): ${output.trim()}")
        }
    }

    private fun error(call: ToolCall, message: String) = ToolResult(call.id, message, isError = true)
}
