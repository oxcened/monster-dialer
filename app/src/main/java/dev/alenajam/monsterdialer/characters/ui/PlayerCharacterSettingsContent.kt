package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.launch

internal enum class PlayerCharacterSettingsRoute(
    val payload: String,
    val selectedTab: Int,
) {
    ChangeTrainer(payload = "change_trainer", selectedTab = 0),
    AddToRoster(payload = "add_to_roster", selectedTab = 1);

    companion object {
        fun fromPayload(payload: String?): PlayerCharacterSettingsRoute? =
            payload?.split(":")?.firstOrNull()?.let { p ->
                entries.firstOrNull { it.payload == p }
            }
    }
}

@Composable
internal fun ColumnScope.PlayerCharacterSettingsContent(
    route: PlayerCharacterSettingsRoute? = null,
    payload: String? = null,
    viewModel: PlayerCharacterSettingsViewModel = hiltViewModel(),
) {
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val monsterRoster by viewModel.monsterRoster.collectAsStateWithLifecycle()
    val trainers by viewModel.trainers.collectAsStateWithLifecycle()
    val monsters by viewModel.monsters.collectAsStateWithLifecycle()
    val isLimitReached by viewModel.isLimitReached.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val unlockedVariants by viewModel.unlockedVariants.collectAsStateWithLifecycle()
    val targetSlotIndex by viewModel.targetSlotIndex.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    val currentTabHasCharacters = if (selectedTab == 0) trainers.isNotEmpty() else monsters.isNotEmpty()
    val effectiveLayout = if (currentTabHasCharacters) layout else CharacterLayout.List

    val assignedMonsterForSlot = targetSlotIndex?.let { index ->
        monsterRoster.getOrNull(index)
    } ?: assignedMonster

    val trainerSelectedItemIndex = selectedCharacterIndex(trainers, assignedTrainer)
    val monsterSelectedItemIndex = selectedCharacterIndex(monsters, assignedMonsterForSlot)
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
    val navigator = dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator.current

    LaunchedEffect(route, payload) {
        val slotIndex = payload?.split(":")?.getOrNull(1)?.toIntOrNull()
        viewModel.setTargetSlotIndex(slotIndex)
        if (slotIndex != null) viewModel.setFilter(MonsterFilter.All)
        route?.let { viewModel.setSelectedTab(it.selectedTab) }
    }

    CharacterTypeTabs(
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
        }
    )

    if (selectedTab == 1) {
        MonsterFilterChips(
            selectedFilter = filter,
            onFilterSelected = viewModel::setFilter
        )
    }

    val selectedItemIndex = when (selectedTab) {
        0 -> trainerSelectedItemIndex
        else -> monsterSelectedItemIndex
    }
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

    LaunchedEffect(
        selectedTab,
        selectedItemIndex,
        effectiveLayout,
        trainers,
        monsters,
    ) {
        if (currentTabHasCharacters) {
            if (effectiveLayout == CharacterLayout.List) listState.requestScrollToItem(selectedItemIndex)
            else gridState.requestScrollToItem(selectedItemIndex)
        }
    }

    val selectedReferences = monsterRoster.toSet() + listOfNotNull(assignedMonster)

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
                            navigator?.navigateBack()
                        },
                        onAddCharacter = { navigator?.navigateTo(0) },
                        addLabel = addTrainerLabel,
                        isAddEnabled = !isLimitReached,
                        unlockedVariants = unlockedVariants,
                        onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                        onEdit = { navigator?.navigateTo(0, it.character.id) },
                        onShare = { pendingShare = it }
                    )
                    1 -> characterTypeItems(
                        title = monsterTitle,
                        pluralTitle = monstersTitle,
                        defaultCharacter = BuiltInCharacters.monster.character,
                        characters = monsters,
                        selected = assignedMonsterForSlot,
                        defaultArtwork = { it.contactArtwork },
                        artworkTarget = CharacterAssignmentTarget.Contact,
                        defaultReference = BuiltInCharacters.defaultMonsterReference,
                        onSelect = {
                            viewModel.assignMonster(requireNotNull(it))
                            navigator?.navigateBack()
                        },
                        onAddCharacter = { navigator?.navigateTo(1) },
                        addLabel = addMonsterLabel,
                        isAddEnabled = !isLimitReached,
                        unlockedVariants = unlockedVariants,
                        onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                        onEdit = { navigator?.navigateTo(1, it.character.id) },
                        onShare = { pendingShare = it },
                        selectedReferences = selectedReferences,
                        hideSelected = true,
                        filter = filter
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
                            navigator?.navigateBack()
                        },
                        onAddCharacter = { navigator?.navigateTo(0) },
                        addLabel = addTrainerLabel,
                        isAddEnabled = !isLimitReached,
                        unlockedVariants = unlockedVariants,
                        onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                        onEdit = { navigator?.navigateTo(0, it.character.id) },
                        onShare = { pendingShare = it }
                    )
                    1 -> characterTypeGridItems(
                        title = monsterTitle,
                        pluralTitle = monstersTitle,
                        defaultCharacter = BuiltInCharacters.monster.character,
                        characters = monsters,
                        selected = assignedMonsterForSlot,
                        defaultArtwork = { it.contactArtwork },
                        artworkTarget = CharacterAssignmentTarget.Contact,
                        defaultReference = BuiltInCharacters.defaultMonsterReference,
                        onSelect = {
                            viewModel.assignMonster(requireNotNull(it))
                            navigator?.navigateBack()
                        },
                        onAddCharacter = { navigator?.navigateTo(1) },
                        addLabel = addMonsterLabel,
                        isAddEnabled = !isLimitReached,
                        unlockedVariants = unlockedVariants,
                        onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } },
                        onEdit = { navigator?.navigateTo(1, it.character.id) },
                        onShare = { pendingShare = it },
                        selectedReferences = selectedReferences,
                        hideSelected = true,
                        filter = filter
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MonsterFilterChips(
    selectedFilter: MonsterFilter,
    onFilterSelected: (MonsterFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MonsterFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            MonsterFilter.All -> stringResource(R.string.filter_all)
                            MonsterFilter.Regular -> stringResource(R.string.filter_regular)
                            MonsterFilter.RadiantUnlocked -> stringResource(R.string.filter_unlocked_radiant)
                            MonsterFilter.RadiantLocked -> stringResource(R.string.filter_locked_radiant)
                        }
                    )
                }
            )
        }
    }
}
