package com.anupshrestha.sleepkeep.ui.common

import androidx.annotation.StringRes
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SessionRules.TimeError

@StringRes
fun timeErrorText(error: TimeError): Int = when (error) {
    TimeError.END_BEFORE_START -> R.string.error_end_before_start
    TimeError.IN_FUTURE -> R.string.error_in_future
    TimeError.TOO_SHORT -> R.string.error_too_short
    TimeError.TOO_LONG -> R.string.error_too_long
    TimeError.OVERLAPS -> R.string.error_overlaps
}
