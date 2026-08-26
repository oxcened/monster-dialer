package dev.alenajam.monsterdialer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.alenajam.monsterdialer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.characters.BuiltInCharacter
import dev.alenajam.monsterdialer.characters.BuiltInCharacters
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ColumnScope.PlayerCharacterSettingsContent() {
    val context = LocalContext.current
    val root = remember(context.filesDir) { File(context.filesDir, "character-packs") }
    val assignments = remember(root) { CharacterAssignmentStore(root) }
    val repository = remember(root) { CharacterPackRepository(root) }
    val trainers = remember(root) {
        repository.charactersAssignableTo(CharacterAssignmentTarget.Player, CharacterType.Trainer)
    }
    val monsters = remember(root) {
        repository.charactersAssignableTo(CharacterAssignmentTarget.Player, CharacterType.Monster)
    }
    var selectedTrainer by remember { mutableStateOf(assignments.player(CharacterType.Trainer)) }
    var selectedMonster by remember { mutableStateOf(assignments.player(CharacterType.Monster)) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CharacterTypeSection(
            title = stringResource(R.string.character_type_trainer),
            defaultCharacter = BuiltInCharacters.trainer,
            characters = trainers,
            selected = selectedTrainer,
            onSelect = { reference ->
                scope.launch {
                    withContext(Dispatchers.IO) { assignments.setPlayer(CharacterType.Trainer, reference) }
                    selectedTrainer = reference
                }
            }
        )
        CharacterTypeSection(
            title = stringResource(R.string.character_type_monster),
            defaultCharacter = BuiltInCharacters.monster.character,
            characters = monsters,
            selected = selectedMonster,
            onSelect = { reference ->
                scope.launch {
                    withContext(Dispatchers.IO) { assignments.setPlayer(CharacterType.Monster, reference) }
                    selectedMonster = reference
                }
            }
        )
    }
}

@Composable
private fun CharacterTypeSection(
    title: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    onSelect: (CharacterReference?) -> Unit
) {
    val availableSelection = selected?.takeIf { reference ->
        characters.any { CharacterReference(it.packId, it.character.id) == reference }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Column {
            CharacterOptionCard(
                name = defaultCharacter.name,
                subtitle = stringResource(R.string.built_in_character),
                isSelected = availableSelection == null,
                roundTop = true,
                roundBottom = false,
                artwork = {
                    Image(
                        painter = painterResource(defaultCharacter.playerArtwork.resource),
                        contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                        modifier = Modifier.size(72.dp)
                    )
                },
                onSelect = { onSelect(null) }
            )
            characters.forEachIndexed { index, installed ->
                val reference = CharacterReference(installed.packId, installed.character.id)
                CharacterOptionCard(
                    name = installed.character.name,
                    subtitle = installed.packName,
                    isRadiant = installed.character.isRadiant,
                    isSelected = availableSelection == reference,
                    roundTop = false,
                    roundBottom = index == characters.lastIndex,
                    artwork = {
                        AsyncImage(
                            model = installed.imageFile(requireNotNull(installed.character.backImage)),
                            contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                            modifier = Modifier.size(72.dp)
                        )
                    },
                    onSelect = { onSelect(reference) }
                )
            }
            if (characters.isEmpty()) {
                NoAdditionalCharacterOptionsCard(title)
            }
        }
    }
}

@Composable
internal fun NoAdditionalCharacterOptionsCard(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        shape = RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 2.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.no_other_character_options, title.lowercase()),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.import_and_enable_character_pack),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun CharacterOptionCard(
    name: String,
    subtitle: String,
    isRadiant: Boolean = false,
    isSelected: Boolean,
    roundTop: Boolean,
    roundBottom: Boolean,
    artwork: @Composable () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        shape = RoundedCornerShape(
            topStart = if (roundTop) 20.dp else 2.dp,
            topEnd = if (roundTop) 20.dp else 2.dp,
            bottomStart = if (roundBottom) 20.dp else 2.dp,
            bottomEnd = if (roundBottom) 20.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            artwork()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    if (isRadiant) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = stringResource(R.string.radiant),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Text(
                        stringResource(R.string.selected),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!isSelected) {
                OutlinedButton(onClick = onSelect) { Text(stringResource(R.string.select)) }
            }
        }
    }
}
