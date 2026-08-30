package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.SharedCharacterArchive

@Composable
internal fun ShareCharacterDialog(
    characterId: String,
    characterName: String,
    onDismiss: () -> Unit,
    viewModel: CharacterSharingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var creator by remember(characterId) { mutableStateOf("") }
    var license by remember(characterId) { mutableStateOf("") }
    val fileName = stringResource(R.string.shared_character_file_name, characterName)
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SharedCharacterArchive.MimeType)
    ) { destination ->
        if (destination != null) {
            viewModel.export(context, characterId, creator, license, destination)
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_character_title, characterName)) },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(
                    value = creator,
                    onValueChange = { creator = it },
                    label = { Text(stringResource(R.string.creator_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = license,
                    onValueChange = { license = it },
                    label = { Text(stringResource(R.string.license_label)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = creator.isNotBlank() && license.isNotBlank(),
                onClick = {
                    createDocument.launch(fileName)
                }
            ) { Text(stringResource(R.string.export_character)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
