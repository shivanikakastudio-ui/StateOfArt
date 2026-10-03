package dev.abhinav.reviewagent.review

data class SubmittedReview(
    val summary: String,
    val findings: List<Finding>,
)
