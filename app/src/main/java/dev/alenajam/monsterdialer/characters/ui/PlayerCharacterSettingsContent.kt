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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.launch

@Composable
fun ColumnScope.PlayerCharacterSettingsContent(
    viewModel: PlayerCharacterSettingsViewModel = hiltViewModel()
) {
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val trainers by viewModel.trainers.collectAsStateWithLifecycle()
    val monsters by viewModel.monsters.collectAsStateWithLifecycle()
    val dataVersion by viewModel.dataVersion.collectAsStateWithLifecycle()
    val isLimitReached by viewModel.isLimitReached.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    val currentTabHasCharacters = if (selectedTab == 0) trainers.isNotEmpty() else monsters.isNotEmpty()
    val effectiveLayout = if (currentTabHasCharacters) layout else CharacterLayout.List

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
    val addTrainerLabel = stringResource(R.string.add_trainer)
    val addMonsterLabel = stringResource(R.string.add_monster)
    val navigator = dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator.current

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
    val selectedItemIndex = when (selectedTab) {
        0 -> trainerSelectedItemIndex
        else -> monsterSelectedItemIndex
    }
    val listState = if (selectedTab == 0) trainerListState else monsterListState
    val gridState = if (selectedTab == 0) trainerGridState else monsterGridState
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingDeletion by remember { mutableStateOf<InstalledPackCharacter?>(null) }
    var isPendingDeletionInUse by remember { mutableStateOf(false) }

    pendingDeletion?.let { character ->
        CustomCharacterDeletionConfirmationDialog(
            characterName = character.character.name,
            isInUse = isPendingDeletionInUse,
            onConfirm = {
                viewModel.deleteCustomCharacter(character.character.id)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }

    LaunchedEffect(dataVersion, trainers.isEmpty(), monsters.isEmpty()) {
        if (trainers.isNotEmpty() || monsters.isNotEmpty()) {
            val selectedItemIndex = if (selectedTab == 0) trainerSelectedItemIndex else monsterSelectedItemIndex
            if (effectiveLayout == CharacterLayout.List) listState.requestScrollToItem(selectedItemIndex)
            else gridState.requestScrollToItem(selectedItemIndex)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        if (effectiveLayout == CharacterLayout.List) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp)) {
                when (selectedTab) {
                    0 -> characterTypeItems(trainerTitle, BuiltInCharacters.trainer, trainers, assignedTrainer, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignTrainer, { navigator?.navigateTo(0) }, addTrainerLabel, !isLimitReached, onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } }, onEdit = { navigator?.navigateTo(0, it.character.id) })
                    1 -> characterTypeItems(monsterTitle, BuiltInCharacters.monster.character, monsters, assignedMonster, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignMonster, { navigator?.navigateTo(1) }, addMonsterLabel, !isLimitReached, onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } }, onEdit = { navigator?.navigateTo(1, it.character.id) })
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (selectedTab) {
                    0 -> characterTypeGridItems(trainerTitle, BuiltInCharacters.trainer, trainers, assignedTrainer, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignTrainer, { navigator?.navigateTo(0) }, addTrainerLabel, !isLimitReached, onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } }, onEdit = { navigator?.navigateTo(0, it.character.id) })
                    1 -> characterTypeGridItems(monsterTitle, BuiltInCharacters.monster.character, monsters, assignedMonster, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignMonster, { navigator?.navigateTo(1) }, addMonsterLabel, !isLimitReached, onDelete = { character -> scope.launch { isPendingDeletionInUse = viewModel.isCharacterInUse(character.character.id); pendingDeletion = character } }, onEdit = { navigator?.navigateTo(1, it.character.id) })
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
