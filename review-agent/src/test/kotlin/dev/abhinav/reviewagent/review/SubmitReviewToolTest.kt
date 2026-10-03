package dev.abhinav.reviewagent.review

import dev.abhinav.reviewagent.tools.ToolCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubmitReviewToolTest {
    private fun call(input: Map<String, Any?>) = ToolCall("t1", SubmitReviewTool.NAME, input)

    private fun finding(severity: String, whyItMatters: String = "y") = mapOf(
        "file" to "a.kt", "line" to 3, "severity" to severity, "title" to "t",
        "what_goes_wrong" to "w", "why_it_matters" to whyItMatters, "how_to_fix" to "f",
    )

    @Test
    fun `rejects a finding with an empty explanation field`() {
        val tool = SubmitReviewTool()
        val result = tool.execute(call(mapOf("summary" to "s", "findings" to listOf(finding("major", whyItMatters = " ")))))
        assertTrue(result.isError)
        assertNull(tool.submitted)
    }

    @Test
    fun `records a well-formed review`() {
        val tool = SubmitReviewTool()
        val result = tool.execute(
            call(
                mapOf(
                    "summary" to "Adds a thing.",
                    "findings" to listOf(finding("major")),
                )
            )
        )
        assertFalse(result.isError)
        assertEquals(
            SubmittedReview("Adds a thing.", listOf(Finding("a.kt", 3, Severity.MAJOR, "t", "w", "y", "f"))),
            tool.submitted,
        )
    }

    @Test
    fun `rejects an unknown severity so the model can resubmit`() {
        val tool = SubmitReviewTool()
        val result = tool.execute(
            call(
                mapOf(
                    "summary" to "s",
                    "findings" to listOf(finding("blocker")),
                )
            )
        )
        assertTrue(result.isError)
        assertNull(tool.submitted)
    }
}
