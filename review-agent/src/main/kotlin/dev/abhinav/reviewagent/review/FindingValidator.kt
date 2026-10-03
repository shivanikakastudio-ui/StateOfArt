package dev.abhinav.reviewagent.review

data class Rejected(val finding: Finding, val reason: String)

data class ValidationResult(val accepted: List<Finding>, val rejected: List<Rejected>)

/**
 * Checks each finding against the actual diff before anything is posted. A schema-valid
 * finding can still point at a file the PR didn't touch or a line GitHub can't comment on.
 */
class FindingValidator(private val diff: DiffLines) {

    fun validate(findings: List<Finding>): ValidationResult {
        val accepted = mutableListOf<Finding>()
        val rejected = mutableListOf<Rejected>()
        val seen = mutableSetOf<Triple<String, Int, String>>()

        for (finding in findings) {
            val reason = when {
                finding.message.isBlank() -> "empty message"
                finding.file !in diff.files -> "file is not part of this PR's changes"
                !diff.contains(finding.file, finding.line) -> "line ${finding.line} is outside the changed sections"
                !seen.add(Triple(finding.file, finding.line, finding.message.trim())) -> "duplicate"
                else -> null
            }
            if (reason == null) accepted += finding else rejected += Rejected(finding, reason)
        }
        return ValidationResult(accepted, rejected)
    }
}
