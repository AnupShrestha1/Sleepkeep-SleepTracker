package com.anupshrestha.sleepkeep.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anupshrestha.sleepkeep.ui.dashboard.DashboardScreen
import com.anupshrestha.sleepkeep.ui.guidance.GuidanceScreen
import com.anupshrestha.sleepkeep.ui.history.HistoryScreen
import com.anupshrestha.sleepkeep.ui.night.NightDetailScreen
import com.anupshrestha.sleepkeep.ui.night.NightDetailViewModel
import com.anupshrestha.sleepkeep.ui.settings.SettingsScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val GUIDANCE = "guidance"
    const val SETTINGS = "settings"
    const val NIGHT = "night/{${NightDetailViewModel.NIGHT_ID_ARG}}"
    fun night(id: Long) = "night/$id"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenNight = { navController.navigate(Routes.night(it)) },
                onOpenGuidance = { navController.navigate(Routes.GUIDANCE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GUIDANCE) {
            GuidanceScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenNight = { navController.navigate(Routes.night(it)) },
            )
        }
        composable(
            route = Routes.NIGHT,
            arguments = listOf(navArgument(NightDetailViewModel.NIGHT_ID_ARG) { type = NavType.LongType }),
        ) {
            NightDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
