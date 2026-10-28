package com.anupshrestha.sleepkeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anupshrestha.sleepkeep.ui.dashboard.DashboardScreen
import com.anupshrestha.sleepkeep.ui.theme.SleepkeepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepkeepTheme {
                DashboardScreen()
            }
        }
    }
}
