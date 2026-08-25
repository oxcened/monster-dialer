package dev.alenajam.monsterdialer.ui.battle

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
    private val pause: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutableState = MutableStateFlow(BattleUiState())
    val state: StateFlow<BattleUiState> = mutableState.asStateFlow()

    private val animationCompletions = Channel<AnimationCompletion>(Channel.UNLIMITED)
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
        typeMessage(runId, "Go! ${encounter.player.name.uppercase()}!")
        phase(runId, BattlePhase.PlayerRevealing)
        reveal(runId, player = true)
        if (encounter.player.isRadiant) showRadiance(runId, player = true)
        update(runId) { copy(playerPanel = BattlePanel.Monster) }
        pause(timing.readyHoldMillis)
        typeMessage(runId, "What will you do?")
        phase(runId, BattlePhase.Ready)
    }

    private suspend fun runTrainerIntro(runId: Long, encounter: BattleEncounter) {
        val trainer = encounter.enemyTrainerName.orEmpty().ifBlank { "Unknown" }
        phase(runId, BattlePhase.IntroMessage)
        update(runId) { copy(playerPanel = BattlePanel.Roster, enemyPanel = BattlePanel.Roster) }
        typeMessage(runId, "Trainer $trainer wants to battle!")
        pause(timing.introHoldMillis)
        update(runId) { copy(playerPanel = BattlePanel.Hidden, enemyPanel = BattlePanel.Hidden, message = "") }
        phase(runId, BattlePhase.EnemyTrainerLeaving)
        awaitAnimation(runId, BattlePhase.EnemyTrainerLeaving)
        val enemyName = encounter.enemy?.name.orEmpty().ifBlank { "Unknown" }
        typeMessage(runId, "Trainer $trainer sent out ${enemyName.uppercase()}!")
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
        val enemyName = encounter.enemy?.name.orEmpty().ifBlank { "Unknown" }.uppercase()
        val text = if (radiant) "Wild radiant $enemyName appeared!" else "Wild $enemyName appeared!"
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
        update(runId) { copy(message = "", isTyping = true) }
        text.forEachIndexed { index, _ ->
            update(runId) { copy(message = text.take(index + 1)) }
            pause(timing.characterMillis)
        }
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

    private fun durationFor(phase: BattlePhase) = when (phase) {
        BattlePhase.TrainersEntering -> timing.trainerEnterMillis
        BattlePhase.TrainersColorizing -> timing.colorizeMillis
        BattlePhase.EnemyTrainerLeaving, BattlePhase.PlayerTrainerLeaving -> timing.trainerExitMillis
        else -> 0
    }

    private data class AnimationCompletion(val runId: Long, val phase: BattlePhase)
}
