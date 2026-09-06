package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared four-layer Game Boy box frame. */
@Composable
internal fun RetroBoxFrame(
    modifier: Modifier = Modifier,
    height: Dp,
    content: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .background(Color.Black)
            .padding(2.dp)
            .background(Color.White)
            .padding(3.dp)
            .background(Color.Black)
            .padding(4.dp)
            .background(Color.White)
            .padding(2.dp),
        content = content,
    )
}
