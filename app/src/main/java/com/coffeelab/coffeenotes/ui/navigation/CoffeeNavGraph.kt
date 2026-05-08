package com.coffeelab.coffeenotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.coffeelab.coffeenotes.ui.screen.*
import com.coffeelab.coffeenotes.viewmodel.*

@Composable
fun CoffeeNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    // Shared ViewModels scoped to NavGraph — persist across navigation
    val beanViewModel: BeanViewModel = viewModel()
    val brewViewModel: BrewViewModel = viewModel()
    val brewMethodViewModel: BrewMethodViewModel = viewModel()
    val equipmentViewModel: EquipmentViewModel = viewModel()
    val grinderViewModel: GrinderViewModel = viewModel()
    val statsViewModel: StatsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, beanViewModel = beanViewModel, brewViewModel = brewViewModel)
        }

        composable(Screen.BeanList.route) {
            BeanListScreen(navController = navController, viewModel = beanViewModel)
        }

        composable(
            route = Screen.BeanDetail.route,
            arguments = listOf(navArgument("beanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            BeanDetailScreen(navController = navController, beanId = beanId, beanViewModel = beanViewModel, brewViewModel = brewViewModel)
        }

        composable(
            route = Screen.BeanEdit.route,
            arguments = listOf(navArgument("beanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            BeanEditScreen(navController = navController, beanId = beanId)
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("beanId") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            val mode = backStackEntry.arguments?.getString("mode") ?: "keyword"
            CameraScreen(navController = navController, beanId = beanId, recognitionMode = mode)
        }

        composable(
            route = Screen.BrewList.route,
            arguments = listOf(navArgument("beanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            BrewListScreen(navController = navController, beanId = beanId, brewViewModel = brewViewModel, beanViewModel = beanViewModel)
        }

        composable(
            route = Screen.BrewEdit.route,
            arguments = listOf(
                navArgument("recordId") { type = NavType.LongType },
                navArgument("beanId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            BrewEditScreen(navController = navController, recordId = recordId, beanId = beanId, brewViewModel = brewViewModel, beanViewModel = beanViewModel, methodViewModel = brewMethodViewModel, equipmentViewModel = equipmentViewModel, grinderViewModel = grinderViewModel)
        }

        composable(Screen.BrewMethodList.route) {
            BrewMethodListScreen(navController = navController, viewModel = brewMethodViewModel)
        }

        composable(
            route = Screen.BrewMethodEdit.route,
            arguments = listOf(navArgument("methodId") { type = NavType.LongType })
        ) { backStackEntry ->
            val methodId = backStackEntry.arguments?.getLong("methodId") ?: -1L
            BrewMethodEditScreen(navController = navController, methodId = methodId)
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("searchScope") { type = NavType.StringType })
        ) { backStackEntry ->
            val searchScope = backStackEntry.arguments?.getString("searchScope") ?: "all"
            SearchScreen(navController = navController, searchScope = searchScope)
        }

        composable(
            route = Screen.Stats.route,
            arguments = listOf(navArgument("beanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            StatsScreen(navController = navController, beanId = beanId, viewModel = statsViewModel)
        }

        composable(Screen.Backup.route) {
            BackupScreen(navController = navController)
        }

        composable(Screen.EquipmentManagement.route) {
            EquipmentManagementScreen(navController = navController, equipmentViewModel = equipmentViewModel)
        }

        composable(Screen.GrinderManagement.route) {
            GrinderManagementScreen(navController = navController, grinderViewModel = grinderViewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.ArchiveList.route) {
            ArchiveListScreen(navController = navController, viewModel = beanViewModel)
        }

        composable(Screen.About.route) {
            AboutScreen()
        }
    }
}
