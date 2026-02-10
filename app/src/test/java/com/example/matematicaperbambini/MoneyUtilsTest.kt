package com.example.matematicaperbambini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyUtilsTest {
    @Test
    fun parseInputToCentsSupportsCommaAndDot() {
        assertEquals(1250, parseInputToCents("12,50"))
        assertEquals(1250, parseInputToCents("12.50"))
    }

    @Test
    fun parseInputToCentsRejectsInvalidValues() {
        assertNull(parseInputToCents(""))
        assertNull(parseInputToCents("12..2"))
        assertNull(parseInputToCents("abc"))
    }
}
