package dev.alenajam.monsterdialer.ui

import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.packs.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.CharacterPackInstaller
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ColumnScope.CharacterPackSettingsContent() {
    val context = LocalContext.current
    val root = remember(context.filesDir) { File(context.filesDir, "character-packs") }
    val catalog = remember(root) { CharacterPackCatalog(root) }
    val installer = remember(root) { CharacterPackInstaller(root, catalog = catalog) }
    var packs by remember { mutableStateOf(catalog.list()) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(context.contentResolver.openInputStream(uri)) { "Unable to open selected file" }
                        .use(installer::install)
                }
            }
            packs = result.fold(onSuccess = { catalog.list() }, onFailure = { packs })
            message = result.fold(
                onSuccess = { "Character pack imported" },
                onFailure = { error -> error.message ?: "Could not import character pack" }
            )
        }
    }

    val importPack = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
    if (packs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Your collection starts here", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    "Import a character pack to personalize your call experience.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = importPack) { Text("Import character pack") }
                Text(
                    "Only import artwork and sounds you created or are licensed to use.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val repository = remember(root) { CharacterPackRepository(root) }
        val previews = remember(packs) {
            packs.associate { pack ->
                pack.id to repository.charactersInPack(pack.id, pack.name).firstOrNull()
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Your character packs", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${packs.size} installed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = importPack) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Import", modifier = Modifier.padding(start = 8.dp))
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
                    val preview = previews[pack.id]
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
                                        model = preview.imageFile(preview.character.frontImage),
                                        contentDescription = "${preview.character.name} artwork",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(pack.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${pack.characterCount} character${if (pack.characterCount == 1) "" else "s"} · v${pack.version}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    if (pack.enabled) "Enabled" else "Disabled",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (pack.enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                pack.license,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { catalog.setEnabled(pack.id, !pack.enabled) }
                                        packs = catalog.list()
                                    }
                                }) { Text(if (pack.enabled) "Disable" else "Enable") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                catalog.remove(pack.id)
                                                File(root, pack.id).deleteRecursively()
                                            }
                                        }
                                        packs = catalog.list()
                                        message = result.fold(
                                            onSuccess = { "Character pack removed" },
                                            onFailure = { it.message ?: "Could not remove character pack" }
                                        )
                                    }
                                }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }
}
