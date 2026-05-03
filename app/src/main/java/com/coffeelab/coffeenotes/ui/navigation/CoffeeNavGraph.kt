package com.coffeelab.coffeenotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.coffeelab.coffeenotes.ui.screen.*

@Composable
fun CoffeeNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.BeanList.route) {
            BeanListScreen(navController = navController)
        }

        composable(
            route = Screen.BeanDetail.route,
            arguments = listOf(navArgument("beanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            BeanDetailScreen(navController = navController, beanId = beanId)
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
            BrewListScreen(navController = navController, beanId = beanId)
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
            BrewEditScreen(navController = navController, recordId = recordId, beanId = beanId)
        }

        composable(Screen.RecipeList.route) {
            RecipeListScreen(navController = navController)
        }

        composable(
            route = Screen.RecipeEdit.route,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: -1L
            RecipeEditScreen(navController = navController, recipeId = recipeId)
        }

        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }

        composable(
            route = Screen.Stats.route,
            arguments = listOf(navArgument("beanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val beanId = backStackEntry.arguments?.getLong("beanId") ?: -1L
            StatsScreen(navController = navController, beanId = beanId)
        }

        composable(Screen.Backup.route) {
            BackupScreen(navController = navController)
        }

        composable(Screen.Import.route) {
            ImportScreen(navController = navController)
        }

        composable(Screen.EquipmentManagement.route) {
            EquipmentManagementScreen(navController = navController)
        }

        composable(Screen.GrinderManagement.route) {
            GrinderManagementScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
