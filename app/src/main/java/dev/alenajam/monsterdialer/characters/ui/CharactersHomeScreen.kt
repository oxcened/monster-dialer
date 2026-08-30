package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.IconSource
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons

@Composable
fun CharactersHomeScreen(
    onOpenSubpage: (Int) -> Unit,
    sharingViewModel: CharacterSharingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val preview by sharingViewModel.preview.collectAsStateWithLifecycle()
    val hasImportError by sharingViewModel.hasImportError.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) sharingViewModel.preview(context, uri)
    }

    preview?.let { shared ->
        AlertDialog(
            onDismissRequest = sharingViewModel::dismissPreview,
            title = { Text(shared.character.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.shared_character_creator, shared.character.creator))
                    Text(stringResource(R.string.shared_character_license, shared.character.license))
                    Text(stringResource(R.string.shared_character_import_description))
                }
            },
            confirmButton = {
                TextButton(onClick = sharingViewModel::importPreview) {
                    Text(stringResource(R.string.add_to_your_characters))
                }
            },
            dismissButton = {
                TextButton(onClick = sharingViewModel::dismissPreview) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (hasImportError) {
        AlertDialog(
            onDismissRequest = sharingViewModel::dismissImportError,
            title = { Text(stringResource(R.string.shared_character_import_failed_title)) },
            text = { Text(stringResource(R.string.shared_character_import_failed_message)) },
            confirmButton = {
                TextButton(onClick = sharingViewModel::dismissImportError) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        CharacterHomeItem(R.string.settings_player_character_title, R.string.settings_player_character_description, LocalAppIcons.current.person, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 2.dp, bottomEnd = 2.dp)) { onOpenSubpage(0) }
        CharacterHomeItem(R.string.settings_contact_characters_title, R.string.settings_contact_characters_description, LocalAppIcons.current.history, RoundedCornerShape(2.dp)) { onOpenSubpage(1) }
        CharacterHomeItem(R.string.settings_character_packs_title, R.string.settings_character_packs_description, LocalAppIcons.current.edit, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 20.dp, bottomEnd = 20.dp)) { onOpenSubpage(2) }
        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) { Text(stringResource(R.string.import_character)) }
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
