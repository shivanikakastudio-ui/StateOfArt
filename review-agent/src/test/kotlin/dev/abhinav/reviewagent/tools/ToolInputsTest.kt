package dev.abhinav.reviewagent.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolInputsTest {
    private fun call(value: Any?) = ToolCall(id = "t1", name = "test", input = mapOf("n" to value))

    @Test
    fun `accepts whole numbers of any numeric type`() {
        assertEquals(9, call(9).intInput("n").getOrThrow())
        assertEquals(9, call(9L).intInput("n").getOrThrow())
        assertEquals(9, call(9.0).intInput("n").getOrThrow())
    }

    @Test
    fun `returns null when absent`() {
        assertNull(ToolCall("t1", "test", emptyMap()).intInput("n").getOrThrow())
    }

    @Test
    fun `rejects decimals instead of truncating them`() {
        assertTrue(call(9.7).intInput("n").isFailure)
    }

    @Test
    fun `rejects values outside Int range instead of wrapping`() {
        assertTrue(call(4_294_967_305L).intInput("n").isFailure)
    }

    @Test
    fun `rejects non-numbers`() {
        assertTrue(call("nine").intInput("n").isFailure)
    }
}
