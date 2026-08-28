package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.opendialer.feature.contacts.ContactPickerScreen
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

@Composable
fun ColumnScope.ContactCharacterSettingsContent(
    viewModel: ContactCharacterSettingsViewModel = hiltViewModel()
) {
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val trainers = viewModel.trainers
    val monsters = viewModel.monsters
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val trainerTitle = stringResource(R.string.character_type_trainer)
    val monsterTitle = stringResource(R.string.character_type_monster)

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
        CharacterTypeTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> characterTypeItems(
                    title = trainerTitle,
                    defaultCharacter = BuiltInCharacters.trainer,
                    characters = emptyList(),
                    selected = null,
                    defaultArtwork = { it.contactArtwork },
                    packArtwork = { it.imageFile(requireNotNull(it.character.frontImage)) },
                    onSelect = {}
                )
                1 -> characterTypeItems(
                    title = monsterTitle,
                    defaultCharacter = BuiltInCharacters.monster.character,
                    characters = emptyList(),
                    selected = null,
                    defaultArtwork = { it.contactArtwork },
                    packArtwork = { it.imageFile(requireNotNull(it.character.frontImage)) },
                    onSelect = {}
                )
            }
        }
        return
    }

    val currentContact = contact
    if (currentContact == null) {
        ContactChooser(
            hasCharacters = true,
            onChooseContact = { navigator?.navigateTo(0) }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    Text(currentContact.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        currentContact.numbers.joinToString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { navigator?.navigateTo(0) }) { Text(stringResource(R.string.change)) }
            }
        }

        CharacterTypeTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
        when (selectedTab) {
            0 -> characterTypeItems(
                title = trainerTitle,
                defaultCharacter = BuiltInCharacters.trainer,
                characters = trainers,
                selected = assignedTrainer,
                defaultArtwork = { it.contactArtwork },
                packArtwork = { it.imageFile(requireNotNull(it.character.frontImage)) },
                onSelect = viewModel::assignTrainer
            )
            1 -> characterTypeItems(
                title = monsterTitle,
                defaultCharacter = BuiltInCharacters.monster.character,
                characters = monsters,
                selected = assignedMonster,
                defaultArtwork = { it.contactArtwork },
                packArtwork = { it.imageFile(requireNotNull(it.character.frontImage)) },
                onSelect = viewModel::assignMonster
            )
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
