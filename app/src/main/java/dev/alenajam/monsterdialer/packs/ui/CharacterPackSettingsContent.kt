package dev.alenajam.monsterdialer.packs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.packs.data.CharacterPackImportDiagnostic
import dev.alenajam.monsterdialer.packs.data.MonsterPack
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import dev.alenajam.opendialer.feature.settings.LocalSettingsRootNavigator
import androidx.compose.ui.text.font.Font

private val PackPixelFont = FontFamily(Font(R.font.ui_pixel_font))

@Composable
fun ColumnScope.CharacterPackSettingsContent(
    viewModel: CharacterPackSettingsViewModel = hiltViewModel(),
    showImportUi: Boolean = true,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val canCreatePack by viewModel.canCreatePack.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val characterPackRemoved = stringResource(R.string.character_pack_removed)
    val characterPackRemoveFailed = stringResource(R.string.character_pack_remove_failed)
    var pendingDeletion by remember { mutableStateOf<MonsterPack?>(null) }
    var pendingDisable by remember { mutableStateOf<MonsterPack?>(null) }
    var selectedPack by remember { mutableStateOf<MonsterPack?>(null) }
    var detailsPack by remember { mutableStateOf<MonsterPack?>(null) }
    var optionsOpen by remember { mutableStateOf(false) }
    var cursorPackId by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(packs) {
        if (packs.none { it.id == cursorPackId }) {
            cursorPackId = packs.firstOrNull()?.id
        }
    }

    val navigator = LocalSettingsSubpageNavigator.current
    val rootNavigator = LocalSettingsRootNavigator.current
    val createPack = { navigator?.navigateTo("create-pack"); Unit }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
    if (showImportUi) {
        CharacterPackImportHandler(viewModel)
    }

    pendingDeletion?.let { pack ->
        CharacterPackDeletionConfirmationDialog(
            onConfirm = {
                viewModel.deletePack(pack.id, characterPackRemoved, characterPackRemoveFailed)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }

    pendingDisable?.let { pack ->
        CharacterPackDisableConfirmationDialog(
            onConfirm = {
                viewModel.togglePack(pack.id, false)
                pendingDisable = null
            },
            onDismiss = { pendingDisable = null }
        )
    }

    detailsPack?.let { pack ->
        CharacterPackDetailsSheet(
            pack = pack,
            onDismiss = { detailsPack = null }
        )
    }

    message?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            viewModel.dismissMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PackOptionsButton(onClick = { optionsOpen = true })

        if (packs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppIcon(LocalMonsterAppIcons.current.importCharacter, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
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
            Column {
                packs.forEach { pack ->
                    val preview = viewModel.getPreviewCharacter(pack.id, pack.name)
                    RetroSelectableRow(
                        selected = pack.id == cursorPackId,
                        onClick = {
                            cursorPackId = pack.id
                            selectedPack = pack
                        },
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
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
                                        preview.character.name,
                                    ),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f).padding(start = if (preview == null) 0.dp else 6.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = pack.name.uppercase(),
                                    fontFamily = PackPixelFont,
                                    fontSize = 18.sp,
                                    color = Color(0xFF202020),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = buildString {
                                        if (!pack.enabled) {
                                            append(stringResource(R.string.disabled).uppercase())
                                            append(" · ")
                                        }
                                        append(
                                            pluralStringResource(
                                                R.plurals.pack_character_count_only,
                                                pack.characterCount,
                                                pack.characterCount,
                                            ),
                                        )
                                    },
                                    fontFamily = PackPixelFont,
                                    fontSize = 13.sp,
                                    color = Color(0xFF202020).copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    selectedPack?.let { pack ->
        RetroContextMenuOverlay(
            modifier = Modifier.fillMaxWidth(0.82f),
            fontFamily = PackPixelFont,
            onDismissRequest = { selectedPack = null },
            items = listOf(
                    RetroContextMenuItem(stringResource(R.string.pack_details_action)) {
                        selectedPack = null
                        detailsPack = pack
                    },
                    RetroContextMenuItem(
                        label = stringResource(if (pack.enabled) R.string.disable else R.string.enable),
                    ) {
                        selectedPack = null
                        if (pack.enabled) {
                            scope.launch {
                                if (viewModel.isPackInUse(pack.id)) pendingDisable = pack
                                else viewModel.togglePack(pack.id, false)
                            }
                        } else {
                            viewModel.togglePack(pack.id, true)
                        }
                    },
                    RetroContextMenuItem(stringResource(R.string.remove)) {
                        selectedPack = null
                        pendingDeletion = pack
                    },
                    RetroContextMenuItem.cancel(stringResource(R.string.cancel), onClick = { selectedPack = null }),
            ),
        )
    }

    if (optionsOpen) {
        RetroContextMenuOverlay(
            modifier = Modifier.fillMaxWidth(0.82f),
            fontFamily = PackPixelFont,
            onDismissRequest = { optionsOpen = false },
            items = buildList {
                    add(
                        RetroContextMenuItem(stringResource(R.string.import_action)) {
                            optionsOpen = false
                            onImport()
                        },
                    )
                    if (canCreatePack) {
                        add(
                            RetroContextMenuItem(stringResource(R.string.create_character_pack)) {
                                optionsOpen = false
                                createPack()
                            },
                        )
                    }
                    add(
                        RetroContextMenuItem(stringResource(R.string.settings_catalogs_title)) {
                            optionsOpen = false
                            rootNavigator?.invoke("catalogs", null)
                        },
                    )
                    add(RetroContextMenuItem.cancel(stringResource(R.string.cancel), onClick = { optionsOpen = false }))
            },
        )
    }

        }
    }
}

@Composable
private fun PackOptionsButton(onClick: () -> Unit) {
    RetroSearchButton(
        label = stringResource(R.string.contact_picker_options),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    )
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    RetroConfirmationDialog(
        title = stringResource(R.string.are_you_sure),
        noLabel = stringResource(R.string.cancel),
        yesLabel = stringResource(R.string.remove),
        fontFamily = PackPixelFont,
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun CharacterPackDisableConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    RetroConfirmationDialog(
        title = stringResource(R.string.are_you_sure),
        noLabel = stringResource(R.string.cancel),
        yesLabel = stringResource(R.string.disable),
        fontFamily = PackPixelFont,
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
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
