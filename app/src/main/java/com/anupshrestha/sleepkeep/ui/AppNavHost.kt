package com.anupshrestha.sleepkeep.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.ui.common.NightBackground
import com.anupshrestha.sleepkeep.ui.dashboard.DashboardScreen
import com.anupshrestha.sleepkeep.ui.guidance.GuidanceScreen
import com.anupshrestha.sleepkeep.ui.history.HistoryScreen
import com.anupshrestha.sleepkeep.ui.night.NightDetailScreen
import com.anupshrestha.sleepkeep.ui.night.NightDetailViewModel
import com.anupshrestha.sleepkeep.ui.settings.SettingsScreen
import com.anupshrestha.sleepkeep.ui.welcome.WelcomeScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val GUIDANCE = "guidance"
    const val SETTINGS = "settings"
    const val NIGHT = "night/{${NightDetailViewModel.NIGHT_ID_ARG}}"
    fun night(id: Long) = "night/$id"
}

private data class Tab(val route: String, @DrawableRes val icon: Int, @StringRes val label: Int)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, R.drawable.ic_home, R.string.tab_home),
    Tab(Routes.HISTORY, R.drawable.ic_history, R.string.tab_history),
    Tab(Routes.GUIDANCE, R.drawable.ic_insights, R.string.tab_insights),
    Tab(Routes.SETTINGS, R.drawable.ic_settings, R.string.tab_settings),
)

@Composable
fun AppNavHost() {
    val repository = (LocalContext.current.applicationContext as SleepApp).container.repository
    val hasSessions by repository.hasSessions.collectAsStateWithLifecycle(initialValue = true)
    var welcomeDismissed by rememberSaveable { mutableStateOf(false) }

    NightBackground {
        if (!hasSessions && !welcomeDismissed) {
            WelcomeScreen(onGetStarted = { welcomeDismissed = true })
            return@NightBackground
        }

        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val showTabs = tabs.any { it.route == currentRoute }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showTabs) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                                label = { Text(stringResource(tab.label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            // Screens draw their own top insets; only the bottom bar's space is taken here.
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(onOpenNight = { navController.navigate(Routes.night(it)) })
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(onOpenNight = { navController.navigate(Routes.night(it)) })
                }
                composable(Routes.GUIDANCE) { GuidanceScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(
                    route = Routes.NIGHT,
                    arguments = listOf(navArgument(NightDetailViewModel.NIGHT_ID_ARG) { type = NavType.LongType }),
                ) {
                    NightDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
