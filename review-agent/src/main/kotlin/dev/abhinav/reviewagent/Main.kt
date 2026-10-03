package dev.abhinav.reviewagent

import dev.abhinav.reviewagent.conversation.ChatMessage
import dev.abhinav.reviewagent.llm.anthropic.AnthropicProvider
import dev.abhinav.reviewagent.llm.LlmProvider

// Step 2: same single call as Step 1, but Main only knows about LlmProvider.
fun main() {
    val provider: LlmProvider = AnthropicProvider()

    val turn = provider.complete(
        system = null,
        messages = listOf(
            ChatMessage.User("In one sentence: what should a good pull request review focus on?")
        ),
    )

    println(turn.text)
    println("\n[stop_reason=${turn.stopReason}, " +
        "input_tokens=${turn.usage.inputTokens}, " +
        "output_tokens=${turn.usage.outputTokens}]")
}
