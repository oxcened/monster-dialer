package dev.alenajam.monsterdialer.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.BuiltInCharacter
import dev.alenajam.monsterdialer.characters.BuiltInCharacters
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import dev.alenajam.opendialer.feature.contacts.ContactPickerScreen
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

@Composable
fun ColumnScope.ContactCharacterSettingsContent(
    viewModel: ContactCharacterSettingsViewModel = hiltViewModel()
) {
    val contact = viewModel.contact
    val assignedTrainer = viewModel.assignedTrainer
    val assignedMonster = viewModel.assignedMonster
    val trainers = viewModel.trainers
    val monsters = viewModel.monsters

    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalSettingsSubpageNavigator.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.restoreSelectedContact()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (trainers.isEmpty() && monsters.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ContactCharacterTypeSection(
                title = stringResource(R.string.character_type_trainer),
                defaultCharacter = BuiltInCharacters.trainer,
                characters = emptyList(),
                selected = null,
                onSelect = {}
            )
            ContactCharacterTypeSection(
                title = stringResource(R.string.character_type_monster),
                defaultCharacter = BuiltInCharacters.monster.character,
                characters = emptyList(),
                selected = null,
                onSelect = {}
            )
        }
        return
    }

    if (contact == null) {
        ContactChooser(
            hasCharacters = true,
            onChooseContact = { navigator?.navigateTo(0) }
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        contact.numbers.joinToString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { navigator?.navigateTo(0) }) { Text(stringResource(R.string.change)) }
            }
        }

        ContactCharacterTypeSection(
            title = stringResource(R.string.character_type_trainer),
            defaultCharacter = BuiltInCharacters.trainer,
            characters = trainers,
            selected = assignedTrainer,
            onSelect = viewModel::assignTrainer
        )
        ContactCharacterTypeSection(
            title = stringResource(R.string.character_type_monster),
            defaultCharacter = BuiltInCharacters.monster.character,
            characters = monsters,
            selected = assignedMonster,
            onSelect = viewModel::assignMonster
        )
    }
}

@Composable
private fun ContactCharacterTypeSection(
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
                        painter = painterResource(defaultCharacter.contactArtwork.resource),
                        contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                        modifier = Modifier.size(72.dp)
                    )
                },
                onSelect = { onSelect(null) }
            )
            characters.forEachIndexed { index, item ->
                val reference = CharacterReference(item.packId, item.character.id)
                CharacterOptionCard(
                    name = item.character.name,
                    subtitle = item.packName,
                    isRadiant = item.character.isRadiant,
                    isSelected = availableSelection == reference,
                    roundTop = false,
                    roundBottom = index == characters.lastIndex,
                    artwork = {
                        AsyncImage(
                            model = item.imageFile(requireNotNull(item.character.frontImage)),
                            contentDescription = stringResource(R.string.character_artwork, item.character.name),
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
private fun ContactChooser(hasCharacters: Boolean, onChooseContact: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.contact_chooser_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (hasCharacters) {
                        stringResource(R.string.contact_chooser_prompt)
                    } else {
                        stringResource(R.string.contact_chooser_empty_prompt)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasCharacters) {
                Button(onClick = onChooseContact) {
                    Text(stringResource(R.string.choose_contact))
                }
            }
        }
    }
}

@Composable
fun ContactPickerDestination(
    onNavigateBack: () -> Unit,
    viewModel: ContactCharacterSettingsViewModel = hiltViewModel()
) {
    ContactPickerScreen(
        onNavigateBack = onNavigateBack,
        onContactSelected = { selectedContact ->
            viewModel.onContactSelected(selectedContact)
            onNavigateBack()
        }
    )
}
