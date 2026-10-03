package dev.abhinav.reviewagent.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiffLinesTest {
    private val diff = """
        diff --git a/app/Foo.kt b/app/Foo.kt
        index 111..222 100644
        --- a/app/Foo.kt
        +++ b/app/Foo.kt
        @@ -10,4 +10,5 @@ class Foo {
             val a = 1
        -    val b = 2
        +    val b = 3
        +    val c = 4
             val d = 5
             val e = 6
        diff --git a/app/Gone.kt b/app/Gone.kt
        deleted file mode 100644
        --- a/app/Gone.kt
        +++ /dev/null
        @@ -1,2 +0,0 @@
        -line one
        -line two
        diff --git a/app/New.kt b/app/New.kt
        new file mode 100644
        --- /dev/null
        +++ b/app/New.kt
        @@ -0,0 +1 @@
        +only line
    """.trimIndent()

    private val lines = DiffLines.parse(diff)

    @Test
    fun `tracks new-side line numbers for added and context lines`() {
        assertEquals((10..14).toSet(), (1..30).filter { lines.contains("app/Foo.kt", it) }.toSet())
    }

    @Test
    fun `ignores deleted files and handles single-line hunks`() {
        assertEquals(setOf("app/Foo.kt", "app/New.kt"), lines.files)
        assertTrue(lines.contains("app/New.kt", 1))
        assertFalse(lines.contains("app/New.kt", 2))
    }
}
