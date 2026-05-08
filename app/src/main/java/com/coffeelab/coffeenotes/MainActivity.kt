package com.coffeelab.coffeenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.Equipment
import com.coffeelab.coffeenotes.data.entity.Grinder
import com.coffeelab.coffeenotes.ui.navigation.CoffeeNavGraph
import com.coffeelab.coffeenotes.ui.navigation.Screen
import com.coffeelab.coffeenotes.ui.theme.CoffeeNotesTheme
import kotlinx.coroutines.launch

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure default equipment and grinders exist on every startup
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            val equipmentDao = db.equipmentDao()
            val grinderDao = db.grinderDao()
            if (equipmentDao.getAllOnce().isEmpty()) {
                val items = Equipment.DEFAULT_EQUIPMENT.mapIndexed { index, name ->
                    Equipment(name = name, sortOrder = index)
                }
                equipmentDao.insertAll(items)
            }
            if (grinderDao.getAllOnce().isEmpty()) {
                val items = Grinder.DEFAULT_GRINDERS.mapIndexed { index, name ->
                    Grinder(name = name, sortOrder = index)
                }
                grinderDao.insertAll(items)
            }
        }

        setContent {
            CoffeeNotesTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Main tab items
                val bottomNavItems = listOf(
                    BottomNavItem("首页", Icons.Default.Home, Screen.Home.route),
                    BottomNavItem("豆子", Icons.Default.Grain, Screen.BeanList.route),
                    BottomNavItem("冲煮", Icons.Default.Coffee, Screen.BrewList.createRoute()),
                    BottomNavItem("设置", Icons.Default.Settings, Screen.Settings.route)
                )

                // Routes that show the bottom bar
                val mainRoutes = setOf(
                    Screen.Home.route,
                    Screen.BeanList.route,
                    Screen.BrewList.createRoute(),
                    Screen.Settings.route,
                    Screen.ArchiveList.route
                )

                val showBottomBar = currentRoute in mainRoutes ||
                    (currentRoute?.startsWith("brew_list/") == true &&
                     navBackStackEntry?.arguments?.getLong("beanId") == -1L)

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomNavItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    CoffeeNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
