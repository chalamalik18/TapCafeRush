package xyz.tapcaferush.jglsrc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EspressoBrown,
    onPrimary = LatteCream,
    primaryContainer = AmberGlow,
    onPrimaryContainer = EspressoBrown,
    secondary = CoralAccent,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = MintFoam,
    onTertiary = EspressoBrown,
    background = LatteCream,
    onBackground = EspressoBrown,
    surface = SurfaceLight,
    onSurface = EspressoBrown,
    surfaceVariant = Color(0xFFEAD8C8),
    onSurfaceVariant = Color(0xFF4E342E)
)

private val DarkColors = darkColorScheme(
    primary = AmberGlow,
    onPrimary = MochaDeep,
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = LatteCream,
    secondary = CoralAccent,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = MintFoam,
    onTertiary = MochaDeep,
    background = MochaDeep,
    onBackground = LatteCream,
    surface = SurfaceDark,
    onSurface = LatteCream,
    surfaceVariant = Color(0xFF2C2420),
    onSurfaceVariant = Color(0xFFD7CCC8)
)

@Composable
fun TapCafeRushTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
