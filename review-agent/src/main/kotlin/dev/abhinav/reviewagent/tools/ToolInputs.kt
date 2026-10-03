package dev.abhinav.reviewagent.tools

import java.math.BigDecimal

/**
 * Reads an optional whole-number input. Decimals (9.7) and values outside Int range are
 * rejected rather than silently converted, so a tool never acts on a different number
 * than the model sent. Returns null when the input is absent.
 */
internal fun ToolCall.intInput(name: String): Result<Int?> {
    val value = input[name] ?: return Result.success(null)
    val number = value as? Number
        ?: return Result.failure(IllegalArgumentException("$name must be a whole number, got: $value"))
    val exact = try {
        BigDecimal(number.toString()).intValueExact()
    } catch (e: ArithmeticException) {
        return Result.failure(IllegalArgumentException("$name must be a whole number within range, got: $value"))
    }
    return Result.success(exact)
}
