package xyz.tapcaferush.jglsrc.game

data class CafeLevel(
    val id: Int,
    val title: String,
    val themeLabel: String,
    val tagline: String,
    val targetOrders: Int,
    val baseOrderMs: Float,
    val rushIntervalMs: Float,
    val rushDurationMs: Float,
    val rushScoreMult: Float,
    val rushOrderFactor: Float,
    val wrongPenalty: Int,
    val vipChance: Float
) {
    companion object {
        val ALL: List<CafeLevel> = listOf(
            CafeLevel(
                id = 1,
                title = "Morning warm-up",
                themeLabel = "Sunrise patio",
                tagline = "Soft dawn light, gentle tickets, first sips of the day.",
                targetOrders = 12,
                baseOrderMs = 5_400f,
                rushIntervalMs = 42_000f,
                rushDurationMs = 11_000f,
                rushScoreMult = 1.1f,
                rushOrderFactor = 0.68f,
                wrongPenalty = 2,
                vipChance = 0.11f
            ),
            CafeLevel(
                id = 2,
                title = "Brunch buzz",
                themeLabel = "Garden terrace",
                tagline = "Herb-toned rush, leafy calm between waves of guests.",
                targetOrders = 15,
                baseOrderMs = 4_700f,
                rushIntervalMs = 38_000f,
                rushDurationMs = 10_500f,
                rushScoreMult = 1.12f,
                rushOrderFactor = 0.64f,
                wrongPenalty = 3,
                vipChance = 0.13f
            ),
            CafeLevel(
                id = 3,
                title = "Lunch rush",
                themeLabel = "Metro counter",
                tagline = "Concrete-and-steel pace, hot plates, tight lunch window.",
                targetOrders = 18,
                baseOrderMs = 4_100f,
                rushIntervalMs = 33_000f,
                rushDurationMs = 10_000f,
                rushScoreMult = 1.14f,
                rushOrderFactor = 0.58f,
                wrongPenalty = 4,
                vipChance = 0.14f
            ),
            CafeLevel(
                id = 4,
                title = "Sunset sprint",
                themeLabel = "Rooftop glow",
                tagline = "Coral skies, long shadows, orders that burn bright and fast.",
                targetOrders = 22,
                baseOrderMs = 3_500f,
                rushIntervalMs = 28_000f,
                rushDurationMs = 9_200f,
                rushScoreMult = 1.16f,
                rushOrderFactor = 0.54f,
                wrongPenalty = 5,
                vipChance = 0.16f
            ),
            CafeLevel(
                id = 5,
                title = "Midnight rush",
                themeLabel = "Neon alley",
                tagline = "After-hours neon, bass-line energy, zero margin for error.",
                targetOrders = 26,
                baseOrderMs = 2_950f,
                rushIntervalMs = 24_000f,
                rushDurationMs = 8_400f,
                rushScoreMult = 1.18f,
                rushOrderFactor = 0.48f,
                wrongPenalty = 6,
                vipChance = 0.18f
            ),
            CafeLevel(
                id = 6,
                title = "Finale frenzy",
                themeLabel = "Aurora roastery",
                tagline = "Northern lights, packed counter, fastest tickets of the night.",
                targetOrders = 32,
                baseOrderMs = 2_500f,
                rushIntervalMs = 20_000f,
                rushDurationMs = 7_500f,
                rushScoreMult = 1.22f,
                rushOrderFactor = 0.44f,
                wrongPenalty = 8,
                vipChance = 0.22f
            )
        )

        fun fromId(id: Int): CafeLevel = ALL.find { it.id == id } ?: ALL.first()
    }
}
