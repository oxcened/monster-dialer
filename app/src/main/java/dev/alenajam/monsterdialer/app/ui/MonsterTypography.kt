package dev.alenajam.monsterdialer.app.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.alenajam.monsterdialer.R

@Composable
fun rememberMonsterTypography(base: Typography): Typography {
    val customFont = FontFamily(Font(R.font.pixel_operator))
    return base.withFontFamily(customFont).scaledBy(1.3f)
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = fontFamily),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = fontFamily),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = fontFamily),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily),
)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private fun Typography.scaledBy(factor: Float): Typography = copy(
    displayLarge = displayLarge.copy(fontSize = displayLarge.fontSize * factor),
    displayMedium = displayMedium.copy(fontSize = displayMedium.fontSize * factor),
    displaySmall = displaySmall.copy(fontSize = displaySmall.fontSize * factor),
    headlineLarge = headlineLarge.copy(fontSize = headlineLarge.fontSize * factor),
    headlineMedium = headlineMedium.copy(fontSize = headlineMedium.fontSize * factor),
    headlineSmall = headlineSmall.copy(fontSize = headlineSmall.fontSize * factor),
    titleLarge = titleLarge.copy(fontSize = titleLarge.fontSize * factor),
    titleMedium = titleMedium.copy(fontSize = titleMedium.fontSize * factor),
    titleSmall = titleSmall.copy(fontSize = titleSmall.fontSize * factor),
    bodyLarge = bodyLarge.copy(fontSize = bodyLarge.fontSize * factor),
    bodyMedium = bodyMedium.copy(fontSize = bodyMedium.fontSize * factor),
    bodySmall = bodySmall.copy(fontSize = bodySmall.fontSize * factor),
    labelLarge = labelLarge.copy(fontSize = labelLarge.fontSize * factor),
    labelMedium = labelMedium.copy(fontSize = labelMedium.fontSize * factor),
    labelSmall = labelSmall.copy(fontSize = labelSmall.fontSize * factor),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontSize = displayLargeEmphasized.fontSize * factor),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontSize = displayMediumEmphasized.fontSize * factor),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontSize = displaySmallEmphasized.fontSize * factor),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontSize = headlineLargeEmphasized.fontSize * factor),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontSize = headlineMediumEmphasized.fontSize * factor),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontSize = headlineSmallEmphasized.fontSize * factor),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontSize = titleLargeEmphasized.fontSize * factor),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontSize = titleMediumEmphasized.fontSize * factor),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontSize = titleSmallEmphasized.fontSize * factor),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontSize = bodyLargeEmphasized.fontSize * factor),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontSize = bodyMediumEmphasized.fontSize * factor),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontSize = bodySmallEmphasized.fontSize * factor),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontSize = labelLargeEmphasized.fontSize * factor),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontSize = labelMediumEmphasized.fontSize * factor),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontSize = labelSmallEmphasized.fontSize * factor),
)
