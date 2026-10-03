package dev.abhinav.reviewagent.agent

/** Limits for one agent run. */
data class AgentConfig(
    /** Maximum model calls in one run. */
    val maxTurns: Int = 15,
    /** Stop once input + output tokens across all turns pass this. */
    val maxTotalTokens: Long = 300_000,
    /**
     * If set, the run only completes when the model calls this tool successfully, and it
     * ends right after. A model that answers in plain text instead gets one reminder.
     */
    val finishTool: String? = null,
)
