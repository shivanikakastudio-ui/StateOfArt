package dev.abhinav.reviewagent.review

import kotlin.test.Test
import kotlin.test.assertEquals

class FindingValidatorTest {
    private val diff = DiffLines.parse(
        """
        diff --git a/app/Foo.kt b/app/Foo.kt
        --- a/app/Foo.kt
        +++ b/app/Foo.kt
        @@ -1,2 +1,3 @@
         a
        +b
         c
        """.trimIndent()
    )
    private val validator = FindingValidator(diff)

    private fun finding(file: String = "app/Foo.kt", line: Int = 2, message: String = "bug") =
        Finding(file, line, Severity.MAJOR, message)

    @Test
    fun `accepts findings on changed lines and rejects the rest with a reason`() {
        val result = validator.validate(
            listOf(
                finding(),
                finding(file = "app/Other.kt"),
                finding(line = 40),
                finding(message = "  "),
                finding(),
            )
        )
        assertEquals(listOf(finding()), result.accepted)
        assertEquals(
            listOf(
                "file is not part of this PR's changes",
                "line 40 is outside the changed sections",
                "empty message",
                "duplicate",
            ),
            result.rejected.map { it.reason },
        )
    }
}
