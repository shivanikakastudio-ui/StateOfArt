package dev.abhinav.reviewagent.agent

import dev.abhinav.reviewagent.conversation.ChatMessage
import dev.abhinav.reviewagent.conversation.ModelTurn
import dev.abhinav.reviewagent.conversation.StopReason
import dev.abhinav.reviewagent.conversation.Usage
import dev.abhinav.reviewagent.llm.LlmProvider
import dev.abhinav.reviewagent.tools.Tool
import dev.abhinav.reviewagent.tools.ToolCall
import dev.abhinav.reviewagent.tools.ToolResult

/**
 * The agent loop: call the model, run any tools it asks for, send the results back,
 * and repeat until it finishes or a limit is hit.
 */
class Agent(
    private val provider: LlmProvider,
    private val system: String,
    tools: List<Tool>,
    private val config: AgentConfig = AgentConfig(),
    private val log: (String) -> Unit = ::println,
) {
    private val toolsByName = tools.associateBy { it.spec.name }
    private val toolSpecs = tools.map { it.spec }
    private val finishToolName = config.finishTool?.spec?.name

    init {
        require(config.finishTool == null || config.finishTool in tools) {
            "finishTool ${finishToolName} must be one of the agent's tools"
        }
    }

    fun run(task: String): AgentResult {
        val messages = mutableListOf<ChatMessage>(ChatMessage.User(task))
        val trace = mutableListOf(
            TraceEntry("system", system, turn = 0),
            TraceEntry("user", task, turn = 0),
        )
        var totalInput = 0L
        var totalOutput = 0L
        var remindersSent = 0

        for (turnNumber in 1..config.maxTurns) {
            val started = System.nanoTime()
            val turn = provider.complete(system, messages, toolSpecs)
            trace += TraceEntry(
                role = "assistant", content = turn.text, turn = turnNumber,
                thinking = turn.reasoning.ifBlank { null },
                stopReason = turn.rawStopReason ?: turn.stopReason.name,
                inputTokens = turn.usage.inputTokens, outputTokens = turn.usage.outputTokens,
                durationMs = (System.nanoTime() - started) / 1_000_000,
            )
            messages += ChatMessage.Assistant(turn)
            totalInput += turn.usage.inputTokens
            totalOutput += turn.usage.outputTokens
            val total = Usage(totalInput, totalOutput)
            logTurn(turnNumber, turn, total)

            fun result(outcome: Outcome, detail: String? = null) =
                AgentResult(outcome, turn.text, turnNumber, total, detail, trace.toList())

            when (turn.stopReason) {
                StopReason.END_TURN -> {
                    val finishTool = finishToolName ?: return result(Outcome.COMPLETED)
                    if (remindersSent >= 1) return result(Outcome.NO_SUBMISSION, "never called $finishTool")
                    remindersSent++
                    log("   ! answered without $finishTool; sending one reminder")
                    val reminder = "Finish by calling $finishTool. Do not answer in plain text."
                    messages += ChatMessage.User(reminder)
                    trace += TraceEntry("user", reminder, turn = turnNumber)
                }
                StopReason.MAX_TOKENS -> return result(Outcome.TRUNCATED, "reply hit maxTokens")
                StopReason.REFUSAL -> return result(Outcome.REFUSED)
                StopReason.OTHER -> return result(Outcome.UNEXPECTED_STOP, "stop_reason=${turn.rawStopReason}")
                StopReason.TOOL_USE -> {
                    // Every tool call must get a result with the matching id, in one message.
                    val results = turn.toolCalls.map(::runTool)
                    turn.toolCalls.zip(results).forEach { (call, res) ->
                        trace += TraceEntry("tool_call", "", turnNumber, name = call.name, input = call.input)
                        trace += TraceEntry("tool_result", res.content, turnNumber, name = call.name, isError = res.isError)
                    }
                    messages += ChatMessage.ToolResults(results)
                    val finished = turn.toolCalls.zip(results).any { (call, res) ->
                        call.name == finishToolName && !res.isError
                    }
                    if (finished) return result(Outcome.COMPLETED)
                }
            }

            if (totalInput + totalOutput > config.maxTotalTokens) {
                return result(
                    Outcome.TOKEN_BUDGET_EXCEEDED,
                    "used ${totalInput + totalOutput} tokens, budget ${config.maxTotalTokens}",
                )
            }
        }

        return AgentResult(
            Outcome.MAX_TURNS, finalText = "", turns = config.maxTurns,
            totalUsage = Usage(totalInput, totalOutput), detail = "no answer after ${config.maxTurns} turns",
            trace = trace.toList(),
        )
    }

    private fun runTool(call: ToolCall): ToolResult {
        val tool = toolsByName[call.name]
            ?: return ToolResult(call.id, "Unknown tool: ${call.name}", isError = true)
        val result = try {
            tool.execute(call)
        } catch (e: Exception) {
            // Tools shouldn't throw, but a bug in one must not end the whole run.
            ToolResult(call.id, "Tool ${call.name} failed: ${e.message}", isError = true)
        }
        log("   → ${call.name}(${call.input}) ← ${if (result.isError) "ERROR" else "ok"}, ${result.content.length} chars")
        return result
    }

    private fun logTurn(n: Int, turn: ModelTurn, total: Usage) {
        log(
            "Turn $n: ${turn.stopReason}, in=${turn.usage.inputTokens} out=${turn.usage.outputTokens} " +
                "(run total in=${total.inputTokens} out=${total.outputTokens})"
        )
    }
}
