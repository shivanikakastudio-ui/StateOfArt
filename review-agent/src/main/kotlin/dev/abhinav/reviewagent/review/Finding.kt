package dev.abhinav.reviewagent.review

/**
 * One problem the model reports, anchored to a line on the new side of the diff.
 * The text fields are split so every comment explains the bug the same way:
 * what goes wrong, why it matters, how to fix it.
 */
data class Finding(
    val file: String,
    val line: Int,
    val severity: Severity,
    /** One short sentence naming the bug in plain words. */
    val title: String,
    /** A concrete scenario: what someone does and what then goes wrong. */
    val whatGoesWrong: String,
    /** The consequence for users, data, security or cost. */
    val whyItMatters: String,
    /** The change to make, concretely. */
    val howToFix: String,
)
