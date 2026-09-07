package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.packs.data.CharacterType

internal enum class PlayerCharacterSettingsRoute(
    val payload: String,
    val selectedTab: Int,
) {
    ChangeTrainer(payload = "change_trainer", selectedTab = 0),
    AddToRoster(payload = "add_to_roster", selectedTab = 1);

    companion object {
        fun fromPayload(payload: String?): PlayerCharacterSettingsRoute? =
            payload?.split(":")?.firstOrNull()?.let { value ->
                entries.firstOrNull { it.payload == value }
            }
    }
}

@Composable
internal fun ColumnScope.PlayerCharacterSettingsContent(
    route: PlayerCharacterSettingsRoute? = null,
    payload: String? = null,
    viewModel: PlayerCharacterSettingsViewModel = hiltViewModel(),
) {
    val assignedTrainer = viewModel.assignedTrainer.collectAsStateWithLifecycle().value
    val assignedMonster = viewModel.assignedMonster.collectAsStateWithLifecycle().value
    val monsterRoster = viewModel.monsterRoster.collectAsStateWithLifecycle().value
    val trainers = viewModel.trainers.collectAsStateWithLifecycle().value
    val monsters = viewModel.monsters.collectAsStateWithLifecycle().value
    val isLimitReached = viewModel.isLimitReached.collectAsStateWithLifecycle().value
    val selectedTab = viewModel.selectedTab.collectAsStateWithLifecycle().value
    val filter = viewModel.filter.collectAsStateWithLifecycle().value
    val unlockedVariants = viewModel.unlockedVariants.collectAsStateWithLifecycle().value
    val navigator = dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator.current
    val targetSlotIndex = payload?.split(":")?.getOrNull(1)?.toIntOrNull()
    val selectedMonster = if (route == PlayerCharacterSettingsRoute.AddToRoster) {
        targetSlotIndex?.let(monsterRoster::getOrNull)
    } else {
        assignedMonster
    }
    val selectedType = if (selectedTab == 0) CharacterType.Trainer else CharacterType.Monster

    LaunchedEffect(route, payload) {
        viewModel.setTargetSlotIndex(targetSlotIndex)
        if (targetSlotIndex != null) viewModel.setFilter(MonsterFilter.All)
        route?.let { viewModel.setSelectedTab(it.selectedTab) }
    }

    RetroCharacterPicker(
        modifier = Modifier.weight(1f),
        type = selectedType,
        selected = if (selectedType == CharacterType.Trainer) assignedTrainer else selectedMonster,
        characters = if (selectedType == CharacterType.Trainer) trainers else monsters,
        unlockedVariants = unlockedVariants,
        filter = if (selectedType == CharacterType.Monster) filter else MonsterFilter.All,
        defaultCharacter = if (selectedType == CharacterType.Trainer) {
            BuiltInCharacters.trainer
        } else {
            BuiltInCharacters.monster.character
        },
        defaultArtwork = { playerArtwork.resource },
        onAssign = { reference ->
            when (selectedType) {
                CharacterType.Trainer -> viewModel.assignTrainer(reference)
                CharacterType.Monster -> viewModel.assignMonster(requireNotNull(reference))
            }
            navigator?.navigateBack()
        },
        onBack = { navigator?.navigateBack() },
        onAddCharacter = if (!isLimitReached) {
            { navigator?.navigateTo(if (selectedType == CharacterType.Trainer) 0 else 1) }
        } else {
            null
        },
        addCharacterLabel = stringResource(R.string.retro_picker_add),
        onFilterSelected = if (selectedType == CharacterType.Monster) viewModel::setFilter else null,
        guideContents = listOf(
            GuideContent(
                R.string.characters_help_character_list_title,
                R.string.characters_help_player_character_list_message,
            ),
            *radiantGuideContents().toTypedArray(),
        ),
    )
}
