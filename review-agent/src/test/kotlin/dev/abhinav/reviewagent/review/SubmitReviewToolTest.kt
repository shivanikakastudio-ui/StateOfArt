package dev.abhinav.reviewagent.review

import dev.abhinav.reviewagent.tools.ToolCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubmitReviewToolTest {
    private fun call(input: Map<String, Any?>) = ToolCall("t1", SubmitReviewTool.NAME, input)

    @Test
    fun `records a well-formed review`() {
        val tool = SubmitReviewTool()
        val result = tool.execute(
            call(
                mapOf(
                    "summary" to "Adds a thing.",
                    "findings" to listOf(mapOf("file" to "a.kt", "line" to 3, "severity" to "major", "message" to "m")),
                )
            )
        )
        assertFalse(result.isError)
        assertEquals(SubmittedReview("Adds a thing.", listOf(Finding("a.kt", 3, Severity.MAJOR, "m"))), tool.submitted)
    }

    @Test
    fun `rejects an unknown severity so the model can resubmit`() {
        val tool = SubmitReviewTool()
        val result = tool.execute(
            call(
                mapOf(
                    "summary" to "s",
                    "findings" to listOf(mapOf("file" to "a.kt", "line" to 3, "severity" to "blocker", "message" to "m")),
                )
            )
        )
        assertTrue(result.isError)
        assertNull(tool.submitted)
    }
}
