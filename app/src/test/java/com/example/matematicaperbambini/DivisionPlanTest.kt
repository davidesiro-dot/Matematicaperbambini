package com.example.matematicaperbambini

import org.junit.Assert.assertEquals
import org.junit.Test

class DivisionPlanTest {
    @Test
    fun div1DigitExample() {
        val plan = generateDivisionPlan(1729, 8)
        assertEquals(216, plan.finalQuotient)
        assertEquals(1, plan.finalRemainder)
        assertEquals(listOf(2, 1, 6), plan.quotientDigits)
    }

    @Test
    fun div1DigitZeroInQuotient() {
        val plan = generateDivisionPlan(1629, 8)
        assertEquals(203, plan.finalQuotient)
        assertEquals(5, plan.finalRemainder)
        assertEquals(listOf(2, 0, 3), plan.quotientDigits)
    }

    @Test
    fun div2DigitExample() {
        val plan = generateDivisionPlan(7156, 23)
        assertEquals(311, plan.finalQuotient)
        assertEquals(3, plan.finalRemainder)
        assertEquals(listOf(3, 1, 1), plan.quotientDigits)
    }

    @Test
    fun div2DigitEstimateCorrection() {
        val plan = generateDivisionPlan(6756, 23)
        assertEquals(293, plan.finalQuotient)
        assertEquals(17, plan.finalRemainder)
        assertEquals(listOf(2, 9, 3), plan.quotientDigits)
    }
}
