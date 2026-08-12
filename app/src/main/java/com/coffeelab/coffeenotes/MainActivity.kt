package com.coffeelab.coffeenotes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
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

    companion object {
        const val PREFS_NAME = "coffee_notes_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val EXTRA_NAVIGATE_TO_BACKUP = "navigate_to_backup"

        fun setThemeModeAndRestart(activity: Activity, mode: String) {
            activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME_MODE, mode)
                .apply()
            activity.recreate()
        }
    }

    private var navController: androidx.navigation.NavHostController? = null
    private var pendingBackupNavigation = false

    /** 通知权限请求（Android 13+ 备份提醒用） */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingNotificationCallback?.invoke(granted)
            pendingNotificationCallback = null
        }
    private var pendingNotificationCallback: ((Boolean) -> Unit)? = null

    fun requestNotificationPermission(callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                callback(true)
            } else {
                pendingNotificationCallback = callback
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            callback(true)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_TO_BACKUP, false)) {
            val controller = navController
            if (controller != null) {
                controller.navigate(Screen.Backup.route)
            } else {
                pendingBackupNavigation = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 通知点击进入备份页：冷启动时记录 pending，待 navController 就绪后导航
        pendingBackupNavigation = intent.getBooleanExtra(EXTRA_NAVIGATE_TO_BACKUP, false)
        // 恢复备份提醒闹钟（install -r / 系统清理会取消已注册 alarm，启动时按锚点重建）
        com.coffeelab.coffeenotes.util.BackupReminder.rescheduleIfNeeded(this)

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
            val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            val themeMode = prefs.getString(MainActivity.KEY_THEME_MODE, "system") ?: "system"
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            CoffeeNotesTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                this@MainActivity.navController = navController
                LaunchedEffect(navController) {
                    if (pendingBackupNavigation) {
                        pendingBackupNavigation = false
                        navController.navigate(Screen.Backup.route)
                    }
                }
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
