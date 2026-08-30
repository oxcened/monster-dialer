package dev.alenajam.monsterdialer.characters.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.characters.data.SharedCharacterImport
import dev.alenajam.monsterdialer.packs.data.CharacterType
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

    preview?.let { shared -> SharedCharacterImportDialog(shared, sharingViewModel::importPreview, sharingViewModel::dismissPreview) }
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
        CharacterHomeItem(R.string.settings_character_packs_title, R.string.settings_character_packs_description, LocalAppIcons.current.edit, RoundedCornerShape(2.dp)) { onOpenSubpage(2) }
        CharacterHomeItem(R.string.import_character, R.string.import_character_description, LocalMonsterAppIcons.current.addCharacter, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 20.dp, bottomEnd = 20.dp)) { picker.launch(arrayOf("*/*")) }
    }
}

@Composable
private fun SharedCharacterImportDialog(
    shared: SharedCharacterImport,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val characterType = stringResource(
        if (shared.character.type == CharacterType.Trainer) R.string.character_type_trainer
        else R.string.character_type_monster
    )
    val characterCollection = stringResource(
        if (shared.character.type == CharacterType.Trainer) R.string.character_type_trainers
        else R.string.character_type_monsters
    )
    val artwork = remember(shared.frontImage, shared.backImage) {
        (shared.frontImage ?: shared.backImage)?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    stringResource(R.string.shared_character_import_title, shared.character.name),
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (artwork != null) {
                        Image(artwork, contentDescription = stringResource(R.string.character_artwork, shared.character.name), modifier = Modifier.size(72.dp))
                    } else {
                        AppIcon(LocalMonsterAppIcons.current.addCharacter, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.creator_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shared.character.creator, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.license_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shared.character.license, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text(
                    stringResource(R.string.shared_character_import_description, characterType, characterCollection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = onConfirm) { Text(stringResource(R.string.add_to_your_characters)) }
                }
            }
        }
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
