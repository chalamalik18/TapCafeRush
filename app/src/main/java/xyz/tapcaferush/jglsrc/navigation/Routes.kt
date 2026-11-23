package xyz.tapcaferush.jglsrc.navigation

object Routes {
    const val HOME = "home"
    const val LEVELS = "levels"
    const val GAME = "game/{levelId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun game(levelId: Int): String = "game/$levelId"
}
