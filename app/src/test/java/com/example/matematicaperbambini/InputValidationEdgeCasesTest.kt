package com.example.matematicaperbambini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InputValidationEdgeCasesTest {
    @Test
    fun returnsNonNumericFailureForTextInput() {
        val result = validateUserInput(
            stepId = "edge-non-numeric",
            value = "12a",
            expectedRange = 0..10
        )

        assertFalse(result.isValid)
        assertEquals(ValidationFailure.NON_NUMERIC, result.failure)
    }

    @Test
    fun returnsTooFastFailureWhenInputIsSubmittedTooQuickly() {
        val guard = StepInputGuard(minIntervalMs = 1_000)

        validateUserInput(
            stepId = "edge-too-fast",
            value = "4",
            expectedRange = 0..10,
            guard = guard
        )
        val second = validateUserInput(
            stepId = "edge-too-fast",
            value = "5",
            expectedRange = 0..10,
            guard = guard
        )

        assertFalse(second.isValid)
        assertEquals(ValidationFailure.TOO_FAST, second.failure)
    }

    @Test
    fun returnsOutOfRangeFailureForValueOutsideRange() {
        val result = validateUserInput(
            stepId = "edge-range",
            value = "101",
            expectedRange = 0..100
        )

        assertFalse(result.isValid)
        assertEquals(ValidationFailure.OUT_OF_RANGE, result.failure)
    }
}
