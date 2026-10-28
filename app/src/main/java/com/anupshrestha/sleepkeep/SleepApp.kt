package com.anupshrestha.sleepkeep

import android.app.Application

class SleepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
