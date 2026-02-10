package com.acutis.firewall.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.acutis.firewall.ui.screens.blocklist.BlocklistScreen
import com.acutis.firewall.ui.screens.blocklist.CustomListDetailScreen
import com.acutis.firewall.ui.screens.home.HomeScreen
import com.acutis.firewall.ui.screens.settings.SettingsScreen
import com.acutis.firewall.ui.screens.timerules.TimeRulesScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Blocklist : Screen("blocklist")
    object CustomListDetail : Screen("custom_list/{listId}") {
        fun createRoute(listId: Long) = "custom_list/$listId"
    }
    object TimeRules : Screen("time_rules")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBlocklist = {
                    navController.navigate(Screen.Blocklist.route)
                },
                onNavigateToTimeRules = {
                    navController.navigate(Screen.TimeRules.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Blocklist.route) {
            BlocklistScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToListDetail = { listId ->
                    navController.navigate(Screen.CustomListDetail.createRoute(listId))
                }
            )
        }

        composable(
            route = Screen.CustomListDetail.route,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) {
            CustomListDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TimeRules.route) {
            TimeRulesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
