package dev.abhinav.reviewagent.tools

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadFileToolTest {
    private val repo: File = Files.createTempDirectory("read-file-tool").toFile().apply {
        git("init", "-q")
        resolve("Tracked.kt").writeText("line one\nline two\n")
        resolve("local.properties").writeText("sdk.dir=/secret\n")
        git("add", "Tracked.kt")
    }
    private val tool = ReadFileTool(repo)

    @AfterTest
    fun cleanUp() {
        repo.deleteRecursively()
    }

    private fun read(path: String) = tool.execute(ToolCall("t1", "read_file", mapOf("path" to path)))

    @Test
    fun `reads tracked files`() {
        val result = read("Tracked.kt")
        assertFalse(result.isError, result.content)
        assertTrue("2: line two" in result.content)
    }

    @Test
    fun `refuses untracked files`() {
        val result = read("local.properties")
        assertTrue(result.isError)
        assertFalse("secret" in result.content)
    }

    @Test
    fun `refuses git internals and paths outside the repo`() {
        assertTrue(read(".git/config").isError)
        assertTrue(read("../outside.txt").isError)
    }

    private fun File.git(vararg args: String) {
        val process = ProcessBuilder(listOf("git") + args).directory(this).redirectErrorStream(true).start()
        check(process.waitFor() == 0) { process.inputStream.bufferedReader().readText() }
    }
}
