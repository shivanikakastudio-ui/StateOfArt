package dev.abhinav.reviewagent.review

/** One problem the model reports, anchored to a line on the new side of the diff. */
data class Finding(
    val file: String,
    val line: Int,
    val severity: Severity,
    val message: String,
)
