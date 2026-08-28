package dev.alenajam.monsterdialer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.packs.CharacterPackImportDiagnostic

@Composable
fun ColumnScope.CharacterPackSettingsContent(
    viewModel: CharacterPackSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val importDiagnostic by viewModel.importDiagnostic.collectAsStateWithLifecycle()
    
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importPack(context, uri)
    }

    val importPack = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
    
    importDiagnostic?.let { diagnostic ->
        CharacterPackImportFailureDialog(
            diagnostic = diagnostic,
            onDismiss = viewModel::dismissDiagnostic
        )
    }
    
    if (packs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.pack_collection_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(
                        stringResource(R.string.pack_collection_empty_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = importPack) { Text(stringResource(R.string.import_character_pack)) }
                Text(
                    stringResource(R.string.pack_import_license_notice),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val characterPackRemoved = stringResource(R.string.character_pack_removed)
        val characterPackRemoveFailed = stringResource(R.string.character_pack_remove_failed)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.your_character_packs), style = MaterialTheme.typography.titleMedium)
                    Text(
                        pluralStringResource(R.plurals.installed_pack_count, packs.size, packs.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = importPack) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.import_pack), modifier = Modifier.padding(start = 8.dp))
                }
            }
            message?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                packs.forEachIndexed { index, pack ->
                    val preview = viewModel.getPreviewCharacter(pack.id)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        shape = RoundedCornerShape(
                            topStart = if (index == 0) 20.dp else 2.dp,
                            topEnd = if (index == 0) 20.dp else 2.dp,
                            bottomStart = if (index == packs.lastIndex) 20.dp else 2.dp,
                            bottomEnd = if (index == packs.lastIndex) 20.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (preview != null) {
                                    AsyncImage(
                                        model = preview.imageFile(
                                            preview.character.frontImage ?: requireNotNull(preview.character.backImage)
                                        ),
                                        contentDescription = stringResource(R.string.character_artwork, preview.character.name),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(pack.name, style = MaterialTheme.typography.titleMedium)
                                    // In a real app we might want the character count in MonsterPack
                                    Text(
                                        pack.version,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    stringResource(if (pack.enabled) R.string.enabled else R.string.disabled),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (pack.enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            // License info removed from MonsterPack for brevity in this example, but should be there
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    viewModel.togglePack(pack.id, !pack.enabled)
                                }) { Text(stringResource(if (pack.enabled) R.string.disable else R.string.enable)) }
                                OutlinedButton(onClick = {
                                    viewModel.deletePack(pack.id, characterPackRemoved, characterPackRemoveFailed)
                                }) { Text(stringResource(R.string.remove)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterPackImportFailureDialog(
    diagnostic: CharacterPackImportDiagnostic,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val diagnosticLabel = stringResource(R.string.pack_import_diagnostic_label)
    var copied by remember(diagnostic.report) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.pack_import_failed_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(diagnostic.summary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.file_label, diagnostic.fileName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.pack_import_report_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectionContainer {
                    Text(
                        diagnostic.report,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                context.copyToClipboard(diagnosticLabel, diagnostic.report)
                copied = true
            }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(if (copied) R.string.copied else R.string.copy_report), modifier = Modifier.padding(start = 8.dp))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { context.shareImportDiagnostic(diagnostic.report) }) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.share), modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        }
    )
}

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun Context.shareImportDiagnostic(report: String) {
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, report),
            getString(R.string.share_pack_import_diagnostic)
        )
    )
}
