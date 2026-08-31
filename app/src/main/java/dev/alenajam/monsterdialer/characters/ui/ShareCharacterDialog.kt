package dev.alenajam.monsterdialer.characters.ui

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val sharedFile by viewModel.sharedFile.collectAsStateWithLifecycle()
    var creator by remember(characterId) { mutableStateOf("") }
    val defaultLicense = stringResource(R.string.default_license)
    var license by remember(characterId, defaultLicense) { mutableStateOf(defaultLicense) }
    val fileName = stringResource(R.string.shared_character_file_name, characterName)
    val shareTitle = stringResource(R.string.share_character_title, characterName)
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SharedCharacterArchive.MimeType)
    ) { destination ->
        if (destination != null) {
            viewModel.export(context, characterId, creator, license, destination)
        }
        onDismiss()
    }

    LaunchedEffect(sharedFile) {
        sharedFile?.let { file ->
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = SharedCharacterArchive.MimeType
                        putExtra(Intent.EXTRA_STREAM, file)
                        clipData = ClipData.newRawUri(fileName, file)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    shareTitle,
                ),
            )
            viewModel.consumeSharedFile()
            onDismiss()
        }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = creator.isNotBlank() && license.isNotBlank(),
                    onClick = {
                        viewModel.exportForSharing(context, characterId, creator, license, fileName)
                    },
                ) { Text(stringResource(R.string.share)) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(
                        enabled = creator.isNotBlank() && license.isNotBlank(),
                        onClick = { createDocument.launch(fileName) },
                    ) { Text(stringResource(R.string.save_to_device)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        },
    )
}
