package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.IconSource
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons

@Composable
fun CharactersHomeScreen(onOpenSubpage: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        CharacterHomeItem(R.string.settings_player_character_title, R.string.settings_player_character_description, LocalAppIcons.current.person, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 2.dp, bottomEnd = 2.dp)) { onOpenSubpage(0) }
        CharacterHomeItem(R.string.settings_contact_characters_title, R.string.settings_contact_characters_description, LocalAppIcons.current.history, RoundedCornerShape(2.dp)) { onOpenSubpage(1) }
        CharacterHomeItem(R.string.settings_character_packs_title, R.string.settings_character_packs_description, LocalAppIcons.current.edit, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 20.dp, bottomEnd = 20.dp)) { onOpenSubpage(2) }
    }
}

@Composable
private fun CharacterHomeItem(title: Int, description: Int, icon: IconSource, shape: RoundedCornerShape, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
