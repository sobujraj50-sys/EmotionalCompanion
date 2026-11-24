package com.emotionalcompanion.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emotionalcompanion.R
import com.emotionalcompanion.core.common.AppContainer
import com.emotionalcompanion.core.designsystem.AppIcons
import com.emotionalcompanion.features.home.HomeScreen
import com.emotionalcompanion.features.settings.ApiConfigScreen
import com.emotionalcompanion.features.settings.SettingsScreen
import com.emotionalcompanion.features.voicestudio.VoiceStudioScreen

private const val API_CONFIG_ROUTE = "settings/api"

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: () -> ImageVector,
) {
    Home("home", R.string.nav_home, { Icons.Filled.Home }),
    Voice("voice", R.string.nav_voice, { AppIcons.GraphicEq }),
    Settings("settings", R.string.nav_settings, { Icons.Filled.Settings }),
}

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route ||
                            (destination == TopLevelDestination.Settings && currentRoute == API_CONFIG_ROUTE),
                        onClick = { navController.navigateTopLevel(destination) },
                        icon = { Icon(destination.icon(), contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.Home.route) {
                HomeScreen(
                    container = container,
                    onOpenApiConfig = { navController.navigate(API_CONFIG_ROUTE) },
                    onOpenVoiceStudio = { navController.navigateTopLevel(TopLevelDestination.Voice) },
                    onOpenSettings = { navController.navigateTopLevel(TopLevelDestination.Settings) },
                )
            }
            composable(TopLevelDestination.Voice.route) {
                VoiceStudioScreen()
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(
                    container = container,
                    onOpenApiConfig = { navController.navigate(API_CONFIG_ROUTE) },
                )
            }
            composable(API_CONFIG_ROUTE) {
                ApiConfigScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
