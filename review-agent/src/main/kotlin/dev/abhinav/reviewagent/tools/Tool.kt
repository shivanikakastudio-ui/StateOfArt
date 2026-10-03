package dev.abhinav.reviewagent.tools

/** A tool the agent can run: what the model sees ([spec]) plus the code that runs it. */
interface Tool {
    val spec: ToolSpec

    /**
     * Runs the tool for [call]. Should not throw for bad input or failures: return a
     * [ToolResult] with isError = true instead, so the model can read the error and react.
     */
    fun execute(call: ToolCall): ToolResult
}
