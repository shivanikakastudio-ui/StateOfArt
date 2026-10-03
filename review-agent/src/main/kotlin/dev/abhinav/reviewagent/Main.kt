package dev.abhinav.reviewagent

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams

// Step 1: a single request/response round trip. No tools, no loop yet.
fun main() {
    // Reads ANTHROPIC_API_KEY from the environment.
    val client = AnthropicOkHttpClient.fromEnv()

    val params = MessageCreateParams.builder()
        .model("claude-opus-5-5")
        .maxTokens(1024L)
        .addUserMessage("In one sentence: what should a good pull request review focus on?")
        .build()

    val response = client.messages().create(params)

    response.content()
        .mapNotNull { it.text().orElse(null) }
        .forEach { println(it.text()) }

    println("\n[stop_reason=${response.stopReason().orElse(null)}, " +
        "input_tokens=${response.usage().inputTokens()}, " +
        "output_tokens=${response.usage().outputTokens()}]")
}
