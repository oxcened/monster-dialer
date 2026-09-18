package dev.alenajam.monsterdialer.app.ui

import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RetroActionPixelFont = FontFamily(Font(dev.alenajam.monsterdialer.R.font.pixel_operator))
private val RetroActionInk = Color(0xFF202020)

@Composable
internal fun RetroActionButton(
    key: String,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = RetroActionPixelFont,
) {
    val displayedLabel = label.uppercase(Locale.ROOT)

    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PixelActionSurface(
            enabled = enabled,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = key, fontFamily = fontFamily, fontSize = 18.sp, color = Color.White)
                RetroSelectionArrow(tint = Color.White, size = 14.dp)
                Text(text = displayedLabel, fontFamily = fontFamily, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

/** Black command key with the stepped corners used by the reference's pixel UI. */
@Composable
private fun PixelActionSurface(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.drawBehind {
            val step = 4.dp.toPx()
            val ink = if (enabled) RetroActionInk else RetroActionInk.copy(alpha = 0.35f)
            val path = Path().apply {
                // Two square steps create a compact pixelated curve.
                moveTo(2f * step, 0f)
                lineTo(size.width - 2f * step, 0f)
                lineTo(size.width - 2f * step, step)
                lineTo(size.width - step, step)
                lineTo(size.width - step, 2f * step)
                lineTo(size.width, 2f * step)
                lineTo(size.width, size.height - 2f * step)
                lineTo(size.width - step, size.height - 2f * step)
                lineTo(size.width - step, size.height - step)
                lineTo(size.width - 2f * step, size.height - step)
                lineTo(size.width - 2f * step, size.height)
                lineTo(2f * step, size.height)
                lineTo(2f * step, size.height - step)
                lineTo(step, size.height - step)
                lineTo(step, size.height - 2f * step)
                lineTo(0f, size.height - 2f * step)
                lineTo(0f, 2f * step)
                lineTo(step, 2f * step)
                lineTo(step, step)
                lineTo(2f * step, step)
                close()
            }
            drawPath(path, ink)
        },
    ) {
        content()
    }
}
