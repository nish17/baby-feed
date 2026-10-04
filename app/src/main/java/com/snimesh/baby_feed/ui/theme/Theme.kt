package com.snimesh.baby_feed.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BabyFeedColorScheme = darkColorScheme(
    primary = AccentColor,
    onPrimary = OnAccentText,
    secondary = SecondaryButtonBackground,
    onSecondary = PrimaryText,
    tertiary = AccentColor,
    onTertiary = OnAccentText,
    background = CardBackground,
    onBackground = PrimaryText,
    surface = CardBackground,
    onSurface = PrimaryText,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = MutedText,
    error = WarningColor,
    onError = OnAccentText,
)

private val BabyFeedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Always the same dark, accent-forward palette the home-screen widget uses -- not reactive to
 * system light/dark or Material You dynamic color, so the app and widget read as one product
 * regardless of the phone's wallpaper or theme settings.
 */
@Composable
fun BabyfeedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BabyFeedColorScheme,
        typography = Typography,
        shapes = BabyFeedShapes,
        content = content,
    )
}
