package dev.alenajam.monsterdialer.ui

import java.io.File
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ColumnScope.PlayerCharacterSettingsContent() {
    val context = LocalContext.current
    val root = remember(context.filesDir) { File(context.filesDir, "character-packs") }
    val assignments = remember(root) { CharacterAssignmentStore(root) }
    val repository = remember(root) { CharacterPackRepository(root) }
    val characters = remember(root) { repository.charactersAssignableTo(CharacterAssignmentTarget.Player) }
    var selected by remember { mutableStateOf(assignments.player()) }
    val scope = rememberCoroutineScope()

    if (characters.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Person, contentDescription = null)
                    Text(
                        "Import and enable a pack containing a player-assignable character first.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    characters.forEach { installed ->
        val reference = CharacterReference(installed.packId, installed.character.id)
        Card(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                AsyncImage(model = installed.imageFile(installed.character.frontImage), contentDescription = installed.character.name)
                Text(installed.character.name, style = MaterialTheme.typography.titleMedium)
                Text(installed.packName, style = MaterialTheme.typography.bodySmall)
                if (selected == reference) {
                    Text("Selected", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Button(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { assignments.setPlayer(reference) }
                            selected = reference
                        }
                    }) { Text("Use as player") }
                }
            }
        }
    }
    if (selected != null) {
        TextButton(onClick = {
            scope.launch {
                withContext(Dispatchers.IO) { assignments.setPlayer(null) }
                selected = null
            }
        }) { Text("Use default player") }
    }
}
