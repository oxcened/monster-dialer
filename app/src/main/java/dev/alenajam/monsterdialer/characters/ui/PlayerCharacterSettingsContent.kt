package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val trainerSelectedItemIndex = selectedCharacterIndex(trainers, assignedTrainer)
    val monsterSelectedItemIndex = selectedCharacterIndex(monsters, assignedMonster)
    val trainerListState = key(trainerSelectedItemIndex) {
        rememberLazyListState(initialFirstVisibleItemIndex = trainerSelectedItemIndex)
    }
    val monsterListState = key(monsterSelectedItemIndex) {
        rememberLazyListState(initialFirstVisibleItemIndex = monsterSelectedItemIndex)
    }
    val trainerTitle = stringResource(R.string.character_type_trainer)
    val monsterTitle = stringResource(R.string.character_type_monster)

    CharacterTypeTabs(
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            val listState = if (tab == 0) trainerListState else monsterListState
            val selectedItemIndex = if (tab == 0) trainerSelectedItemIndex else monsterSelectedItemIndex
            listState.requestScrollToItem(selectedItemIndex)
            selectedTab = tab
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
    val selectedItemIndex = when (selectedTab) {
        0 -> trainerSelectedItemIndex
        else -> monsterSelectedItemIndex
    }
    val listState = if (selectedTab == 0) trainerListState else monsterListState
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            when (selectedTab) {
                0 -> characterTypeItems(
                    title = trainerTitle,
                    defaultCharacter = BuiltInCharacters.trainer,
                    characters = trainers,
                    selected = assignedTrainer,
                    defaultArtwork = { it.playerArtwork },
                    packArtwork = { it.imageFile(requireNotNull(it.character.backImage)) },
                    onSelect = viewModel::assignTrainer
                )
                1 -> characterTypeItems(
                    title = monsterTitle,
                    defaultCharacter = BuiltInCharacters.monster.character,
                    characters = monsters,
                    selected = assignedMonster,
                    defaultArtwork = { it.playerArtwork },
                    packArtwork = { it.imageFile(requireNotNull(it.character.backImage)) },
                    onSelect = viewModel::assignMonster
                )
            }
        }
        JumpToSelectedCharacterButton(
            listState = listState,
            selectedItemIndex = selectedItemIndex,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}
