package dev.abhinav.reviewagent.review

enum class Severity(val label: String) {
    CRITICAL("critical"),
    MAJOR("major"),
    MINOR("minor");

    companion object {
        fun fromLabel(label: String): Severity? = entries.firstOrNull { it.label == label }
    }
}
