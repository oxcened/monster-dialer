package dev.alenajam.monsterdialer.packs.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.packs.data.RemotePackCatalogPack

private val CatalogPixelFont = FontFamily(Font(R.font.ui_pixel_font))

@Composable
fun ColumnScope.CatalogsScreen(viewModel: CatalogsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var addDialogOpen by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    var pendingInstall by remember { mutableStateOf<RemotePackCatalogPack?>(null) }

    val actionMessage = when (state.action) {
        CatalogAction.Added -> stringResource(R.string.catalog_added)
        CatalogAction.AddFailed -> stringResource(R.string.catalog_add_failed)
        CatalogAction.Removed -> stringResource(R.string.catalog_removed)
        CatalogAction.RemoveFailed -> stringResource(R.string.catalog_remove_failed)
        CatalogAction.Installed -> stringResource(R.string.catalog_pack_installed)
        CatalogAction.InstallFailed -> stringResource(R.string.catalog_pack_install_failed)
        null -> null
    }
    actionMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.dismissAction()
        }
    }

    if (addDialogOpen) {
        AddCatalogDialog(
            onAdd = { url -> viewModel.add(url); addDialogOpen = false },
            onDismiss = { addDialogOpen = false },
        )
    }
    pendingRemoval?.let { url ->
        RetroConfirmationDialog(
            title = stringResource(R.string.catalog_remove_confirmation_title),
            message = stringResource(R.string.catalog_remove_confirmation_message),
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
            onConfirm = { viewModel.install(pack); pendingInstall = null },
            onDismissRequest = { pendingInstall = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.catalogs_description))
        Button(onClick = { addDialogOpen = true }) { Text(stringResource(R.string.add_catalog)) }
        if (state.sources.isEmpty()) {
            Text(stringResource(R.string.catalogs_empty))
        }
        state.sources.forEach { source ->
            val load = state.loads[source.url]
            CatalogSourceContent(
                sourceUrl = source.url,
                load = load,
                onRefresh = { viewModel.refresh(source.url) },
                onRemove = { pendingRemoval = source.url },
                onInstall = { pendingInstall = it },
            )
        }
    }
}

@Composable
private fun CatalogSourceContent(
    sourceUrl: String,
    load: CatalogLoad?,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    onInstall: (RemotePackCatalogPack) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(sourceUrl, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.refresh_catalog)) }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.remove_catalog)) }
        }
        when (load) {
            null, CatalogLoad.Loading -> Text(stringResource(R.string.catalog_loading))
            CatalogLoad.Failed -> Text(stringResource(R.string.catalog_load_failed))
            is CatalogLoad.Content -> {
                Text(load.catalog.name)
                load.catalog.packs.forEach { pack ->
                    RetroSelectableRow(selected = false, onClick = { onInstall(pack) }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(pack.name)
                            Text(stringResource(R.string.catalog_pack_metadata, pack.creator ?: load.catalog.publisher.orEmpty(), pack.version))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCatalogDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
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
        confirmButton = { TextButton(onClick = { onAdd(url) }) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
