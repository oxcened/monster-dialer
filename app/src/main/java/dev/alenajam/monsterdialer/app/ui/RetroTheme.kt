package dev.alenajam.monsterdialer.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.alenajam.opendialer.core.common.ui.rememberAppIsDarkTheme

@Immutable
data class RetroColors(
    val ink: Color,
    val paper: Color,
    val panel: Color,
    val border: Color,
    val mutedInk: Color,
    val accent: Color,
    val accentFace: Color,
    val actionButton: Color,
    val onActionButton: Color,
    val profileBackground: Color,
    val tabBackground: Color,
    val tabTrack: Color,
    val tabHighlight: Color,
    val tabShadow: Color,
    val tabFace: Color,
)

private val LightRetroColors = RetroColors(
    ink = Color(0xFF202020),
    paper = Color(0xFFF7F7F2),
    panel = Color(0xFFF9F7FC),
    border = Color(0xFF202020),
    mutedInk = Color(0xFF686860),
    accent = Color(0xFF5B4D8E),
    accentFace = Color(0xFFE5DDF7),
    actionButton = Color(0xFF202020),
    onActionButton = Color.White,
    profileBackground = Color(0xFF4B376D),
    tabBackground = Color(0xFF9A7BC4),
    tabTrack = Color(0xFF7355A5),
    tabHighlight = Color(0xFFEDE5F7),
    tabShadow = Color(0xFFB8B0C2),
    tabFace = Color(0xFF9A7BC4),
)

private val DarkRetroColors = RetroColors(
    ink = Color(0xFFF2F0E8),
    paper = Color(0xFF1B1B1A),
    panel = Color(0xFF25232A),
    border = Color(0xFFD8D5C8),
    mutedInk = Color(0xFFAAA79A),
    accent = Color(0xFFB9A7E8),
    accentFace = Color(0xFF665589),
    actionButton = Color(0xFF665589),
    onActionButton = Color(0xFFF2F0E8),
    profileBackground = Color(0xFF17151C),
    tabBackground = Color(0xFF25232A),
    tabTrack = Color(0xFF393542),
    tabHighlight = Color(0xFFAAA2B8),
    tabShadow = Color(0xFF514A5E),
    tabFace = Color(0xFF665589),
)

val LocalRetroColors = compositionLocalOf { LightRetroColors }

@Composable
fun RetroTheme(content: @Composable () -> Unit) {
    val darkTheme = rememberAppIsDarkTheme()
    val palette = if (darkTheme) {
        DarkRetroColors
    } else {
        LightRetroColors
    }
    val colors = if (darkTheme) {
        val appBackground = MaterialTheme.colorScheme.background
        palette.copy(
            panel = appBackground,
            profileBackground = appBackground,
            tabBackground = appBackground,
        )
    } else {
        palette
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalRetroColors provides colors) {
        content()
    }
}

object RetroThemeDefaults {
    val colors: RetroColors
        @Composable get() = LocalRetroColors.current
}
