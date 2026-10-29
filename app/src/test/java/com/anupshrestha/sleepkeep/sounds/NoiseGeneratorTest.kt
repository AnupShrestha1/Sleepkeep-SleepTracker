package com.anupshrestha.sleepkeep.sounds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class NoiseGeneratorTest {

    private val rate = 22_050

    private fun samples(sound: SleepSound, seconds: Int = 2): ShortArray {
        val buffer = ShortArray(rate * seconds)
        NoiseGenerator(sound, rate).fill(buffer, 1f)
        return buffer
    }

    private fun rms(s: ShortArray) = sqrt(s.sumOf { it.toDouble() * it } / s.size)

    /** How often the signal crosses zero: high for bright sounds, low for deep ones. */
    private fun zeroCrossings(s: ShortArray) = (1 until s.size).count { (s[it - 1] < 0) != (s[it] < 0) }

    @Test
    fun `every sound is audible and never clips`() {
        SleepSound.entries.forEach { sound ->
            val s = samples(sound, seconds = 6)
            assertTrue("$sound is too quiet", rms(s) > 300)
            assertTrue("$sound clips", s.none { abs(it.toInt()) >= Short.MAX_VALUE * 0.5 })
        }
    }

    @Test
    fun `brown is deeper than pink, which is deeper than white`() {
        val white = zeroCrossings(samples(SleepSound.WHITE))
        val pink = zeroCrossings(samples(SleepSound.PINK))
        val brown = zeroCrossings(samples(SleepSound.BROWN))
        assertTrue("white $white > pink $pink", white > pink)
        assertTrue("pink $pink > brown $brown", pink > brown)
    }

    @Test
    fun `no timer fades in then stays at full volume`() {
        assertEquals(0f, NoiseGenerator.gainAt(0, null), 0.001f)
        assertEquals(1f, NoiseGenerator.gainAt(3_000, null), 0.001f)
        assertEquals(1f, NoiseGenerator.gainAt(5 * 60 * 60_000L, null), 0.001f)
    }

    @Test
    fun `timer fades out over the last minute and then is silent`() {
        val thirty = 30 * 60_000L
        assertEquals(1f, NoiseGenerator.gainAt(thirty - NoiseGenerator.FADE_MILLIS, thirty), 0.001f)
        val halfway = NoiseGenerator.gainAt(thirty - NoiseGenerator.FADE_MILLIS / 2, thirty)
        assertTrue(halfway in 0.5f..0.9f)
        assertEquals(0f, NoiseGenerator.gainAt(thirty, thirty), 0.001f)
        assertEquals(0f, NoiseGenerator.gainAt(thirty + 1, thirty), 0.001f)
    }

    @Test
    fun `gain scales the output`() {
        val full = ShortArray(rate).also { NoiseGenerator(SleepSound.PINK, rate, seed = 3).fill(it, 1f) }
        val half = ShortArray(rate).also { NoiseGenerator(SleepSound.PINK, rate, seed = 3).fill(it, 0.5f) }
        assertEquals(rms(full) / 2, rms(half), rms(full) * 0.02)
    }
}
