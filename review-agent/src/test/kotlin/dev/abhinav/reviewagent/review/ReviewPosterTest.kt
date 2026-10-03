package dev.abhinav.reviewagent.review

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewPosterTest {
    @Test
    fun `lays out a comment as title, what, why and fix`() {
        val body = ReviewPoster(File(".")).commentBody(
            Finding(
                file = "a.kt", line = 1, severity = Severity.CRITICAL,
                title = "Secret files can be read through a shortcut file",
                whatGoesWrong = "A PR adds a shortcut that points at a secret file.",
                whyItMatters = "The secret can end up in a public comment.",
                howToFix = "Refuse shortcuts.",
            )
        )
        assertEquals(
            """
            **Critical: Secret files can be read through a shortcut file**

            **What goes wrong:** A PR adds a shortcut that points at a secret file.

            **Why it matters:** The secret can end up in a public comment.

            **How to fix:** Refuse shortcuts.
            """.trimIndent(),
            body,
        )
    }
}
