package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    val trainerTitle = stringResource(R.string.character_type_trainer)
    val monsterTitle = stringResource(R.string.character_type_monster)

    CharacterTypeTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
}
