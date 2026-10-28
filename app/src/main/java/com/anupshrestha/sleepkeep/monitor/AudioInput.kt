package com.anupshrestha.sleepkeep.monitor

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/** An opened microphone plus the sizes the read loop needs. Audio is analysed and discarded. */
class AudioInput private constructor(
    val record: AudioRecord,
    val sampleRate: Int,
    /** Samples per read: 100 ms. */
    val chunkSize: Int,
) {
    companion object {
        /**
         * VOICE_RECOGNITION applies less automatic gain on most phones, which keeps levels
         * comparable through the night; MIC is the fallback. 44.1 kHz is the only rate every
         * phone must support. Returns null if no combination opens.
         */
        @SuppressLint("MissingPermission") // The service checks RECORD_AUDIO before calling this.
        fun open(): AudioInput? {
            for (source in listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC)) {
                for (rate in listOf(16_000, 44_100)) {
                    val minBytes = AudioRecord.getMinBufferSize(
                        rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minBytes <= 0) continue
                    val chunk = rate / 10
                    val record = try {
                        AudioRecord(
                            source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                            maxOf(minBytes, chunk * 2 * 4),
                        )
                    } catch (e: IllegalArgumentException) {
                        continue
                    }
                    if (record.state == AudioRecord.STATE_INITIALIZED) return AudioInput(record, rate, chunk)
                    record.release()
                }
            }
            return null
        }
    }
}
