package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
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
    arrowSize: Dp = RetroSelectionArrowSize,
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = if (enabled) RetroActionInk else RetroActionInk.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = key, fontFamily = fontFamily, fontSize = 18.sp, color = Color.White)
            RetroSelectionArrow(tint = Color.White, size = arrowSize)
            Text(text = label, fontFamily = fontFamily, fontSize = 18.sp, color = Color.White)
        }
    }
}
