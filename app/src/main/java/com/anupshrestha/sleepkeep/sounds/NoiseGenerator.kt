package com.anupshrestha.sleepkeep.sounds

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** The sleep sounds. Rain, storm and ocean are CC0 recordings (see [SleepSoundService]); the noises are generated. */
enum class SleepSound { RAIN, STORM, OCEAN, WHITE, PINK, BROWN }

/**
 * Makes an endless stream of 16-bit mono samples for one [SleepSound]. Plain Kotlin, so it can be
 * tested without a phone.
 *
 * - White: equal energy at all frequencies (bright hiss).
 * - Pink: falls off with frequency, softer (Paul Kellett's filter).
 * - Brown: falls off faster still, a deep rumble (leaky integration of white noise).
 * - Rain: soft pink noise plus many tiny decaying "drops".
 * - Storm: heavier rain plus an occasional long rumble of thunder.
 * - Ocean: brown and pink noise swelling and fading like waves every 8–12 s.
 */
class NoiseGenerator(
    private val sound: SleepSound,
    private val sampleRate: Int,
    seed: Long = 1,
) {
    private val random = Random(seed)

    // Pink filter state.
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var b3 = 0.0
    private var b4 = 0.0
    private var b5 = 0.0
    private var b6 = 0.0

    // Brown state.
    private var brown = 0.0

    // Drops: a few overlapping decaying bursts.
    private var dropLevel = 0.0
    private val dropDecay = exp(-1.0 / (0.004 * sampleRate))

    // Thunder: an envelope over brown noise.
    private var thunderLevel = 0.0
    private var thunderTarget = 0.0
    private var samplesToNextThunder = nextThunderGap()
    private var thunderRumble = 0.0

    // Waves.
    private var t = 0L
    private var wavePeriod = 10.0

    private fun white(): Double = random.nextDouble() * 2 - 1

    private fun pink(w: Double): Double {
        b0 = 0.99886 * b0 + w * 0.0555179
        b1 = 0.99332 * b1 + w * 0.0750759
        b2 = 0.96900 * b2 + w * 0.1538520
        b3 = 0.86650 * b3 + w * 0.3104856
        b4 = 0.55000 * b4 + w * 0.5329522
        b5 = -0.7616 * b5 - w * 0.0168980
        val out = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
        b6 = w * 0.115926
        return out * 0.11
    }

    private fun brownNext(w: Double): Double {
        brown = (brown + 0.02 * w) / 1.02
        return brown * 3.5
    }

    private fun nextThunderGap(): Int = ((20 + random.nextDouble() * 40) * sampleRate).toInt()

    private fun rain(w: Double, dropsPerSecond: Double, dropLoudness: Double): Double {
        if (random.nextDouble() < dropsPerSecond / sampleRate) {
            dropLevel += dropLoudness * (0.3 + random.nextDouble() * 0.7)
        }
        dropLevel *= dropDecay
        return pink(w) * 0.55 + white() * dropLevel
    }

    private fun next(): Double {
        val w = white()
        t++
        return when (sound) {
            SleepSound.WHITE -> w * 0.35
            SleepSound.PINK -> pink(w)
            SleepSound.BROWN -> brownNext(w)
            SleepSound.RAIN -> rain(w, dropsPerSecond = 350.0, dropLoudness = 0.25)
            SleepSound.STORM -> {
                val base = rain(w, dropsPerSecond = 700.0, dropLoudness = 0.35)
                if (--samplesToNextThunder <= 0) {
                    thunderTarget = 0.6 + random.nextDouble() * 0.4
                    samplesToNextThunder = nextThunderGap()
                }
                // Rises over about half a second, then fades over several seconds.
                thunderLevel += (thunderTarget - thunderLevel) * (4.0 / sampleRate)
                thunderTarget *= exp(-1.0 / (2.5 * sampleRate))
                thunderRumble = (thunderRumble + 0.01 * w) / 1.01
                base + thunderRumble * 9 * thunderLevel
            }
            SleepSound.OCEAN -> {
                val phase = 2 * PI * t / (wavePeriod * sampleRate)
                if (phase >= 2 * PI) {
                    t = 0
                    wavePeriod = 8 + random.nextDouble() * 4
                }
                val swell = 0.25 + 0.75 * (0.5 - 0.5 * kotlin.math.cos(phase))
                (brownNext(w) * 0.8 + pink(w) * 0.5) * swell
            }
        }
    }

    /** Fills [buffer] with the next samples at [gain] (0–1). */
    fun fill(buffer: ShortArray, gain: Float) {
        for (i in buffer.indices) {
            val v = (next() * gain).coerceIn(-1.0, 1.0)
            buffer[i] = (v * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
    }

    companion object {
        /** Fade-out length at the end of a timer, so the sound doesn't stop abruptly. */
        const val FADE_MILLIS = 60_000L

        /**
         * Volume over time for a timer of [durationMillis] (`null` = no timer): full until the
         * last minute, then a smooth fade to silence. Also fades in over the first 3 seconds.
         */
        fun gainAt(elapsedMillis: Long, durationMillis: Long?): Float {
            val fadeIn = (elapsedMillis / 3_000f).coerceIn(0f, 1f)
            if (durationMillis == null) return fadeIn
            val remaining = durationMillis - elapsedMillis
            if (remaining <= 0) return 0f
            val fadeOut = (remaining.toFloat() / FADE_MILLIS).coerceIn(0f, 1f)
            // Ease out (half a sine), which sounds more even than a straight line.
            return fadeIn * sin(fadeOut * PI.toFloat() / 2)
        }
    }
}
