package dev.alenajam.monsterdialer.packs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroMenuWindow
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.packs.data.RemotePackCatalogPack

private val CatalogPixelFont = FontFamily(Font(R.font.ui_pixel_font))

@Composable
fun ColumnScope.CatalogsScreen(viewModel: CatalogsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val catalogUrlLabel = stringResource(R.string.catalog_url_label)
    var addDialogOpen by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    var pendingInstall by remember { mutableStateOf<RemotePackCatalogPack?>(null) }
    var selectedPack by remember { mutableStateOf<RemotePackCatalogPack?>(null) }

    LaunchedEffect(state.sources, state.loads) {
        if (selectedPack == null) {
            selectedPack = state.sources.asSequence()
                .mapNotNull { state.loads[it.url] as? CatalogLoad.Content }
                .mapNotNull { it.catalog.packs.firstOrNull() }
                .firstOrNull()
        }
    }

    val actionMessage = when (state.action) {
        CatalogAction.Added -> stringResource(R.string.catalog_added)
        CatalogAction.AddFailed -> stringResource(R.string.catalog_add_failed)
        CatalogAction.Removed -> stringResource(R.string.catalog_removed)
        CatalogAction.RemoveFailed -> stringResource(R.string.catalog_remove_failed)
        CatalogAction.Installed -> stringResource(R.string.catalog_pack_installed)
        is CatalogAction.InstallFailed -> null
        null -> null
    }
    LaunchedEffect(state.action) {
        when (state.action) {
            CatalogAction.Added, CatalogAction.AddFailed -> addDialogOpen = false
            CatalogAction.Installed, is CatalogAction.InstallFailed -> pendingInstall = null
            else -> Unit
        }
    }
    actionMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.dismissAction()
        }
    }
    (state.action as? CatalogAction.InstallFailed)?.let { failure ->
        val reason = failure.reason?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.catalog_pack_install_failed_unknown_reason)
        AlertDialog(
            onDismissRequest = viewModel::dismissAction,
            title = { Text(stringResource(R.string.catalog_pack_install_failed)) },
            text = {
                Text(stringResource(R.string.catalog_pack_install_failed_message, reason))
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAction) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (addDialogOpen) {
        AddCatalogDialog(
            isAdding = state.isAddingCatalog,
            onAdd = viewModel::add,
            onDismiss = { if (!state.isAddingCatalog) addDialogOpen = false },
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetroMenuWindow {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.catalogs_description))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFF202020))
                    RetroSelectableRow(selected = true, onClick = { addDialogOpen = true }) {
                        Text(stringResource(R.string.add_catalog).uppercase(Locale.ROOT), fontFamily = CatalogPixelFont, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp))
                    }
                }
            }
            if (state.sources.isEmpty()) {
                RetroMenuWindow { Text(stringResource(R.string.catalogs_empty)) }
            }
            state.sources.forEach { source ->
                val load = state.loads[source.url]
                CatalogSourceContent(
                    load = load,
                    selectedPack = selectedPack,
                    installingPackId = state.installingPackId,
                    onRefresh = { viewModel.refresh(source.url) },
                    onRemove = { pendingRemoval = source.url },
                    onCopy = {
                        context.copyToClipboard(catalogUrlLabel, source.url)
                        Toast.makeText(context, R.string.catalog_url_copied, Toast.LENGTH_SHORT).show()
                    },
                    onInstall = {
                        selectedPack = it
                        pendingInstall = it
                    },
                )
            }
        }

        pendingRemoval?.let { url ->
            RetroConfirmationDialog(
                title = stringResource(R.string.catalog_remove_confirmation_title),
                noLabel = stringResource(R.string.cancel),
                yesLabel = stringResource(R.string.remove),
                fontFamily = CatalogPixelFont,
                onConfirm = { viewModel.remove(url); pendingRemoval = null },
                onDismissRequest = { pendingRemoval = null },
            )
        }
        pendingInstall?.let { pack ->
            RetroConfirmationDialog(
                title = stringResource(R.string.catalog_install_confirmation_title),
                message = stringResource(R.string.catalog_install_confirmation_message, pack.name, pack.license),
                noLabel = stringResource(R.string.cancel),
                yesLabel = stringResource(R.string.import_pack),
                fontFamily = CatalogPixelFont,
                confirmEnabled = state.installingPackId == null,
                confirmLoading = state.installingPackId == pack.id,
                onConfirm = { viewModel.install(pack) },
                onDismissRequest = { if (state.installingPackId == null) pendingInstall = null },
            )
        }
    }
}

@Composable
private fun CatalogSourceContent(
    load: CatalogLoad?,
    selectedPack: RemotePackCatalogPack?,
    installingPackId: String?,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
    onInstall: (RemotePackCatalogPack) -> Unit,
) {
    val isLoading = load == null || load == CatalogLoad.Loading
    val hasLoadedPacks = (load as? CatalogLoad.Content)?.catalog?.packs?.isNotEmpty() == true
    RetroMenuWindow {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            when (load) {
                null, CatalogLoad.Loading -> Text(stringResource(R.string.catalog_loading))
                CatalogLoad.Failed -> Text(stringResource(R.string.catalog_load_failed))
                is CatalogLoad.Content -> Text(load.catalog.name, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF202020))
            Column {
                RetroSelectableRow(selected = false, onClick = onCopy) {
                    Text(stringResource(R.string.copy_catalog_url).uppercase(Locale.ROOT), fontFamily = CatalogPixelFont, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp))
                }
                RetroSelectableRow(selected = false, onClick = onRefresh) {
                    Text(stringResource(R.string.refresh_catalog).uppercase(Locale.ROOT), fontFamily = CatalogPixelFont, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp))
                }
                RetroSelectableRow(selected = false, onClick = onRemove) {
                    Text(stringResource(R.string.remove).uppercase(Locale.ROOT), fontFamily = CatalogPixelFont, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp))
                }
            }
            if (isLoading || hasLoadedPacks) {
                HorizontalDivider(thickness = 1.dp, color = Color(0xFF202020))
            }
            when (load) {
                null, CatalogLoad.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp).size(20.dp),
                    strokeWidth = 2.dp,
                )
                CatalogLoad.Failed -> Unit
                is CatalogLoad.Content -> load.catalog.packs.forEachIndexed { index, pack ->
                    if (index > 0) HorizontalDivider(thickness = 1.dp, color = Color(0xFF202020))
                    RetroSelectableRow(
                        selected = selectedPack == pack,
                        enabled = installingPackId == null,
                        onClick = { onInstall(pack) },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(pack.name.uppercase(Locale.ROOT), fontFamily = CatalogPixelFont, fontSize = 18.sp)
                            val attribution = pack.creator?.takeIf { it.isNotBlank() }
                                ?: load.catalog.publisher?.takeIf { it.isNotBlank() }
                            Text(
                                attribution?.let { stringResource(R.string.catalog_pack_metadata, it, pack.version) }
                                    ?: pack.version,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCatalogDialog(
    isAdding: Boolean,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_catalog)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.catalog_add_notice))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.catalog_url_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !isAdding, onClick = { onAdd(url) }) {
                if (isAdding) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.add))
                }
            }
        },
        dismissButton = { TextButton(enabled = !isAdding, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
