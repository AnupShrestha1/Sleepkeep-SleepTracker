package com.anupshrestha.sleepkeep

import android.content.Context
import com.anupshrestha.sleepkeep.data.SleepDatabase
import com.anupshrestha.sleepkeep.data.SleepRepository
import java.time.Clock

/** Builds the app's long-lived objects once; screens get them from here. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val database: SleepDatabase = SleepDatabase.create(context)
    val repository = SleepRepository(database, clock)
}
