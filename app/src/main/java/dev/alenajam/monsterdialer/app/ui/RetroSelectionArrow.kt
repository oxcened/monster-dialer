package dev.alenajam.monsterdialer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.alenajam.opendialer.core.common.ui.AppIcon

/** Shared Game Boy-style selection cursor with a fixed default size across the app. */
internal val RetroSelectionArrowSize = 18.dp

@Composable
internal fun RetroSelectionArrow(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
    size: Dp = RetroSelectionArrowSize,
) {
    AppIcon(
        icon = LocalMonsterAppIcons.current.selectionCursor,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
