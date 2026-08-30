package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons

@Composable
fun CharactersHomeScreen(onOpenSubpage: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CharacterHomeItem(R.string.settings_player_character_title, R.string.settings_player_character_description, LocalAppIcons.current.person) { onOpenSubpage(0) }
        CharacterHomeItem(R.string.settings_contact_characters_title, R.string.settings_contact_characters_description, LocalAppIcons.current.history) { onOpenSubpage(1) }
        CharacterHomeItem(R.string.settings_character_packs_title, R.string.settings_character_packs_description, LocalAppIcons.current.edit) { onOpenSubpage(2) }
    }
}

@Composable
private fun CharacterHomeItem(title: Int, description: Int, icon: dev.alenajam.opendialer.core.common.ui.IconSource, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
