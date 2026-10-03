package dev.abhinav.reviewagent

import dev.abhinav.reviewagent.conversation.ChatMessage
import dev.abhinav.reviewagent.conversation.ModelTurn
import dev.abhinav.reviewagent.conversation.StopReason
import dev.abhinav.reviewagent.llm.LlmProvider
import dev.abhinav.reviewagent.llm.anthropic.AnthropicProvider
import dev.abhinav.reviewagent.tools.GetPrDiffTool

// Step 3: one tool-call round trip, written out by hand. Step 4 turns this into a loop.
fun main(args: Array<String>) {
    val prNumber = args.firstOrNull()?.toIntOrNull() ?: 9

    val provider: LlmProvider = AnthropicProvider()
    val tool = GetPrDiffTool()
    val system = "You are a code reviewer. Use the tools you are given to look at the actual changes."
    val messages = mutableListOf<ChatMessage>(
        ChatMessage.User("Summarise what pull request #$prNumber changes, in 3-5 bullet points."),
    )

    // Turn 1: the model sees the tool and should ask to use it.
    val first = provider.complete(system, messages, listOf(tool.spec))
    printTurn("Turn 1", first)
    if (first.stopReason != StopReason.TOOL_USE) {
        println("Expected TOOL_USE, got ${first.stopReason} (${first.rawStopReason}). Stopping.")
        return
    }
    messages += ChatMessage.Assistant(first)

    // Our code, not the model, runs the tool. Every call gets a result with the matching id.
    val results = first.toolCalls.map { call ->
        println("→ running ${call.name}(${call.input})")
        tool.execute(call).also {
            println("← ${if (it.isError) "error" else "ok"}: ${it.content.length} chars")
        }
    }
    messages += ChatMessage.ToolResults(results)

    // Turn 2: the model reads the diff and answers.
    val second = provider.complete(system, messages, listOf(tool.spec))
    printTurn("Turn 2", second)
}

private fun printTurn(label: String, turn: ModelTurn) {
    println("\n=== $label: stop_reason=${turn.stopReason} (raw=${turn.rawStopReason}), " +
        "input_tokens=${turn.usage.inputTokens}, output_tokens=${turn.usage.outputTokens}")
    if (turn.text.isNotBlank()) println(turn.text)
    turn.toolCalls.forEach { println("tool call: ${it.name} id=${it.id} input=${it.input}") }
}
