package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
internal data class RetroContextMenuItem(
    val label: String,
    val showCursor: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
internal fun RetroContextMenu(
    items: List<RetroContextMenuItem>,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    title: String? = null,
    height: androidx.compose.ui.unit.Dp = 132.dp,
) {
    RetroBoxFrame(modifier = modifier, height = height) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            title?.let {
                Text(
                    text = it.uppercase(),
                    fontFamily = fontFamily,
                    fontSize = 18.sp,
                    color = Color(0xFF202020),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            items.forEach { item ->
                RetroContextMenuItemRow(item = item, fontFamily = fontFamily)
            }
        }
    }
}

@Composable
private fun RetroContextMenuItemRow(
    item: RetroContextMenuItem,
    fontFamily: FontFamily,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(onClick = item.onClick)
            .semantics { contentDescription = item.label },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.showCursor) {
            RetroSelectionArrow(
                modifier = Modifier.padding(start = 4.dp),
                tint = Color(0xFF202020),
            )
        } else {
            Spacer(modifier = Modifier.size(RetroSelectionArrowSize + 4.dp))
        }
        Text(
            text = item.label.uppercase(),
            fontFamily = fontFamily,
            fontSize = 18.sp,
            color = Color(0xFF202020),
        )
    }
}
