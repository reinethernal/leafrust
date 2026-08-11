package com.leafrust

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.leafrust.ui.inspections.InspectionsScreen
import com.leafrust.ui.result.ResultScreen
import com.leafrust.ui.scan.ScanScreen
import com.leafrust.ui.settings.SettingsScreen
import com.leafrust.ui.theme.FieldElevated
import com.leafrust.ui.theme.InkMuted
import com.leafrust.ui.theme.LeafGreen
import com.leafrust.ui.theme.LeafRustTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeafRustTheme {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route.orEmpty()
                val showBar = route in setOf("scan", "inspections", "settings")

                Scaffold(
                    bottomBar = {
                        if (showBar) {
                            NavigationBar(containerColor = FieldElevated) {
                                NavigationBarItem(
                                    selected = route == "scan",
                                    onClick = { nav.navigate("scan") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    label = { Text("Скан") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LeafGreen,
                                        selectedTextColor = LeafGreen,
                                        unselectedIconColor = InkMuted,
                                        unselectedTextColor = InkMuted,
                                    ),
                                )
                                NavigationBarItem(
                                    selected = route == "inspections",
                                    onClick = { nav.navigate("inspections") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                                    label = { Text("История") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LeafGreen,
                                        selectedTextColor = LeafGreen,
                                        unselectedIconColor = InkMuted,
                                        unselectedTextColor = InkMuted,
                                    ),
                                )
                                NavigationBarItem(
                                    selected = route == "settings",
                                    onClick = { nav.navigate("settings") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text("Ещё") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LeafGreen,
                                        selectedTextColor = LeafGreen,
                                        unselectedIconColor = InkMuted,
                                        unselectedTextColor = InkMuted,
                                    ),
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = "scan",
                        modifier = Modifier.padding(padding),
                    ) {
                        composable("scan") {
                            ScanScreen(onResult = { id -> nav.navigate("result/$id") })
                        }
                        composable("inspections") {
                            InspectionsScreen(onOpen = { id -> nav.navigate("result/$id") })
                        }
                        composable("settings") { SettingsScreen() }
                        composable(
                            route = "result/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { entry ->
                            val id = entry.arguments?.getLong("id") ?: return@composable
                            ResultScreen(
                                id = id,
                                onBackToScan = {
                                    nav.navigate("scan") {
                                        popUpTo("scan") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onDeleted = {
                                    nav.navigate("inspections") {
                                        popUpTo("scan")
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
