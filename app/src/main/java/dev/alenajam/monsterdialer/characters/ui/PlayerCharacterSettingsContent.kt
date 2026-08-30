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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters

@Composable
fun ColumnScope.PlayerCharacterSettingsContent(
    viewModel: PlayerCharacterSettingsViewModel = hiltViewModel()
) {
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val trainers = viewModel.trainers
    val monsters = viewModel.monsters
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
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

    CharacterTypeTabs(
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            val selectedItemIndex = if (tab == 0) trainerSelectedItemIndex else monsterSelectedItemIndex
            if (layout == CharacterLayout.List) {
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
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        if (layout == CharacterLayout.List) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp)) {
                when (selectedTab) {
                    0 -> characterTypeItems(trainerTitle, BuiltInCharacters.trainer, trainers, assignedTrainer, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignTrainer)
                    1 -> characterTypeItems(monsterTitle, BuiltInCharacters.monster.character, monsters, assignedMonster, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignMonster)
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (selectedTab) {
                    0 -> characterTypeGridItems(trainerTitle, BuiltInCharacters.trainer, trainers, assignedTrainer, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignTrainer)
                    1 -> characterTypeGridItems(monsterTitle, BuiltInCharacters.monster.character, monsters, assignedMonster, { it.playerArtwork }, { it.imageFile(requireNotNull(it.character.backImage)) }, viewModel::assignMonster)
                }
            }
        }
        if (trainers.isNotEmpty() || monsters.isNotEmpty()) {
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
            if (layout == CharacterLayout.List) JumpToSelectedCharacterButton(listState, selectedItemIndex, Modifier.align(Alignment.BottomEnd).padding(16.dp))
            else JumpToSelectedCharacterButton(gridState, selectedItemIndex, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}
