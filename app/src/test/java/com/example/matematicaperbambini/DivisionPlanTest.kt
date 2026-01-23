package com.example.matematicaperbambini

import org.junit.Assert.assertEquals
import org.junit.Test

class DivisionPlanTest {
    @Test
    fun planFor1729Div8() {
        val plan = generateDivisionPlan(1729, 8)
        assertEquals(216, plan.finalQuotient)
        assertEquals(1, plan.finalRemainder)
    }

    @Test
    fun planWithZeroInQuotient() {
        val plan = generateDivisionPlan(1629, 8)
        assertEquals(203, plan.finalQuotient)
        assertEquals(5, plan.finalRemainder)
    }

    @Test
    fun planFor7156Div23() {
        val plan = generateDivisionPlan(7156, 23)
        assertEquals(311, plan.finalQuotient)
        assertEquals(3, plan.finalRemainder)
    }
}
