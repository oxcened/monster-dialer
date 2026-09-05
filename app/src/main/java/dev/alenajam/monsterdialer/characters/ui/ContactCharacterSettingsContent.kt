package dev.alenajam.monsterdialer.characters.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.ContactCharacterMode
import dev.alenajam.monsterdialer.characters.data.ContactCharacterDefaults
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.feature.contacts.ContactPickerScreen
import dev.alenajam.opendialer.feature.settings.LocalSettingsRootNavigator
import dev.alenajam.opendialer.feature.settings.LocalSettingsBackInterceptor
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import kotlinx.coroutines.launch

private enum class ContactAssignmentMode { Global, Custom, Random }

@Composable
fun ColumnScope.ContactCharacterSettingsContent(
    entryPoint: ContactCharacterSettingsEntryPoint = ContactCharacterSettingsEntryPoint.Toolbox,
    viewModel: ContactCharacterSettingsViewModel = hiltViewModel()
) {
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val trainerMode by viewModel.trainerMode.collectAsStateWithLifecycle()
    val trainerUsesGlobalDefaults by viewModel.trainerUsesGlobalDefaults.collectAsStateWithLifecycle()
    val monsterMode by viewModel.monsterMode.collectAsStateWithLifecycle()
    val monsterUsesGlobalDefaults by viewModel.monsterUsesGlobalDefaults.collectAsStateWithLifecycle()
    val contactSelectionVersion by viewModel.contactSelectionVersion.collectAsStateWithLifecycle()
    val trainers by viewModel.trainers.collectAsStateWithLifecycle()
    val monsters by viewModel.monsters.collectAsStateWithLifecycle()
    val isLimitReached by viewModel.isLimitReached.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val unlockedVariants by viewModel.unlockedVariants.collectAsStateWithLifecycle()
    val pendingOnlineProfileId by viewModel.pendingOnlineProfileId.collectAsStateWithLifecycle()
    val contactDefaults by viewModel.contactDefaults.collectAsStateWithLifecycle()
    val contactRandomPools by viewModel.contactRandomPools.collectAsStateWithLifecycle()
    val trainerSelectedItemIndex = selectedCharacterIndex(trainers, assignedTrainer, hasRandomize = true)
    val monsterSelectedItemIndex = selectedCharacterIndex(monsters, assignedMonster, hasRandomize = true)
    val trainerRandomPool = contactRandomPools[CharacterType.Trainer]
        ?: viewModel.selectedContactPool(CharacterType.Trainer, viewModel.allContactPoolReferences(CharacterType.Trainer))
    val monsterRandomPool = contactRandomPools[CharacterType.Monster]
        ?: viewModel.selectedContactPool(CharacterType.Monster, viewModel.allContactPoolReferences(CharacterType.Monster))
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

    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalSettingsSubpageNavigator.current
    val rootNavigator = LocalSettingsRootNavigator.current
    val contactRandomPoolDrafts = remember { mutableStateMapOf<CharacterType, Set<CharacterReference>>() }
    val effectiveTrainerRandomPool = contactRandomPoolDrafts[CharacterType.Trainer] ?: trainerRandomPool
    val effectiveMonsterRandomPool = contactRandomPoolDrafts[CharacterType.Monster] ?: monsterRandomPool
    val hasUnsavedEmptyContactPool = contactRandomPoolDrafts.values.any(Set<CharacterReference>::isEmpty)
    val updateContactPool: (CharacterType, Set<CharacterReference>) -> Unit = { type, pool ->
        contactRandomPoolDrafts[type] = pool
        if (pool.isNotEmpty()) viewModel.setContactSpecificRandomPool(type, pool)
    }
    RandomPoolEditorBackHandling(hasUnsavedEmptyContactPool) { contactRandomPoolDrafts.clear() }

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
    if (entryPoint == ContactCharacterSettingsEntryPoint.Defaults) {
        ContactCharacterDefaultsSection(
            viewModel = viewModel,
            trainers = trainers,
            monsters = monsters,
            defaults = contactDefaults,
            onDefaultChanged = viewModel::setContactDefault,
            onPoolChanged = viewModel::setContactRandomPool,
            onPoolReset = viewModel::resetContactRandomPool,
        )
        return
    }
    if (currentContact == null) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(if (pendingOnlineProfileId != null) R.string.online_profile_choose_contact_prompt else R.string.contact_chooser_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val selectedItemIndex = when (selectedTab) {
            0 -> trainerSelectedItemIndex
            else -> monsterSelectedItemIndex
        }
        val currentTabHasCharacters = if (selectedTab == 0) trainers.isNotEmpty() else monsters.isNotEmpty()
        val effectiveLayout = if (currentTabHasCharacters) layout else CharacterLayout.List
        val listState = if (selectedTab == 0) trainerListState else monsterListState
        val gridState = if (selectedTab == 0) trainerGridState else monsterGridState
        val usesGlobalDefaults = if (selectedTab == 0) trainerUsesGlobalDefaults else monsterUsesGlobalDefaults
        val controlsVisible = rememberCharacterSelectionControlsVisibility(
            listState = listState,
            gridState = gridState,
            useGrid = effectiveLayout == CharacterLayout.Grid,
        )

        AnimatedVisibility(
            visible = controlsVisible || usesGlobalDefaults,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CharacterSettingsDropdowns(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        val selectedItemIndex = if (tab == 0) trainerSelectedItemIndex else monsterSelectedItemIndex
                        val nextTabHasCharacters = if (tab == 0) trainers.isNotEmpty() else monsters.isNotEmpty()
                        val nextTabEffectiveLayout = if (nextTabHasCharacters) layout else CharacterLayout.List

                        if (nextTabEffectiveLayout == CharacterLayout.List) {
                            (if (tab == 0) trainerListState else monsterListState).requestScrollToItem(selectedItemIndex)
                        } else {
                            (if (tab == 0) trainerGridState else monsterGridState).requestScrollToItem(selectedItemIndex)
                        }
                        viewModel.setSelectedTab(tab)
                    },
                    mode = when {
                        usesGlobalDefaults -> ContactAssignmentMode.Global
                        (if (selectedTab == 0) trainerMode else monsterMode) == ContactCharacterMode.Random -> ContactAssignmentMode.Random
                        else -> ContactAssignmentMode.Custom
                    },
                    onModeChanged = { nextMode ->
                        val type = if (selectedTab == 0) CharacterType.Trainer else CharacterType.Monster
                        contactRandomPoolDrafts.remove(type)
                        when (nextMode) {
                            ContactAssignmentMode.Global -> viewModel.setUsesGlobalDefaults(type, true)
                            ContactAssignmentMode.Custom -> viewModel.setUsesGlobalDefaults(type, false)
                            ContactAssignmentMode.Random -> if (type == CharacterType.Trainer) viewModel.randomizeTrainer() else viewModel.randomizeMonster()
                        }
                    },
                )
                if (!usesGlobalDefaults) {
                    CharacterSelectionActions(
                        selectedTab = selectedTab,
                        onTabSelected = {},
                        isAddEnabled = !isLimitReached,
                        onAddCharacter = { navigator?.navigateTo(if (selectedTab == 0) 1 else 2) },
                        showCharacterTypeTabs = false,
                        filter = if (selectedTab == 1) filter else null,
                        onFilterSelected = if (selectedTab == 1) { nextFilter ->
                            monsterListState.requestScrollToItem(0)
                            monsterGridState.requestScrollToItem(0)
                            viewModel.setFilter(nextFilter)
                        } else null,
                    )
                    if ((if (selectedTab == 0) trainerMode else monsterMode) == ContactCharacterMode.Random) {
                        val randomPool = if (selectedTab == 0) effectiveTrainerRandomPool else effectiveMonsterRandomPool
                        val allReferences = if (selectedTab == 0) {
                            viewModel.allContactPoolReferences(CharacterType.Trainer)
                        } else {
                            viewModel.allContactPoolReferences(CharacterType.Monster)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val type = if (selectedTab == 0) CharacterType.Trainer else CharacterType.Monster
                                    viewModel.clearContactSpecificRandomPool(type)
                                    contactRandomPoolDrafts.remove(type)
                                },
                            ) {
                                Text(stringResource(R.string.contact_random_pool_reset))
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val type = if (selectedTab == 0) CharacterType.Trainer else CharacterType.Monster
                                    updateContactPool(type, if (randomPool == allReferences) emptySet() else allReferences)
                                },
                            ) {
                                Text(stringResource(if (randomPool == allReferences) R.string.contact_random_pool_deselect_all else R.string.contact_random_pool_select_all))
                            }
                        }
                }
                }
            }
        }
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
            if (usesGlobalDefaults) {
                ContactCharacterInheritedSummary(
                    selectedType = if (selectedTab == 0) CharacterType.Trainer else CharacterType.Monster,
                    onOpenGlobalDefaults = {
                        rootNavigator?.invoke(CharacterSettingsPage.ContactDefaults.index, null)
                    },
                )
            } else if (effectiveLayout == CharacterLayout.List) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 0.dp, bottom = 72.dp)) {
                    when (selectedTab) {
                        0 -> characterTypeItems(
                            title = trainerTitle,
                            pluralTitle = trainersTitle,
                            defaultCharacter = BuiltInCharacters.trainer,
                            defaultReference = BuiltInCharacters.defaultTrainerReference,
                            characters = trainers,
                            selected = assignedTrainer,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                if (trainerMode == ContactCharacterMode.Random && it != null) {
                                    updateContactPool(CharacterType.Trainer, effectiveTrainerRandomPool + it)
                                } else {
                                    viewModel.assignTrainer(it)
                                }
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = trainerMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeTrainer()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            showRandomize = false,
                            selectedReferences = if (trainerMode == ContactCharacterMode.Random) effectiveTrainerRandomPool else emptySet(),
                            onSelected = if (trainerMode == ContactCharacterMode.Random) { reference ->
                                updateContactPool(CharacterType.Trainer, effectiveTrainerRandomPool - reference)
                            } else null,
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(1, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                        1 -> characterTypeItems(
                            title = monsterTitle,
                            pluralTitle = monstersTitle,
                            defaultCharacter = BuiltInCharacters.monster.character,
                            defaultReference = BuiltInCharacters.defaultMonsterReference,
                            characters = monsters,
                            selected = assignedMonster,
                            filter = filter,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                if (monsterMode == ContactCharacterMode.Random && it != null) {
                                    updateContactPool(CharacterType.Monster, effectiveMonsterRandomPool + it)
                                } else {
                                    viewModel.assignMonster(it)
                                }
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = monsterMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeMonster()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            showRandomize = false,
                            selectedReferences = if (monsterMode == ContactCharacterMode.Random) effectiveMonsterRandomPool else emptySet(),
                            onSelected = if (monsterMode == ContactCharacterMode.Random) { reference ->
                                updateContactPool(CharacterType.Monster, effectiveMonsterRandomPool - reference)
                            } else null,
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(2, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 0.dp, bottom = 72.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when (selectedTab) {
                        0 -> characterTypeGridItems(
                            title = trainerTitle,
                            pluralTitle = trainersTitle,
                            defaultCharacter = BuiltInCharacters.trainer,
                            defaultReference = BuiltInCharacters.defaultTrainerReference,
                            characters = trainers,
                            selected = assignedTrainer,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                if (trainerMode == ContactCharacterMode.Random && it != null) {
                                    updateContactPool(CharacterType.Trainer, effectiveTrainerRandomPool + it)
                                } else {
                                    viewModel.assignTrainer(it)
                                }
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = trainerMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeTrainer()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            showRandomize = false,
                            selectedReferences = if (trainerMode == ContactCharacterMode.Random) effectiveTrainerRandomPool else emptySet(),
                            onSelected = if (trainerMode == ContactCharacterMode.Random) { reference ->
                                updateContactPool(CharacterType.Trainer, effectiveTrainerRandomPool - reference)
                            } else null,
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(1, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                        1 -> characterTypeGridItems(
                            title = monsterTitle,
                            pluralTitle = monstersTitle,
                            defaultCharacter = BuiltInCharacters.monster.character,
                            defaultReference = BuiltInCharacters.defaultMonsterReference,
                            characters = monsters,
                            selected = assignedMonster,
                            filter = filter,
                            defaultArtwork = { it.contactArtwork },
                            artworkTarget = CharacterAssignmentTarget.Contact,
                            onSelect = {
                                if (monsterMode == ContactCharacterMode.Random && it != null) {
                                    updateContactPool(CharacterType.Monster, effectiveMonsterRandomPool + it)
                                } else {
                                    viewModel.assignMonster(it)
                                }
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            unlockedVariants = unlockedVariants,
                            isRandomSelected = monsterMode == ContactCharacterMode.Random,
                            onRandomize = {
                                viewModel.randomizeMonster()
                                if (entryPoint == ContactCharacterSettingsEntryPoint.ContactList) {
                                    navigator?.navigateBack()
                                }
                            },
                            showRandomize = false,
                            selectedReferences = if (monsterMode == ContactCharacterMode.Random) effectiveMonsterRandomPool else emptySet(),
                            onSelected = if (monsterMode == ContactCharacterMode.Random) { reference ->
                                updateContactPool(CharacterType.Monster, effectiveMonsterRandomPool - reference)
                            } else null,
                            onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                            onEdit = { navigator?.navigateTo(2, it.character.id) },
                            onShare = { pendingShare = it }
                        )
                    }
                }
            }
            if (!usesGlobalDefaults && currentTabHasCharacters) {
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
private fun ContactCharacterInheritedSummary(
    selectedType: CharacterType,
    onOpenGlobalDefaults: () -> Unit,
) {
    val typeLabel = stringResource(
        if (selectedType == CharacterType.Trainer) R.string.character_type_trainer else R.string.character_type_monster,
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.contact_character_inherited_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.contact_character_inherited_message, typeLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onOpenGlobalDefaults,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.contact_character_edit_global_defaults))
            }
        }
    }
}

@Composable
private fun CharacterSettingsDropdowns(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    mode: ContactAssignmentMode,
    onModeChanged: (ContactAssignmentMode) -> Unit,
) {
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    val chooseCharacterLabel = if (selectedTab == 0) {
        R.string.contact_choose_trainer
    } else {
        R.string.contact_choose_monster
    }
    val sourceLabel = stringResource(
        when (mode) {
            ContactAssignmentMode.Global -> R.string.contact_character_source_global
            ContactAssignmentMode.Custom -> chooseCharacterLabel
            ContactAssignmentMode.Random -> R.string.randomize
        },
    )
    val buttonShape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CharacterTypeSwitch(
            selectedType = if (selectedTab == 0) CharacterType.Trainer else CharacterType.Monster,
            onTypeSelected = { onTabSelected(if (it == CharacterType.Trainer) 0 else 1) },
            modifier = Modifier.weight(1f),
        )
        CompactDropdown(
            label = sourceLabel,
            expanded = sourceMenuExpanded,
            onExpandedChange = { sourceMenuExpanded = it },
            modifier = Modifier.weight(1f),
            shape = buttonShape,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.contact_character_source_global)) },
                onClick = {
                    onModeChanged(ContactAssignmentMode.Global)
                    sourceMenuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(chooseCharacterLabel)) },
                onClick = {
                    onModeChanged(ContactAssignmentMode.Custom)
                    sourceMenuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.randomize)) },
                onClick = {
                    onModeChanged(ContactAssignmentMode.Random)
                    sourceMenuExpanded = false
                },
            )
        }
    }
}

@Composable
private fun CharacterTypeSwitch(
    selectedType: CharacterType,
    onTypeSelected: (CharacterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.height(36.dp),
    ) {
        SegmentedButton(
            selected = selectedType == CharacterType.Trainer,
            onClick = { onTypeSelected(CharacterType.Trainer) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.character_type_trainer),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SegmentedButton(
            selected = selectedType == CharacterType.Monster,
            onClick = { onTypeSelected(CharacterType.Monster) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.character_type_monster),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ContactDefaultsDropdowns(
    selectedType: CharacterType,
    onTypeSelected: (CharacterType) -> Unit,
    isPoolMode: Boolean,
    onPoolModeChanged: (Boolean) -> Unit,
) {
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val buttonShape = RoundedCornerShape(18.dp)
    val modeLabel = stringResource(
        if (isPoolMode) {
            R.string.randomize
        } else if (selectedType == CharacterType.Trainer) {
            R.string.contact_choose_trainer
        } else {
            R.string.contact_choose_monster
        },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CharacterTypeSwitch(
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            modifier = Modifier.weight(1f),
        )
        CompactDropdown(
            label = modeLabel,
            expanded = modeMenuExpanded,
            onExpandedChange = { modeMenuExpanded = it },
            modifier = Modifier.weight(1f),
            shape = buttonShape,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                            stringResource(
                                if (selectedType == CharacterType.Trainer) {
                                    R.string.contact_choose_trainer
                                } else {
                                    R.string.contact_choose_monster
                                },
                        ),
                    )
                },
                onClick = {
                    onPoolModeChanged(false)
                    modeMenuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.randomize)) },
                onClick = {
                    onPoolModeChanged(true)
                    modeMenuExpanded = false
                },
            )
        }
    }
}

@Composable
private fun CompactDropdown(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable { onExpandedChange(true) }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppIcon(
                icon = LocalAppIcons.current.arrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content,
        )
    }
}

@Composable
internal fun rememberCharacterSelectionControlsVisibility(
    listState: LazyListState,
    gridState: LazyGridState,
    useGrid: Boolean,
): Boolean {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(listState, gridState, useGrid) {
        visible = true
        var previousPosition: Pair<Int, Int>? = null
        snapshotFlow {
            if (useGrid) {
                gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
            } else {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }
        }.collect { position ->
            previousPosition?.let { previous ->
                when {
                    position.first < previous.first ||
                        (position.first == previous.first && position.second < previous.second) -> visible = true
                    position.first > previous.first ||
                        (position.first == previous.first && position.second > previous.second) -> visible = false
                }
            }
            previousPosition = position
        }
    }

    return visible
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
