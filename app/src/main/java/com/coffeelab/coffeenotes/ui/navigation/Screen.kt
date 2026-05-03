package com.coffeelab.coffeenotes.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object BeanList : Screen("bean_list")
    object BeanDetail : Screen("bean_detail/{beanId}") {
        fun createRoute(beanId: Long) = "bean_detail/$beanId"
    }
    object BeanEdit : Screen("bean_edit/{beanId}") {
        fun createRoute(beanId: Long = -1L) = "bean_edit/$beanId"
    }
    object Camera : Screen("camera/{beanId}/{mode}") {
        fun createRoute(beanId: Long = -1L, mode: String = "keyword") = "camera/$beanId/$mode"
    }
    object BrewList : Screen("brew_list/{beanId}") {
        fun createRoute(beanId: Long = -1L) = "brew_list/$beanId"
    }
    object BrewEdit : Screen("brew_edit/{recordId}/{beanId}") {
        fun createRoute(recordId: Long = -1L, beanId: Long = -1L) =
            "brew_edit/$recordId/$beanId"
    }
    object RecipeList : Screen("recipe_list")
    object RecipeEdit : Screen("recipe_edit/{recipeId}") {
        fun createRoute(recipeId: Long = -1L) = "recipe_edit/$recipeId"
    }
    object Search : Screen("search")
    object Stats : Screen("stats/{beanId}") {
        fun createRoute(beanId: Long = -1L) = "stats/$beanId"
    }
    object Backup : Screen("backup")
    object Import : Screen("import")
    object EquipmentManagement : Screen("equipment_management")
    object Settings : Screen("settings")
}
