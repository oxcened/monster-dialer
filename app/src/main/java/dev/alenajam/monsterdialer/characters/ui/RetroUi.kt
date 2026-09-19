package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R

internal val RetroInk = Color(0xFF202020)
internal val RetroPaper = Color(0xFFF7F7F2)
internal val RetroLavender = Color(0xFFE5E5DA)
internal val RetroCyan = Color(0xFFD8F2E8)
internal val RetroOrange = Color(0xFFF0D48A)
internal val RetroPink = Color(0xFFF0B5D8)

@Composable
internal fun RetroPanel(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .border(2.dp, RetroInk, RectangleShape)
            .background(if (selected) RetroLavender else RetroPaper, RectangleShape)
            .padding(10.dp),
    ) {
        content()
    }
}

@Composable
internal fun RetroActionLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.retro_action_select),
            style = MaterialTheme.typography.labelMedium,
            color = RetroInk,
        )
        Text(
            text = stringResource(R.string.retro_action_back),
            style = MaterialTheme.typography.labelMedium,
            color = RetroInk,
        )
    }
}
