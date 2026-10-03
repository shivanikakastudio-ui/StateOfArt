package dev.abhinav.reviewagent.agent

import dev.abhinav.reviewagent.conversation.ChatMessage
import dev.abhinav.reviewagent.conversation.ModelTurn
import dev.abhinav.reviewagent.conversation.StopReason
import dev.abhinav.reviewagent.conversation.Usage
import dev.abhinav.reviewagent.llm.LlmProvider
import dev.abhinav.reviewagent.tools.Tool
import dev.abhinav.reviewagent.tools.ToolCall
import dev.abhinav.reviewagent.tools.ToolResult
import dev.abhinav.reviewagent.tools.ToolSpec
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentTest {
    /** Replies with each scripted step in turn; a step can throw instead of replying. */
    private class ScriptedProvider(private val steps: List<() -> ModelTurn>) : LlmProvider {
        private var next = 0
        override fun complete(system: String?, messages: List<ChatMessage>, tools: List<ToolSpec>) = steps[next++]()
    }

    private val echo = object : Tool {
        override val spec = ToolSpec("echo", "Echo", emptyMap())
        override fun execute(call: ToolCall) = ToolResult(call.id, "echoed")
    }

    private fun toolUseTurn() = ModelTurn(
        text = "", toolCalls = listOf(ToolCall("t1", "echo", emptyMap())),
        stopReason = StopReason.TOOL_USE, rawStopReason = "tool_use", usage = Usage(100, 10),
        reasoning = "Check with echo first.",
    )

    @Test
    fun `a failed model call ends the run with the trace so far instead of throwing`() {
        val provider = ScriptedProvider(listOf({ toolUseTurn() }, { throw IOException("connection reset") }))
        val result = Agent(provider, system = "sys", tools = listOf(echo), log = {}).run("task")

        assertEquals(Outcome.PROVIDER_ERROR, result.outcome)
        assertEquals(1, result.turns)
        assertEquals(Usage(100, 10), result.totalUsage)
        assertEquals(
            listOf("system", "user", "assistant", "tool_call", "tool_result", "error"),
            result.trace.map { it.role },
        )
        assertEquals("Check with echo first.", result.trace[2].thinking)
        assertTrue("connection reset" in result.trace.last().content)
    }
}
