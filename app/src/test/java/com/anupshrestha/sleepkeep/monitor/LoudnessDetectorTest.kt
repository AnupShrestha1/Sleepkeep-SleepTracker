package com.anupshrestha.sleepkeep.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoudnessDetectorTest {

    private val window = LoudnessDetector.WINDOW_MILLIS

    /** Runs [levels] (one per 100 ms window, starting at time 0) and collects events. */
    private fun run(levels: List<Float>): List<LoudnessDetector.Event> {
        val detector = LoudnessDetector()
        val events = levels.mapIndexedNotNull { i, level -> detector.process(i * window, level) }
        return events + listOfNotNull(detector.flush(levels.size * window))
    }

    private fun seconds(s: Int, level: Float) = List(s * 10) { level }

    @Test
    fun `a clap in a quiet room is one short event`() {
        val levels = seconds(120, -60f) + List(5) { -25f } + seconds(10, -60f)
        val events = run(levels)
        assertEquals(1, events.size)
        val event = events.single()
        assertEquals(120_000L, event.startTime)
        assertEquals(120_500L, event.endTime)
        assertEquals(35f, event.peakAboveBackgroundDb, 0.01f)
    }

    @Test
    fun `nothing is reported during the first minute`() {
        val levels = seconds(30, -60f) + List(5) { -25f } + seconds(60, -60f)
        assertTrue(run(levels).isEmpty())
    }

    @Test
    fun `a fan running all night produces no events`() {
        // A steady fan, with small natural wobble.
        val levels = List(30 * 60 * 10) { i -> -40f + (i % 7) * 0.5f }
        assertTrue(run(levels).isEmpty())
    }

    @Test
    fun `a slow rise in background noise is not an event`() {
        // 30 dB louder over 30 minutes, e.g. traffic building up towards morning.
        val levels = List(30 * 60 * 10) { i -> -65f + 30f * i / (30 * 60 * 10) }
        assertTrue(run(levels).isEmpty())
    }

    @Test
    fun `near-silent room ignores tiny sounds`() {
        // 20 dB above background, but still very quiet in absolute terms.
        val levels = seconds(120, -85f) + List(10) { -65f } + seconds(10, -85f)
        assertTrue(run(levels).isEmpty())
    }

    @Test
    fun `a blip shorter than 300 ms is ignored`() {
        val levels = seconds(120, -60f) + List(2) { -25f } + seconds(10, -60f)
        assertTrue(run(levels).isEmpty())
    }

    @Test
    fun `sounds less than 2 seconds apart are one event`() {
        val levels = seconds(120, -60f) + List(5) { -25f } + List(10) { -60f } + List(5) { -25f } + seconds(10, -60f)
        val event = run(levels).single()
        assertEquals(120_000L, event.startTime)
        assertEquals(122_000L, event.endTime)
    }

    @Test
    fun `an event still going when monitoring stops is kept`() {
        val levels = seconds(120, -60f) + seconds(5, -25f)
        val event = run(levels).single()
        assertEquals(120_000L, event.startTime)
        assertEquals(125_000L, event.endTime)
    }

    @Test
    fun `level of full-scale samples is about 0 dBFS and silence is the minimum`() {
        val loud = ShortArray(100) { if (it % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE }
        assertEquals(0f, LoudnessDetector.levelDb(loud, loud.size), 0.01f)
        assertEquals(LoudnessDetector.MIN_DB, LoudnessDetector.levelDb(ShortArray(100), 100))
    }
}
