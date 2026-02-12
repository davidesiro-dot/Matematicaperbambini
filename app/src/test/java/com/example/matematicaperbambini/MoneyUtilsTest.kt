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

    @Test
    fun formatEuroHandlesZeroValue() {
        assertEquals("€0,00", formatEuro(0))
    }

    @Test
    fun parseInputToCentsRejectsNegativeValues() {
        assertNull(parseInputToCents("-1"))
        assertNull(parseInputToCents("-2,50"))
    }

    @Test
    fun formatEuroHandlesBoundaryCents() {
        assertEquals("€0,99", formatEuro(99))
    }

    @Test
    fun formatEuroHandlesLargeNumbers() {
        assertEquals("€123456,78", formatEuro(12_345_678))
    }
}
