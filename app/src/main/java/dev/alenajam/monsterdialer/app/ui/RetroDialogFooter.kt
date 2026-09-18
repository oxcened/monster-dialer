package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class RetroFooterAction(
    val key: String,
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/** Shared GSC footer with an optional dialog box followed by bottom actions. */
@Composable
internal fun RetroFooter(
    message: String? = null,
    onBack: () -> Unit,
    backKey: String,
    backLabel: String,
    modifier: Modifier = Modifier,
    animationKey: Any? = message,
    leftAction: RetroFooterAction? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RetroScreenFooterVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        message?.let {
            RetroDoubleBorderTextBox(message = it, animationKey = animationKey)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (leftAction == null) Arrangement.End else Arrangement.SpaceBetween,
        ) {
            leftAction?.let { action ->
                RetroActionButton(
                    key = action.key,
                    label = action.label,
                    enabled = action.enabled,
                    onClick = action.onClick,
                )
            }
            RetroActionButton(
                key = backKey,
                label = backLabel,
                onClick = onBack,
            )
        }
    }
}
