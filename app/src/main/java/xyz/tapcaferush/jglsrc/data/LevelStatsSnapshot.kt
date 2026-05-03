package xyz.tapcaferush.jglsrc.data

data class LevelStatsSnapshot(
    val shiftsPlayed: Int = 0,
    val bestScore: Int = 0,
    val totalServed: Int = 0,
    val bestCombo: Int = 0,
    val bestRushScore: Int = 0,
    val correctTaps: Int = 0,
    val wrongTaps: Int = 0,
    val expiredOrders: Int = 0
) {
    val accuracyPercent: Float
        get() {
            val denom = correctTaps + wrongTaps
            return if (denom == 0) 0f else (100f * correctTaps / denom)
        }
}
