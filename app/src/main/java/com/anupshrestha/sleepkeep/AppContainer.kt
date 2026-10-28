package com.anupshrestha.sleepkeep

import android.content.Context
import com.anupshrestha.sleepkeep.data.SleepDatabase
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.monitor.MonitorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/** Builds the app's long-lived objects once; screens get them from here. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val database: SleepDatabase = SleepDatabase.create(context)
    val repository = SleepRepository(database, clock)
    val monitor = MonitorController(context)

    /** For work that must finish even if the screen that started it goes away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
