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
    /** Null uses the menu's default cursor position; true/false explicitly overrides it. */
    val showCursor: Boolean? = null,
    val onClick: () -> Unit,
)

@Composable
internal fun RetroContextMenu(
    items: List<RetroContextMenuItem>,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    RetroBoxFrame(modifier = modifier) {
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
            items.forEachIndexed { index, item ->
                RetroContextMenuItemRow(
                    item = item,
                    showCursor = item.showCursor ?: (index == 0),
                    fontFamily = fontFamily,
                )
            }
        }
    }
}

@Composable
internal fun RetroConfirmationMenu(
    title: String,
    message: String? = null,
    noLabel: String,
    yesLabel: String,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    RetroBoxFrame(modifier = modifier, height = 156.dp) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title.uppercase(), fontFamily = fontFamily, fontSize = 18.sp, color = Color(0xFF202020))
            message?.takeIf { it.isNotBlank() }?.let {
                Text(it.uppercase(), fontFamily = fontFamily, fontSize = 18.sp, color = Color(0xFF202020))
            }
            Spacer(modifier = Modifier.height(4.dp))
            ConfirmationActionRow(noLabel, true, fontFamily, onCancel)
            ConfirmationActionRow(yesLabel, false, fontFamily, onConfirm)
        }
    }
}

@Composable
private fun ConfirmationActionRow(
    label: String,
    showCursor: Boolean,
    fontFamily: FontFamily,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCursor) {
            RetroSelectionArrow(
                modifier = Modifier.padding(start = 4.dp),
                tint = Color(0xFF202020),
                size = 14.dp,
            )
        } else {
            Spacer(modifier = Modifier.size(RetroSelectionArrowSize))
        }
        Text(label.uppercase(), fontFamily = fontFamily, fontSize = 18.sp, color = Color(0xFF202020))
    }
}

@Composable
private fun RetroContextMenuItemRow(
    item: RetroContextMenuItem,
    showCursor: Boolean,
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
        if (showCursor) {
            RetroSelectionArrow(
                modifier = Modifier.padding(start = 4.dp),
                tint = Color(0xFF202020),
                size = 14.dp
            )
        } else {
            Spacer(modifier = Modifier.size(RetroSelectionArrowSize))
        }
        Text(
            text = item.label.uppercase(),
            fontFamily = fontFamily,
            fontSize = 18.sp,
            color = Color(0xFF202020),
        )
    }
}
