package com.example.matematicaperbambini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeworkCodeNormalizationTest {
    @Test
    fun normalizesCodeWithSpaces() {
        assertEquals("ABCD1234", normalizeHomeworkCode("  AB CD 1234  "))
    }

    @Test
    fun convertsLowercaseToUppercase() {
        assertEquals("ABCD1234", normalizeHomeworkCode("abcd1234"))
    }

    @Test
    fun removesDashesFromCode() {
        assertEquals("ABCD1234", normalizeHomeworkCode("ABCD-1234"))
    }

    @Test
    fun returnsNullForTooShortInvalidCodeLookup() {
        val payload = findHomeworkCodePayload(code = "ab-1", entries = emptyList())

        assertNull(payload)
    }
}
