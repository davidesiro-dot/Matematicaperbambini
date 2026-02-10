package com.example.matematicaperbambini

import org.junit.Assert.assertTrue
import org.junit.Test

class DivisionPlanGuardsTest {
    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroDivisor() {
        generateDivisionPlan(100, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeDividend() {
        generateDivisionPlan(-1, 2)
    }

    @Test
    fun acceptsRegularValues() {
        val plan = generateDivisionPlan(1729, 8)
        assertTrue(plan.finalQuotient > 0)
    }
}
