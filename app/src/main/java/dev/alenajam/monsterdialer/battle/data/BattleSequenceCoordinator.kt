package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BattleSequenceCoordinator(
    private val scope: CoroutineScope,
    private val timing: BattleTiming = BattleTiming(),
    private val string: (Int, Array<out Any>) -> String = { _, _ -> error("A battle string resolver is required") },
    private val pause: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutableState = MutableStateFlow(BattleUiState())
    val state: StateFlow<BattleUiState> = mutableState.asStateFlow()

    private val animationCompletions = Channel<AnimationCompletion>(Channel.UNLIMITED)
    private val dialogueCompletions = Channel<DialogueCompletion>(Channel.UNLIMITED)
    private var sequenceJob: Job? = null
    private var nextRunId = 0L

    fun start(encounter: BattleEncounter) {
        sequenceJob?.cancel()
        val runId = ++nextRunId
        mutableState.value = BattleUiState(runId = runId, encounter = encounter)
        sequenceJob = scope.launch { runSequence(runId, encounter) }
    }

    fun stop() {
        sequenceJob?.cancel()
        sequenceJob = null
        mutableState.value = BattleUiState(runId = ++nextRunId)
    }

    fun animationCompleted(runId: Long, phase: BattlePhase) {
        animationCompletions.trySend(AnimationCompletion(runId, phase))
    }

    fun dialogueCompleted(runId: Long, dialogueId: Long) {
        dialogueCompletions.trySend(DialogueCompletion(runId, dialogueId))
    }

    private suspend fun runSequence(runId: Long, encounter: BattleEncounter) {
        phase(runId, BattlePhase.TrainersEntering)
        awaitAnimation(runId, BattlePhase.TrainersEntering)
        phase(runId, BattlePhase.TrainersColorizing)
        awaitAnimation(runId, BattlePhase.TrainersColorizing)

        when (encounter.type) {
            EncounterType.Trainer -> runTrainerIntro(runId, encounter)
            EncounterType.RadiantWild -> runWildIntro(runId, encounter, radiant = true)
            EncounterType.Anonymous -> runWildIntro(runId, encounter, radiant = false)
        }

        phase(runId, BattlePhase.PlayerTrainerLeaving)
        awaitAnimation(runId, BattlePhase.PlayerTrainerLeaving)
        typeMessage(runId, string(R.string.battle_player_send_out, arrayOf(encounter.player.name.uppercase())))
        phase(runId, BattlePhase.PlayerRevealing)
        reveal(runId, player = true)
        if (encounter.player.isRadiant) showRadiance(runId, player = true)
        update(runId) { copy(playerPanel = BattlePanel.Monster) }
        pause(timing.readyHoldMillis)
        typeMessage(runId, string(R.string.battle_prompt, emptyArray()))
        phase(runId, BattlePhase.Ready)
    }

    private suspend fun runTrainerIntro(runId: Long, encounter: BattleEncounter) {
        val trainer = encounter.enemyTrainerName.orEmpty().ifBlank { string(R.string.unknown, emptyArray()) }
        phase(runId, BattlePhase.IntroMessage)
        update(runId) { copy(playerPanel = BattlePanel.Roster, enemyPanel = BattlePanel.Roster) }
        typeMessage(runId, string(R.string.battle_trainer_challenge, arrayOf(trainer)))
        pause(timing.introHoldMillis)
        update(runId) { copy(playerPanel = BattlePanel.Hidden, enemyPanel = BattlePanel.Hidden, message = "") }
        phase(runId, BattlePhase.EnemyTrainerLeaving)
        awaitAnimation(runId, BattlePhase.EnemyTrainerLeaving)
        val enemyName = encounter.enemy?.name.orEmpty().ifBlank { string(R.string.unknown, emptyArray()) }
        typeMessage(runId, string(R.string.battle_trainer_sent_out, arrayOf(trainer, enemyName.uppercase())))
        pause(timing.panelHoldMillis)
        phase(runId, BattlePhase.EnemyRevealing)
        reveal(runId, player = false)
        if (encounter.enemy?.isRadiant == true) showRadiance(runId, player = false)
        phase(runId, BattlePhase.EnemyReady)
        update(runId) { copy(enemyPanel = BattlePanel.Monster) }
    }

    private suspend fun runWildIntro(runId: Long, encounter: BattleEncounter, radiant: Boolean) {
        phase(runId, BattlePhase.IntroMessage)
        if (radiant) showRadiance(runId, player = false)
        val enemyName = encounter.enemy?.name.orEmpty().ifBlank { string(R.string.unknown, emptyArray()) }.uppercase()
        val text = string(if (radiant) R.string.battle_wild_radiant_appeared else R.string.battle_wild_appeared, arrayOf(enemyName))
        update(runId) { copy(playerPanel = BattlePanel.Roster) }
        typeMessage(runId, text)
        pause(timing.introHoldMillis)
        update(runId) { copy(playerPanel = BattlePanel.Hidden, message = "") }
        phase(runId, BattlePhase.EnemyReady)
        update(runId) { copy(enemyPanel = BattlePanel.Monster) }
    }

    private suspend fun reveal(runId: Long, player: Boolean) {
        for (frame in 1..4) {
            update(runId) {
                if (player) copy(playerRevealFrame = frame) else copy(enemyRevealFrame = frame)
            }
            pause(timing.revealFrameMillis)
        }
    }

    private suspend fun showRadiance(runId: Long, player: Boolean) {
        update(runId) {
            if (player) copy(showPlayerRadiance = true) else copy(showEnemyRadiance = true)
        }
        pause(timing.radianceMillis)
        update(runId) {
            if (player) copy(showPlayerRadiance = false) else copy(showEnemyRadiance = false)
        }
    }

    private suspend fun typeMessage(runId: Long, text: String) {
        val dialogueId = mutableState.value.dialogueId + 1
        update(runId) { copy(message = text, dialogueId = dialogueId, isTyping = true) }
        if (timing.characterMillis == 0L && timing.dialoguePageHoldMillis == 0L) {
            update(runId) { copy(isTyping = false) }
            return
        }
        awaitDialogue(runId, dialogueId)
        update(runId) { copy(isTyping = false) }
    }

    private fun phase(runId: Long, phase: BattlePhase) = update(runId) { copy(phase = phase) }

    private fun update(runId: Long, transform: BattleUiState.() -> BattleUiState) {
        if (mutableState.value.runId == runId) mutableState.value = mutableState.value.transform()
    }

    private suspend fun awaitAnimation(runId: Long, phase: BattlePhase) {
        if (durationFor(phase) == 0) return
        while (true) {
            val completion = animationCompletions.receive()
            if (completion.runId == runId && completion.phase == phase) return
        }
    }

    private suspend fun awaitDialogue(runId: Long, dialogueId: Long) {
        while (true) {
            val completion = dialogueCompletions.receive()
            if (completion.runId == runId && completion.dialogueId == dialogueId) return
        }
    }

    private fun durationFor(phase: BattlePhase) = when (phase) {
        BattlePhase.TrainersEntering -> timing.trainerEnterMillis
        BattlePhase.TrainersColorizing -> timing.colorizeMillis
        BattlePhase.EnemyTrainerLeaving, BattlePhase.PlayerTrainerLeaving -> timing.trainerExitMillis
        else -> 0
    }

    private data class AnimationCompletion(val runId: Long, val phase: BattlePhase)
    private data class DialogueCompletion(val runId: Long, val dialogueId: Long)
}
