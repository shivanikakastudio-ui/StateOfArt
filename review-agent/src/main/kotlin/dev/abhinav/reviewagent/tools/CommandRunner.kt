package dev.abhinav.reviewagent.tools

import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal sealed interface CommandResult {
    data class Finished(val exitCode: Int, val output: String, val truncated: Boolean) : CommandResult
    data class TimedOut(val seconds: Long) : CommandResult
    data class FailedToStart(val message: String) : CommandResult
}

/**
 * Runs [command] in [dir] with a real timeout. Output is read on a separate thread,
 * because reading it on this one would block until the process exits and the
 * timeout would never fire. Keeps at most [maxOutputChars] and drains the rest so
 * the process can't stall on a full pipe.
 */
internal fun runCommand(
    command: List<String>,
    dir: File,
    timeoutSeconds: Long,
    maxOutputChars: Int = 1_000_000,
): CommandResult {
    val process = try {
        ProcessBuilder(command).directory(dir).redirectErrorStream(true).start()
    } catch (e: Exception) {
        return CommandResult.FailedToStart(e.message ?: e.toString())
    }

    val reader = CompletableFuture.supplyAsync {
        val kept = StringBuilder()
        var truncated = false
        val buffer = CharArray(8_192)
        process.inputStream.bufferedReader().use { input ->
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                val room = maxOutputChars - kept.length
                if (room > 0) kept.appendRange(buffer, 0, minOf(n, room))
                if (n > room) truncated = true
            }
        }
        kept.toString() to truncated
    }

    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        process.destroyForcibly()  // closes the pipe, which also ends the reader
        return CommandResult.TimedOut(timeoutSeconds)
    }
    val (output, truncated) = reader.get(5, TimeUnit.SECONDS)
    return CommandResult.Finished(process.exitValue(), output, truncated)
}
