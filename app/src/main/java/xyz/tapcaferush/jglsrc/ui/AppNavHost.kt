package xyz.tapcaferush.jglsrc.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.tapcaferush.jglsrc.game.CafeLevel
import xyz.tapcaferush.jglsrc.navigation.Routes
import xyz.tapcaferush.jglsrc.ui.screens.GameScreen
import xyz.tapcaferush.jglsrc.ui.screens.HomeScreen
import xyz.tapcaferush.jglsrc.ui.screens.LevelSelectScreen
import xyz.tapcaferush.jglsrc.ui.screens.SettingsScreen
import xyz.tapcaferush.jglsrc.ui.screens.StatsScreen

@Composable
fun AppNavHost(nav: NavHostController) {
    NavHost(
        navController = nav,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.LEVELS) { LevelSelectScreen(nav) }
        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("levelId") { type = NavType.IntType }
            )
        ) { entry ->
            val raw = entry.arguments?.getInt("levelId") ?: 1
            val levelId = raw.coerceIn(1, CafeLevel.ALL.last().id)
            GameScreen(nav, levelId)
        }
        composable(Routes.STATS) { StatsScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
    }
}
