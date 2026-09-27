package dev.alenajam.monsterdialer.app

import androidx.compose.foundation.Image
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroManualDialogueBox
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.ui.MonsterFilter
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.RetroCharacterPicker
import dev.alenajam.monsterdialer.packs.data.CharacterType

private enum class FirstRunDialogueStep(
    val messageResource: Int,
    val showsMonster: Boolean = false,
) {
    Welcome(R.string.first_run_welcome_description),
    GuideName(R.string.first_run_guide_name),
    GuideTitle(R.string.first_run_guide_title),
    BattleStarts(R.string.first_run_battle_starts, showsMonster = true),
    BattleIsWatchOnly(R.string.first_run_battle_watch_only, showsMonster = true),
    ChooseTeam(R.string.first_run_choose_team);

    fun next(): FirstRunDialogueStep = when (this) {
        Welcome -> GuideName
        GuideName -> GuideTitle
        GuideTitle -> BattleStarts
        BattleStarts -> BattleIsWatchOnly
        BattleIsWatchOnly -> ChooseTeam
        ChooseTeam -> this
    }

    fun previous(): FirstRunDialogueStep? = when (this) {
        Welcome -> null
        GuideName -> Welcome
        GuideTitle -> GuideName
        BattleStarts -> GuideTitle
        BattleIsWatchOnly -> BattleStarts
        ChooseTeam -> BattleIsWatchOnly
    }
}

@Composable
fun FirstRunWelcomeScreen(
    onContinue: () -> Unit,
) {
    var dialogueStep by rememberSaveable { mutableStateOf(FirstRunDialogueStep.Welcome) }
    var advanceDialogue by remember { mutableStateOf<() -> Unit>({}) }
    var characterChoiceType by rememberSaveable { mutableStateOf<CharacterType?>(null) }
    val dialogueMessage = stringResource(dialogueStep.messageResource)

    if (characterChoiceType != null) {
        FirstRunCharacterChoice(
            type = requireNotNull(characterChoiceType),
            onBack = {
                characterChoiceType = when (characterChoiceType) {
                    CharacterType.Trainer -> null
                    CharacterType.Monster -> CharacterType.Trainer
                    null -> null
                }
            },
            onTrainerChosen = { characterChoiceType = CharacterType.Monster },
            onMonsterChosen = {
                characterChoiceType = null
                onContinue()
            },
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Crossfade(
                        targetState = dialogueStep.showsMonster,
                        label = "onboarding-sprite",
                    ) { showMonster ->
                        Image(
                            painter = painterResource(
                                if (showMonster) R.drawable.battle_enemy_monster
                                else R.drawable.first_run_guide,
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(128.dp),
                        )
                    }
                    RetroManualDialogueBox(
                        message = dialogueMessage,
                        modifier = Modifier.fillMaxWidth(),
                        animationKey = dialogueStep,
                        onAdvanceActionChanged = { advanceDialogue = it },
                        onMessageFinished = {
                            when (dialogueStep) {
                                FirstRunDialogueStep.ChooseTeam -> {
                                    characterChoiceType = CharacterType.Trainer
                                }
                                else -> dialogueStep = dialogueStep.next()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RetroActionButton(
                    key = stringResource(R.string.retro_key_a),
                    label = stringResource(R.string.first_run_next),
                    onClick = advanceDialogue,
                )
                dialogueStep.previous()?.let { previousStep ->
                    RetroActionButton(
                        key = stringResource(R.string.retro_key_b),
                        label = stringResource(R.string.retro_action_back_label),
                        onClick = { dialogueStep = previousStep },
                    )
                }
            }
        }
    }
}

@Composable
fun FirstRunCompletionScreen(
    onContinue: () -> Unit,
) {
    var advanceDialogue by remember { mutableStateOf<() -> Unit>({}) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.first_run_guide),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(128.dp),
                    )
                    RetroManualDialogueBox(
                        message = stringResource(R.string.first_run_setup_complete),
                        modifier = Modifier.fillMaxWidth(),
                        animationKey = "setup-complete",
                        onAdvanceActionChanged = { advanceDialogue = it },
                        onMessageFinished = onContinue,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                RetroActionButton(
                    key = stringResource(R.string.retro_key_a),
                    label = stringResource(R.string.first_run_start_dialing),
                    onClick = advanceDialogue,
                )
            }
        }
    }
}

@Composable
private fun FirstRunCharacterChoice(
    type: CharacterType,
    onBack: () -> Unit,
    onTrainerChosen: () -> Unit,
    onMonsterChosen: () -> Unit,
) {
    val viewModel: PlayerCharacterSettingsViewModel = hiltViewModel()
    val assignedTrainer by viewModel.assignedTrainer.collectAsStateWithLifecycle()
    val assignedMonster by viewModel.assignedMonster.collectAsStateWithLifecycle()
    val trainers by viewModel.trainers.collectAsStateWithLifecycle()
    val monsters by viewModel.monsters.collectAsStateWithLifecycle()
    val unlockedVariants by viewModel.unlockedVariants.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        RetroCharacterPicker(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            type = type,
            selected = when (type) {
                CharacterType.Trainer -> assignedTrainer ?: BuiltInCharacters.defaultTrainerReference
                CharacterType.Monster -> assignedMonster ?: BuiltInCharacters.defaultMonsterReference
            },
            characters = if (type == CharacterType.Trainer) trainers else monsters,
            unlockedVariants = unlockedVariants,
            filter = MonsterFilter.All,
            defaultCharacter = if (type == CharacterType.Trainer) {
                BuiltInCharacters.trainer
            } else {
                BuiltInCharacters.monster.character
            },
            defaultArtwork = { contactArtwork.resource },
            onAssign = { reference ->
                when (type) {
                    CharacterType.Trainer -> {
                        viewModel.assignTrainer(reference, onCompleted = onTrainerChosen)
                    }
                    CharacterType.Monster -> {
                        reference?.let {
                            viewModel.assignMonster(
                                reference = it,
                                makeActive = true,
                                onCompleted = onMonsterChosen,
                            )
                        }
                    }
                }
            },
            onBack = onBack,
            isGuidedFirstStep = true,
            showOptions = false,
            showFilterOptions = false,
        )
    }
}
