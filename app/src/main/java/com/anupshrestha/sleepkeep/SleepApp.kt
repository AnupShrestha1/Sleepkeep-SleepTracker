package com.anupshrestha.sleepkeep

import android.app.Application
import kotlinx.coroutines.launch

class SleepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // A fresh process means any monitoring from before was killed with the old one.
        val processStart = container.clock.millis()
        container.appScope.launch { container.repository.closeStaleSegments(processStart) }
    }
}
