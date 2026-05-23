package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppRepository
import com.example.data.local.SettingsRepository
import com.example.ui.chat.ChatScreen
import com.example.ui.image.ImageRenderScreen
import com.example.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    object Image : Screen("image", "Image", Icons.Filled.Image)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun NovaNavHost(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Chat,
        Screen.Image,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Chat.route, Modifier.padding(innerPadding)) {
            composable(Screen.Chat.route) {
                ChatScreen(appRepository, settingsRepository)
            }
            composable(Screen.Image.route) {
                ImageRenderScreen(appRepository, settingsRepository)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(settingsRepository)
            }
        }
    }
}
