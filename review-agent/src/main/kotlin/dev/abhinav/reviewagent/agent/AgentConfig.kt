package dev.abhinav.reviewagent.agent

/** Limits for one agent run. */
data class AgentConfig(
    /** Maximum model calls in one run. */
    val maxTurns: Int = 15,
    /** Stop once input + output tokens across all turns pass this. */
    val maxTotalTokens: Long = 300_000,
)
