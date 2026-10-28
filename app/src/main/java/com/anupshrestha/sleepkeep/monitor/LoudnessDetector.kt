package com.anupshrestha.sleepkeep.monitor

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Finds periods noticeably louder than the room's own background. Fed one level every 100 ms;
 * plain Kotlin so it can be tested with made-up nights.
 *
 * All thresholds are starting points, to be tuned on real nights (plan step 7). Phone microphones
 * aren't calibrated, so everything is relative to the background, never an absolute decibel value.
 */
class LoudnessDetector {

    data class Event(val startTime: Long, val endTime: Long, val peakAboveBackgroundDb: Float)

    private val history = FloatArray(HISTORY_WINDOWS)
    private var historyCount = 0
    private var historyNext = 0
    private var windowsSeen = 0L
    private var windowsSinceBackground = 0

    /** dBFS; starts as quiet as possible until there's history. */
    var background = MIN_DB
        private set

    private var loudRun = 0
    private var quietRun = 0
    private var loudRunStart = 0L
    private var quietRunStart = 0L
    private var eventStart: Long? = null
    private var eventPeak = 0f

    /** Feeds one window's level; returns an event when one has just ended. */
    fun process(time: Long, levelDb: Float): Event? {
        addToHistory(levelDb)
        windowsSeen++
        if (++windowsSinceBackground >= BACKGROUND_EVERY_WINDOWS || windowsSeen <= BACKGROUND_EVERY_WINDOWS) {
            background = percentile(BACKGROUND_PERCENTILE)
            windowsSinceBackground = 0
        }
        if (windowsSeen <= WARM_UP_WINDOWS) return null

        val above = levelDb - background
        val start = eventStart
        if (start == null) {
            if (above >= START_ABOVE_DB && levelDb >= MIN_EVENT_DB) {
                if (loudRun == 0) loudRunStart = time
                loudRun++
                eventPeak = maxOf(eventPeak, above)
                if (loudRun >= START_WINDOWS) {
                    eventStart = loudRunStart
                    quietRun = 0
                }
            } else {
                loudRun = 0
                eventPeak = 0f
            }
            return null
        }

        eventPeak = maxOf(eventPeak, above)
        if (above < END_ABOVE_DB) {
            if (quietRun == 0) quietRunStart = time
            quietRun++
            if (quietRun >= END_WINDOWS) return closeEvent(start, quietRunStart)
        } else {
            quietRun = 0
        }
        return null
    }

    /** Ends any event in progress, e.g. when monitoring stops or the microphone goes silent. */
    fun flush(time: Long): Event? {
        val start = eventStart ?: return null
        return closeEvent(start, if (quietRun > 0) quietRunStart else time)
    }

    private fun closeEvent(start: Long, end: Long): Event {
        val event = Event(start, maxOf(end, start + WINDOW_MILLIS), eventPeak)
        eventStart = null
        loudRun = 0
        quietRun = 0
        eventPeak = 0f
        return event
    }

    private fun addToHistory(level: Float) {
        history[historyNext] = level
        historyNext = (historyNext + 1) % HISTORY_WINDOWS
        if (historyCount < HISTORY_WINDOWS) historyCount++
    }

    private fun percentile(fraction: Float): Float {
        val sorted = history.copyOf(historyCount).also { it.sort() }
        return sorted[((historyCount - 1) * fraction).toInt()]
    }

    companion object {
        const val WINDOW_MILLIS = 100L
        private const val HISTORY_WINDOWS = 5 * 60 * 10 // 5 minutes
        private const val BACKGROUND_EVERY_WINDOWS = 50 // 5 s
        private const val BACKGROUND_PERCENTILE = 0.2f
        private const val WARM_UP_WINDOWS = 60 * 10 // 60 s
        private const val START_ABOVE_DB = 15f
        private const val END_ABOVE_DB = 10f
        private const val START_WINDOWS = 3 // 300 ms
        private const val END_WINDOWS = 20 // 2 s
        /** Ignores "loud" sounds that are still tiny, e.g. in a near-silent room. */
        private const val MIN_EVENT_DB = -50f
        const val MIN_DB = -120f

        /** Level of 16-bit samples in dBFS (0 = loudest possible). */
        fun levelDb(samples: ShortArray, count: Int): Float {
            if (count <= 0) return MIN_DB
            var sumSquares = 0.0
            for (i in 0 until count) {
                val s = samples[i].toDouble()
                sumSquares += s * s
            }
            val rms = sqrt(sumSquares / count)
            if (rms <= 0.0) return MIN_DB
            return (20 * log10(rms / 32768.0)).toFloat().coerceAtLeast(MIN_DB)
        }
    }
}
