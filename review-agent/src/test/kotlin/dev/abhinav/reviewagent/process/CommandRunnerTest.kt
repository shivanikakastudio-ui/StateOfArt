package dev.abhinav.reviewagent.process

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommandRunnerTest {
    private val dir = File(".")

    @Test
    fun `returns output and exit code`() {
        val result = runCommand(listOf("sh", "-c", "echo hello; exit 3"), dir, timeoutSeconds = 5)
        assertIs<CommandResult.Finished>(result)
        assertEquals(3, result.exitCode)
        assertEquals("hello\n", result.output)
    }

    @Test
    fun `times out a command that keeps running`() {
        val started = System.nanoTime()
        val result = runCommand(listOf("sleep", "30"), dir, timeoutSeconds = 1)
        val seconds = (System.nanoTime() - started) / 1e9
        assertIs<CommandResult.TimedOut>(result)
        assertTrue(seconds < 5, "took ${seconds}s; the timeout did not fire")
    }

    @Test
    fun `keeps at most maxOutputChars and reports truncation`() {
        val result = runCommand(listOf("sh", "-c", "yes x | head -n 1000"), dir, timeoutSeconds = 5, maxOutputChars = 10)
        assertIs<CommandResult.Finished>(result)
        assertEquals(10, result.output.length)
        assertTrue(result.truncated)
    }
}
