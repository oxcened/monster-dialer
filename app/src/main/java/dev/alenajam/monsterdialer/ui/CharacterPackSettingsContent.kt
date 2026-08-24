package dev.alenajam.monsterdialer.ui

import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.packs.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.CharacterPackInstaller
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

    Text("Character Packs", style = MaterialTheme.typography.titleMedium)
    Text(
        "Import only artwork and sounds you created or are licensed to use.",
        style = MaterialTheme.typography.bodyMedium
    )
    Button(onClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }) {
        Text("Import character pack")
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    packs.forEach { pack ->
        Card(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                Text(pack.name, style = MaterialTheme.typography.titleMedium)
                Text("${pack.characterCount} characters · v${pack.version}")
                Text(pack.license, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { catalog.setEnabled(pack.id, !pack.enabled) }
                            packs = catalog.list()
                        }
                    }) { Text(if (pack.enabled) "Disable" else "Enable") }
                    TextButton(onClick = {
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
