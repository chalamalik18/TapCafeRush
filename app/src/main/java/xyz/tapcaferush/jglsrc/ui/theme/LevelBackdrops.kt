package xyz.tapcaferush.jglsrc.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object LevelBackdrops {
    fun verticalBrush(levelId: Int): Brush {
        val c = colorsFor(levelId)
        return Brush.verticalGradient(colors = c)
    }

    fun horizontalBrush(levelId: Int): Brush {
        val c = colorsFor(levelId)
        return Brush.horizontalGradient(colors = c)
    }

    fun colorsFor(levelId: Int): List<Color> = when (levelId) {
        1 -> listOf(
            Color(0xFFFFFDE7),
            Color(0xFFFFE0B2),
            Color(0xFFFFB74D)
        )
        2 -> listOf(
            Color(0xFFE8F5E9),
            Color(0xFFC8E6C9),
            Color(0xFF66BB6A)
        )
        3 -> listOf(
            Color(0xFFECEFF1),
            Color(0xFFCFD8DC),
            Color(0xFFFFB74D)
        )
        4 -> listOf(
            Color(0xFFFFE0B2),
            Color(0xFFFF8A65),
            Color(0xFF5E35B1)
        )
        5 -> listOf(
            Color(0xFF0D1B2A),
            Color(0xFF1B263B),
            Color(0xFF006064)
        )
        else -> listOf(
            Color(0xFF12002F),
            Color(0xFF283593),
            Color(0xFF26C6DA)
        )
    }

    fun accentFor(levelId: Int): Color = when (levelId) {
        1 -> Color(0xFFE65100)
        2 -> Color(0xFF2E7D32)
        3 -> Color(0xFF37474F)
        4 -> Color(0xFFD84315)
        5 -> Color(0xFF00E5FF)
        else -> Color(0xFF7CFFCB)
    }
}
