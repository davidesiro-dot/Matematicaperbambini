package com.example.matematicaperbambini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationTest {
    @Test
    fun rejectsNonNumericInput() {
        val result = validateUserInput(
            stepId = "s1",
            value = "abc",
            expectedRange = 0..9,
            gameState = GameState.AWAITING_INPUT
        )

        assertFalse(result.isValid)
        assertEquals(ValidationFailure.NON_NUMERIC, result.failure)
    }

    @Test
    fun rejectsOutOfRangeInput() {
        val result = validateUserInput(
            stepId = "s2",
            value = "12",
            expectedRange = 0..9,
            gameState = GameState.AWAITING_INPUT
        )

        assertFalse(result.isValid)
        assertEquals(ValidationFailure.OUT_OF_RANGE, result.failure)
    }

    @Test
    fun blocksTooFastInputWhenGuardEnabled() {
        val guard = StepInputGuard(minIntervalMs = 1_000)

        val first = validateUserInput(
            stepId = "s3",
            value = "5",
            expectedRange = 0..9,
            guard = guard
        )
        val second = validateUserInput(
            stepId = "s3",
            value = "6",
            expectedRange = 0..9,
            guard = guard
        )

        assertTrue(first.isValid)
        assertFalse(second.isValid)
        assertEquals(ValidationFailure.TOO_FAST, second.failure)
    }
}
