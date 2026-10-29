package com.anupshrestha.sleepkeep.sounds

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SoundStatus {
    data object Off : SoundStatus

    /** [endsAt] is when the timer finishes (UTC millis), or `null` with no timer. */
    data class Playing(val sound: SleepSound, val endsAt: Long?) : SoundStatus
}

/** Starts and stops [SleepSoundService]; the UI, the service and monitoring share its [status]. */
class SleepSoundController(private val context: Context) {

    internal val mutableStatus = MutableStateFlow<SoundStatus>(SoundStatus.Off)
    val status: StateFlow<SoundStatus> = mutableStatus.asStateFlow()

    val isPlaying: Boolean get() = status.value is SoundStatus.Playing

    /** [minutes] `null` plays until stopped. Call while the app is on screen. */
    fun play(sound: SleepSound, minutes: Int?): Boolean = try {
        ContextCompat.startForegroundService(
            context,
            Intent(context, SleepSoundService::class.java)
                .setAction(SleepSoundService.ACTION_PLAY)
                .putExtra(SleepSoundService.EXTRA_SOUND, sound.name)
                .putExtra(SleepSoundService.EXTRA_MINUTES, minutes ?: 0),
        )
        true
    } catch (e: IllegalStateException) {
        false
    }

    fun stop() {
        if (!isPlaying) return
        try {
            context.startService(SleepSoundService.stopIntent(context))
        } catch (e: IllegalStateException) {
            // Not allowed from the background; the service is gone with its process anyway.
        }
    }
}
