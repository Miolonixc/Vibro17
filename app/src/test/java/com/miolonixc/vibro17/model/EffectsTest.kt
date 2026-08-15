package com.miolonixc.vibro17.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanity checks for the built-in preset table. A malformed effect (mismatched
 * timings/amplitudes, out-of-range amplitude, negative duration…) would crash
 * the vibrator API at runtime, so we assert the invariants up front.
 */
class EffectsTest {

    private val effects = Effects.ALL

    @Test
    fun everyEffectHasMatchingTimingsAndAmplitudes() {
        for (e in effects) {
            assertEquals(
                "Effect '${e.id}' timings/amplitudes length mismatch",
                e.timings.size,
                e.amplitudes.size
            )
        }
    }

    @Test
    fun everyEffectHasAtLeastOneSegment() {
        for (e in effects) {
            assertTrue("Effect '${e.id}' has no segments", e.timings.isNotEmpty())
        }
    }

    @Test
    fun durationsAreNonNegative() {
        for (e in effects) {
            for ((i, d) in e.timings.withIndex()) {
                assertTrue("Effect '${e.id}' segment $i has negative duration", d >= 0)
            }
        }
    }

    @Test
    fun amplitudesAreWithinRange() {
        for (e in effects) {
            for ((i, a) in e.amplitudes.withIndex()) {
                assertTrue(
                    "Effect '${e.id}' amplitude $i out of range: $a",
                    a in 0..255
                )
            }
        }
    }

    @Test
    fun idsAreUnique() {
        val seen = mutableSetOf<String>()
        for (e in effects) {
            assertFalse("Duplicate effect id '${e.id}'", e.id in seen)
            seen.add(e.id)
        }
    }

    @Test
    fun everyEffectHasACategory() {
        for (e in effects) {
            assertTrue(
                "Effect '${e.id}' has blank category",
                e.category.isNotBlank()
            )
        }
    }
}
