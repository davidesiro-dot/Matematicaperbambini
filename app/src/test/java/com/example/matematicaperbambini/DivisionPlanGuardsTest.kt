package com.example.matematicaperbambini

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DivisionPlanGuardsTest {
    @Test
    fun normalizesZeroDivisor() {
        val plan = generateDivisionPlan(100, 0)
        assertEquals(1, plan.divisor)
    }

    @Test
    fun normalizesNegativeDividend() {
        val plan = generateDivisionPlan(-1, 2)
        assertEquals(0, plan.dividend)
        assertEquals(0, plan.finalQuotient)
        assertEquals(0, plan.finalRemainder)
    }

    @Test
    fun acceptsRegularValues() {
        val plan = generateDivisionPlan(1729, 8)
        assertTrue(plan.finalQuotient > 0)
    }
}
