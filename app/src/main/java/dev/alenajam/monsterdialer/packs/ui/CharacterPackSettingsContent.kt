package dev.alenajam.monsterdialer.packs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.packs.data.CharacterPackImportDiagnostic
import dev.alenajam.monsterdialer.packs.data.CharacterPackArchive
import dev.alenajam.monsterdialer.packs.data.MonsterPack
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.CharacterPackSettingsContent(
    viewModel: CharacterPackSettingsViewModel = hiltViewModel(),
    showImportUi: Boolean = true,
) {
    val context = LocalContext.current
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val canCreatePack by viewModel.canCreatePack.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val characterPackRemoved = stringResource(R.string.character_pack_removed)
    val characterPackRemoveFailed = stringResource(R.string.character_pack_remove_failed)
    var pendingDeletion by remember { mutableStateOf<MonsterPack?>(null) }
    var isPendingDeletionInUse by remember { mutableStateOf(false) }
    var pendingDisable by remember { mutableStateOf<MonsterPack?>(null) }
    var selectedPack by remember { mutableStateOf<MonsterPack?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.previewPack(context, uri)
    }

    val importPack = { picker.launch(CharacterPackArchive.importMimeTypes) }
    val navigator = LocalSettingsSubpageNavigator.current
    val createPack = { navigator?.navigateTo(0); Unit }
    
    if (showImportUi) {
        CharacterPackImportHandler(viewModel)
    }

    pendingDeletion?.let { pack ->
        CharacterPackDeletionConfirmationDialog(
            packName = pack.name,
            isInUse = isPendingDeletionInUse,
            onConfirm = {
                viewModel.deletePack(pack.id, characterPackRemoved, characterPackRemoveFailed)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }

    pendingDisable?.let { pack ->
        CharacterPackDisableConfirmationDialog(
            packName = pack.name,
            onConfirm = {
                viewModel.togglePack(pack.id, false)
                pendingDisable = null
            },
            onDismiss = { pendingDisable = null }
        )
    }

    selectedPack?.let { pack ->
        CharacterPackDetailsSheet(
            pack = pack,
            onDismiss = { selectedPack = null }
        )
    }

    message?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            viewModel.dismissMessage()
        }
    }
    
    if (packs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppIcon(LocalMonsterAppIcons.current.importPacks, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
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
                if (canCreatePack) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(onClick = importPack) { Text(stringResource(R.string.import_character_pack)) }
                        OutlinedButton(onClick = createPack) { Text(stringResource(R.string.create_character_pack)) }
                    }
                } else {
                    Button(onClick = importPack) { Text(stringResource(R.string.import_character_pack)) }
                }
                Text(
                    stringResource(R.string.pack_import_license_notice),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = importPack,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIcon(LocalMonsterAppIcons.current.importPacks, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.import_pack), modifier = Modifier.padding(start = 8.dp))
            }
            if (canCreatePack) {
                OutlinedButton(
                    onClick = createPack,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.create_character_pack)) }
            }
            Column {
                packs.forEachIndexed { index, pack ->
                    val preview = viewModel.getPreviewCharacter(pack.id, pack.name)
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
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
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = { selectedPack = pack },
                                        onLongClick = { showMenu = true }
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (preview != null) {
                                        AsyncImage(
                                            model = preview.imageFile(
                                                requireNotNull(
                                                    preview.character.visualVariants.firstOrNull()?.frontImage
                                                        ?: preview.character.visualVariants.firstOrNull()?.backImage
                                                )
                                            ),
                                            contentDescription = stringResource(
                                                R.string.character_artwork,
                                                preview.character.name
                                            ),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(pack.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            stringResource(R.string.pack_version, pack.version),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = pack.enabled,
                                        onCheckedChange = { enabled ->
                                            if (!enabled) {
                                                scope.launch {
                                                    if (viewModel.isPackInUse(pack.id)) {
                                                        pendingDisable = pack
                                                    } else {
                                                        viewModel.togglePack(pack.id, false)
                                                    }
                                                }
                                            } else {
                                                viewModel.togglePack(pack.id, true)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove)) },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        isPendingDeletionInUse = viewModel.isPackInUse(pack.id)
                                        pendingDeletion = pack
                                    }
                                },
                                leadingIcon = {
                                    AppIcon(
                                        LocalAppIcons.current.delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterPackImportHandler(viewModel: CharacterPackSettingsViewModel) {
    val preview by viewModel.importPreview.collectAsStateWithLifecycle()
    val importDiagnostic by viewModel.importDiagnostic.collectAsStateWithLifecycle()
    preview?.let { CharacterPackImportPreviewDialog(it, viewModel::importPreview, viewModel::dismissPreview) }
    importDiagnostic?.let { diagnostic ->
        CharacterPackImportFailureDialog(
            diagnostic = diagnostic,
            onDismiss = viewModel::dismissDiagnostic
        )
    }
}

@Composable
private fun CharacterPackImportPreviewDialog(
    preview: CharacterPackSettingsViewModel.ImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val artwork = remember(preview.pack.previewImage) {
        preview.pack.previewImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_character_import_title, preview.pack.manifest.name)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                artwork?.let { Image(it, contentDescription = stringResource(R.string.character_artwork, preview.pack.manifest.name), modifier = Modifier.size(72.dp)) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                preview.pack.manifest.creator?.let { creator ->
                    Text(stringResource(R.string.pack_creator_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(creator, style = MaterialTheme.typography.bodyLarge)
                }
                Text(stringResource(R.string.pack_license_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(preview.pack.manifest.license, style = MaterialTheme.typography.bodyLarge)
                Text(
                    pluralStringResource(
                        R.plurals.pack_character_count,
                        preview.pack.manifest.characters.size,
                        preview.pack.manifest.characters.size,
                        preview.pack.manifest.version,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_pack)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CharacterPackDetailsSheet(
    pack: MonsterPack,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val metadataLabel = stringResource(R.string.pack_metadata)
    val version = stringResource(R.string.pack_version, pack.version)
    val creatorLabel = stringResource(R.string.pack_creator_label)
    val licenseLabel = stringResource(R.string.pack_license_label)
    val identifierLabel = stringResource(R.string.pack_identifier_label)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pack.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = {
                        context.copyToClipboard(
                            label = metadataLabel,
                            text = pack.metadataText(
                                version = version,
                                creatorLabel = creatorLabel,
                                licenseLabel = licenseLabel,
                                identifierLabel = identifierLabel
                            )
                        )
                        Toast.makeText(context, R.string.pack_metadata_copied, Toast.LENGTH_SHORT).show()
                    }) {
                        AppIcon(
                            LocalAppIcons.current.copy,
                            contentDescription = stringResource(R.string.copy_pack_metadata),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    version,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            pack.creator?.takeIf { it.isNotBlank() }?.let { creator ->
                PackMetadataField(
                    label = creatorLabel,
                    value = creator
                )
            }
            PackMetadataField(
                label = licenseLabel,
                value = pack.license,
                valueStyle = MaterialTheme.typography.bodySmall
            )
            PackMetadataField(
                label = identifierLabel,
                value = pack.id,
                valueStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PackMetadataField(
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = valueStyle
        )
    }
}

private fun MonsterPack.metadataText(
    version: String,
    creatorLabel: String,
    licenseLabel: String,
    identifierLabel: String
): String = buildString {
    appendLine(name)
    appendLine(version)
    creator?.takeIf { it.isNotBlank() }?.let { appendLine("$creatorLabel: $it") }
    appendLine("$licenseLabel: $license")
    append("$identifierLabel: $id")
}

@Composable
private fun CharacterPackDeletionConfirmationDialog(
    packName: String,
    isInUse: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_character_pack_title, packName)) },
        text = { 
            Text(
                stringResource(
                    if (isInUse) R.string.remove_character_pack_in_use_message
                    else R.string.remove_character_pack_message, 
                    packName
                )
            ) 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun CharacterPackDisableConfirmationDialog(
    packName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.disable_character_pack_title, packName)) },
        text = { Text(stringResource(R.string.disable_character_pack_message, packName)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.disable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun CharacterPackImportFailureDialog(
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
                AppIcon(
                    LocalAppIcons.current.copy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(stringResource(if (copied) R.string.copied else R.string.copy_report), modifier = Modifier.padding(start = 8.dp))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { context.shareImportDiagnostic(diagnostic.report) }) {
                    AppIcon(
                        LocalAppIcons.current.share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
