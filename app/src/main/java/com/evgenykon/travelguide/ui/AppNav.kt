package com.evgenykon.travelguide.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.ui.auth.OpenRouterAuthScreen
import com.evgenykon.travelguide.ui.auth.YandexAuthScreen
import com.evgenykon.travelguide.ui.history.HistoryScreen
import com.evgenykon.travelguide.ui.map.MapScreen
import com.evgenykon.travelguide.ui.routes.RouteDetailScreen
import com.evgenykon.travelguide.ui.routes.RoutesScreen
import com.evgenykon.travelguide.ui.settings.SettingsScreen

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem("map", "Карта", Icons.Default.Map),
    TabItem("routes", "Маршруты", Icons.Default.Route),
    TabItem("history", "История", Icons.Default.History),
    TabItem("settings", "Настройки", Icons.Default.Settings)
)

@Composable
fun AppNav(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(padding)
        ) {
            composable("map") { MapScreen(container, navController) }
            composable("routes") { RoutesScreen(container, navController) }
            composable(
                "routes/{routeId}",
                arguments = listOf(navArgument("routeId") { type = NavType.LongType })
            ) { entry ->
                RouteDetailScreen(
                    container = container,
                    navController = navController,
                    routeId = entry.arguments?.getLong("routeId") ?: 0L
                )
            }
            composable("history") { HistoryScreen(container) }
            composable("settings") { SettingsScreen(container, navController) }
            composable("auth/yandex") { YandexAuthScreen(container) }
            composable("auth/openrouter") { OpenRouterAuthScreen(container) }
        }
    }
}
