package com.kma.foodhub.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF003733),
    primaryContainer = BrandDark,
    onPrimaryContainer = Color.White,
    secondary = Warning,
    tertiary = SoftGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFF4F5FA),
    onSurface = Color(0xFFF4F5FA),
    onSurfaceVariant = Color(0xFFB9CECA),
    outline = Color(0xFF89A39F),
    outlineVariant = Color(0xFF385653),
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = BrandDark,
    secondary = Secondary,
    onSecondary = WarningDark,
    secondaryContainer = WarmAccent,
    onSecondaryContainer = WarningDark,
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = SoftGreen,
    onTertiaryContainer = SuccessDark,
    background = AppBackground,
    surface = CeramicSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = CaptionBrown,
    outlineVariant = OutlineVariant,
    onBackground = Neutral,
    onSurface = Neutral,
    error = Color(0xFFB3261E),
    surfaceTint = Color.Transparent,
)

private val FoodHubShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun FoodHubAppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // FoodHub has a deliberate brand palette; dynamic colors would replace it
    // with wallpaper colors on Android 12+.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FoodHubShapes,
        content = content,
    )
}
