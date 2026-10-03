package dev.abhinav.reviewagent.llm

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlockParam
import com.anthropic.models.messages.StopReason as AnthropicStopReason

/** Adapter between our provider-agnostic types and the Anthropic Java SDK. */
class AnthropicProvider(
    private val client: AnthropicClient = AnthropicOkHttpClient.fromEnv(),
    private val model: String = "claude-opus-5-5",
    private val maxTokens: Long = 16_000L,
) : LlmProvider {

    override fun complete(
        system: String?,
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
    ): ModelTurn {
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens)
            .apply { if (!system.isNullOrBlank()) system(system) }
            .apply { tools.forEach { addTool(it.toAnthropicTool()) } }
            .messages(messages.map { it.toAnthropicParam() })
            .build()

        return client.messages().create(params).toModelTurn()
    }

    // --- our types -> SDK types ---

    private fun ToolSpec.toAnthropicTool(): Tool = Tool.builder()
        .name(name)
        .description(description)
        .inputSchema(
            Tool.InputSchema.builder()
                .properties(
                    Tool.InputSchema.Properties.builder()
                        .apply { properties.forEach { (key, schema) -> putAdditionalProperty(key, JsonValue.from(schema)) } }
                        .build()
                )
                .required(required)
                .build()
        )
        .build()

    private fun ChatMessage.toAnthropicParam(): MessageParam = when (this) {
        is ChatMessage.User -> MessageParam.builder()
            .role(MessageParam.Role.USER)
            .content(text)
            .build()

        // Replay our own earlier response exactly, including thinking blocks.
        is ChatMessage.Assistant -> (turn.providerPayload as? Message)?.toParam()
            ?: rebuildAssistant(turn)

        is ChatMessage.ToolResults -> MessageParam.builder()
            .role(MessageParam.Role.USER)
            .contentOfBlockParams(results.map { result ->
                ContentBlockParam.ofToolResult(
                    ToolResultBlockParam.builder()
                        .toolUseId(result.toolCallId)
                        .content(result.content)
                        .isError(result.isError)
                        .build()
                )
            })
            .build()
    }

    /** Fallback for a turn that didn't come from this provider: rebuild it from text and tool calls. */
    private fun rebuildAssistant(turn: ModelTurn): MessageParam {
        val blocks = buildList {
            if (turn.text.isNotEmpty()) {
                add(ContentBlockParam.ofText(TextBlockParam.builder().text(turn.text).build()))
            }
            turn.toolCalls.forEach { call ->
                add(
                    ContentBlockParam.ofToolUse(
                        ToolUseBlockParam.builder()
                            .id(call.id)
                            .name(call.name)
                            .input(JsonValue.from(call.input))
                            .build()
                    )
                )
            }
        }
        return MessageParam.builder()
            .role(MessageParam.Role.ASSISTANT)
            .contentOfBlockParams(blocks)
            .build()
    }

    // --- SDK types -> our types ---

    private fun Message.toModelTurn(): ModelTurn {
        val text = content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("\n")

        val toolCalls = content().mapNotNull { block ->
            block.toolUse().orElse(null)?.let { toolUse ->
                @Suppress("UNCHECKED_CAST")
                ToolCall(
                    id = toolUse.id(),
                    name = toolUse.name(),
                    input = toolUse._input().convert(Map::class.java) as Map<String, Any?>? ?: emptyMap(),
                )
            }
        }

        val stopReason = when (stopReason().orElse(null)) {
            AnthropicStopReason.END_TURN -> StopReason.END_TURN
            AnthropicStopReason.TOOL_USE -> StopReason.TOOL_USE
            AnthropicStopReason.MAX_TOKENS -> StopReason.MAX_TOKENS
            AnthropicStopReason.REFUSAL -> StopReason.REFUSAL
            else -> StopReason.OTHER
        }

        return ModelTurn(
            text = text,
            toolCalls = toolCalls,
            stopReason = stopReason,
            usage = Usage(usage().inputTokens(), usage().outputTokens()),
            providerPayload = this,
        )
    }
}
