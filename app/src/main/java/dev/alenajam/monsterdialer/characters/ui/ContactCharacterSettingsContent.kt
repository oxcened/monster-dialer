package dev.alenajam.monsterdialer.characters.ui

import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import dev.alenajam.monsterdialer.characters.data.ContactCharacterMode
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.contacts.ui.formatPhoneNumber
import dev.alenajam.opendialer.core.common.ui.ContactAvatar
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.feature.contacts.ContactPickerScreen
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import kotlinx.coroutines.launch

@Composable
fun ColumnScope.ContactCharacterSettingsContent(
    entryPoint: ContactCharacterSettingsEntryPoint = ContactCharacterSettingsEntryPoint.Toolbox,
    viewModel: ContactCharacterSettingsViewModel = hiltViewModel()
) {
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val trainerMode by viewModel.trainerMode.collectAsStateWithLifecycle()
    val monsterMode by viewModel.monsterMode.collectAsStateWithLifecycle()
    val contactSelectionVersion by viewModel.contactSelectionVersion.collectAsStateWithLifecycle()
    val trainers by viewModel.trainers.collectAsStateWithLifecycle()
    val monsters by viewModel.monsters.collectAsStateWithLifecycle()
    val isLimitReached by viewModel.isLimitReached.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val unlockedVariants by viewModel.unlockedVariants.collectAsStateWithLifecycle()
    val pendingOnlineProfileId by viewModel.pendingOnlineProfileId.collectAsStateWithLifecycle()
    val trainerSelectedItemIndex = selectedCharacterIndex(trainers, assignedTrainer)
    val monsterSelectedItemIndex = selectedCharacterIndex(monsters, assignedMonster)
    val trainerListState = rememberLazyListState(
        initialFirstVisibleItemIndex = trainerSelectedItemIndex
    )
    val monsterListState = rememberLazyListState(
        initialFirstVisibleItemIndex = monsterSelectedItemIndex
    )
    val trainerGridState = rememberLazyGridState(initialFirstVisibleItemIndex = trainerSelectedItemIndex)
    val monsterGridState = rememberLazyGridState(initialFirstVisibleItemIndex = monsterSelectedItemIndex)
    val trainerTitle = stringResource(R.string.character_type_trainer)
    val monsterTitle = stringResource(R.string.character_type_monster)
    val trainersTitle = stringResource(R.string.character_type_trainers)
    val monstersTitle = stringResource(R.string.character_type_monsters)
    val addTrainerLabel = stringResource(R.string.add_trainer)
    val addMonsterLabel = stringResource(R.string.add_monster)

    val locale = LocalConfiguration.current.locales[0]

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

    val currentContact = contact
    if (currentContact == null) {
        if (pendingOnlineProfileId != null) {
            Text(
                text = stringResource(R.string.online_profile_choose_contact_prompt),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ContactChooser(
            hasCharacters = true,
            onChooseContact = { navigator?.navigateTo(0) }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (entryPoint == ContactCharacterSettingsEntryPoint.Toolbox) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContactAvatar(
                        name = currentContact.name,
                        photoUri = currentContact.photoUri,
                        modifier = Modifier.size(24.dp),
                        initialTextStyle = MaterialTheme.typography.labelLarge
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentContact.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            currentContact.numbers.firstOrNull()?.let { formatPhoneNumber(it, locale) }
                                ?: stringResource(R.string.unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { navigator?.navigateTo(0) }) { Text(stringResource(R.string.change)) }
                }
            }
        }

        CharacterTypeTabs(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                val selectedItemIndex = if (tab == 0) trainerSelectedItemIndex else monsterSelectedItemIndex
                val nextTabHasCharacters = if (tab == 0) trainers.isNotEmpty() else monsters.isNotEmpty()
                val nextTabEffectiveLayout = if (nextTabHasCharacters) layout else CharacterLayout.List

                if (nextTabEffectiveLayout == CharacterLayout.List) (if (tab == 0) trainerListState else monsterListState).requestScrollToItem(selectedItemIndex)
                else (if (tab == 0) trainerGridState else monsterGridState).requestScrollToItem(selectedItemIndex)
                viewModel.setSelectedTab(tab)
            }
        )
        val selectedItemIndex = when (selectedTab) {
            0 -> trainerSelectedItemIndex
            else -> monsterSelectedItemIndex
        }
        val currentTabHasCharacters = if (selectedTab == 0) trainers.isNotEmpty() else monsters.isNotEmpty()
        val effectiveLayout = if (currentTabHasCharacters) layout else CharacterLayout.List

        val listState = if (selectedTab == 0) trainerListState else monsterListState
        val gridState = if (selectedTab == 0) trainerGridState else monsterGridState
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var pendingDeletion by remember { mutableStateOf<InstalledPackCharacter?>(null) }
        var isPendingDeletionInUse by remember { mutableStateOf(false) }
        var pendingShare by remember { mutableStateOf<InstalledPackCharacter?>(null) }

        pendingDeletion?.let { character ->
            CustomCharacterDeletionConfirmationDialog(
                characterName = character.character.name,
                hasRadiantVariant = character.character.hasRadiantVariant,
                isInUse = isPendingDeletionInUse,
                onConfirm = {
                    viewModel.deleteCustomCharacter(character.character.id)
                    pendingDeletion = null
                },
                onDismiss = { pendingDeletion = null }
            )
        }
        pendingShare?.let { character ->
            ShareCharacterDialog(
                characterId = character.character.id,
                characterName = character.character.name,
                onDismiss = { pendingShare = null }
            )
        }

        LaunchedEffect(contactSelectionVersion) {
            val selectedItemIndex = if (selectedTab == 0) trainerSelectedItemIndex else monsterSelectedItemIndex
            if (effectiveLayout == CharacterLayout.List) listState.requestScrollToItem(selectedItemIndex)
            else gridState.requestScrollToItem(selectedItemIndex)
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (effectiveLayout == CharacterLayout.List) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp)) {
                    when (selectedTab) {
                        0 -> characterTypeItems(
                            title = trainerTitle,
                            pluralTitle = trainersTitle,
                            defaultCharacter = BuiltInCharacters.trainer,
                            characters = trainers,
                            selected = assignedTrainer,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                viewModel.assignTrainer(it)
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onAddCharacter = { navigator?.navigateTo(1) },
                            addLabel = addTrainerLabel,
                            isAddEnabled = !isLimitReached,
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = trainerMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeTrainer()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(1, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                        1 -> characterTypeItems(
                            title = monsterTitle,
                            pluralTitle = monstersTitle,
                            defaultCharacter = BuiltInCharacters.monster.character,
                            characters = monsters,
                            selected = assignedMonster,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                viewModel.assignMonster(it)
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onAddCharacter = { navigator?.navigateTo(2) },
                            addLabel = addMonsterLabel,
                            isAddEnabled = !isLimitReached,
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = monsterMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeMonster()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(2, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when (selectedTab) {
                        0 -> characterTypeGridItems(
                            title = trainerTitle,
                            pluralTitle = trainersTitle,
                            defaultCharacter = BuiltInCharacters.trainer,
                            characters = trainers,
                            selected = assignedTrainer,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                viewModel.assignTrainer(it)
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onAddCharacter = { navigator?.navigateTo(1) },
                            addLabel = addTrainerLabel,
                            isAddEnabled = !isLimitReached,
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = trainerMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeTrainer()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(1, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                        1 -> characterTypeGridItems(
                            title = monsterTitle,
                            pluralTitle = monstersTitle,
                            defaultCharacter = BuiltInCharacters.monster.character,
                            characters = monsters,
                            selected = assignedMonster,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                viewModel.assignMonster(it)
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onAddCharacter = { navigator?.navigateTo(2) },
                            addLabel = addMonsterLabel,
                            isAddEnabled = !isLimitReached,
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = monsterMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeMonster()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(2, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                    }
                }
            }
            if (currentTabHasCharacters) {
                CharacterLayoutToggle(
                    layout,
                    onLayoutChanged = { nextLayout ->
                        val firstVisibleItemIndex = if (layout == CharacterLayout.List) listState.firstVisibleItemIndex else gridState.firstVisibleItemIndex
                        if (nextLayout == CharacterLayout.List) listState.requestScrollToItem(firstVisibleItemIndex)
                        else gridState.requestScrollToItem(firstVisibleItemIndex)
                    viewModel.setLayout(nextLayout)
                    },
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                )
                if (effectiveLayout == CharacterLayout.List) JumpToSelectedCharacterButton(listState, selectedItemIndex, Modifier.align(Alignment.BottomEnd).padding(16.dp))
                else JumpToSelectedCharacterButton(gridState, selectedItemIndex, Modifier.align(Alignment.BottomEnd).padding(16.dp))
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
            AppIcon(
                LocalAppIcons.current.person,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
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
    val context = LocalContext.current
    val contactPhoneNumberRequiredMessage = stringResource(R.string.contact_phone_number_required)
    ContactPickerScreen(
        onNavigateBack = onNavigateBack,
        onContactSelected = { selectedContact ->
            viewModel.onContactSelected(
                selectedContact = selectedContact,
                onSelected = onNavigateBack,
                onRejected = {
                    Toast.makeText(context, contactPhoneNumberRequiredMessage, Toast.LENGTH_SHORT).show()
                },
            )
        }
    )
}
