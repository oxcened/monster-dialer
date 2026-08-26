package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BattleSequenceCoordinatorTest {
    @Test
    fun trainerBattleProgressesThroughExpectedPhases() = runTest {
        val timing = BattleTiming.Instant.copy(
            trainerEnterMillis = 1,
            colorizeMillis = 1,
            trainerExitMillis = 1
        )
        val coordinator = coordinator(this, timing)

        coordinator.start(encounter("first", EncounterType.Trainer))
        runCurrent()
        val runId = coordinator.state.value.runId
        assertEquals(BattlePhase.TrainersEntering, coordinator.state.value.phase)

        coordinator.animationCompleted(runId, BattlePhase.TrainersEntering)
        runCurrent()
        assertEquals(BattlePhase.TrainersColorizing, coordinator.state.value.phase)

        coordinator.animationCompleted(runId, BattlePhase.TrainersColorizing)
        runCurrent()
        assertEquals(BattlePhase.EnemyTrainerLeaving, coordinator.state.value.phase)

        coordinator.animationCompleted(runId, BattlePhase.EnemyTrainerLeaving)
        runCurrent()
        assertEquals(BattlePhase.PlayerTrainerLeaving, coordinator.state.value.phase)

        coordinator.animationCompleted(runId, BattlePhase.PlayerTrainerLeaving)
        advanceUntilIdle()

        assertEquals(BattlePhase.Ready, coordinator.state.value.phase)
        assertEquals("What will you do?", coordinator.state.value.message)
        assertEquals(BattlePanel.Monster, coordinator.state.value.enemyPanel)
        assertEquals(BattlePanel.Monster, coordinator.state.value.playerPanel)
    }

    @Test
    fun startingNewBattleCancelsOldSequence() = runTest {
        val timing = BattleTiming.Instant.copy(characterMillis = 100, introHoldMillis = 1_000)
        val coordinator = coordinator(this, timing)
        coordinator.start(encounter("old", EncounterType.Trainer))
        runCurrent()
        advanceTimeBy(200)

        coordinator.start(encounter("new", EncounterType.Anonymous))
        advanceUntilIdle()

        val state = coordinator.state.value
        assertEquals("new", state.encounter?.id)
        assertEquals(EncounterType.Anonymous, state.encounter?.type)
        assertEquals(BattlePhase.Ready, state.phase)
        assertFalse(state.isTyping)
    }

    @Test
    fun stopCancelsSequenceAndReturnsToIdle() = runTest {
        val timing = BattleTiming.Instant.copy(characterMillis = 100)
        val coordinator = coordinator(this, timing)
        coordinator.start(encounter("battle", EncounterType.RadiantWild))
        runCurrent()

        coordinator.stop()
        advanceUntilIdle()

        assertEquals(BattlePhase.Idle, coordinator.state.value.phase)
        assertEquals(null, coordinator.state.value.encounter)
    }

    private fun encounter(id: String, type: EncounterType) = BattleEncounter(
        id = id,
        type = type,
        player = monster("Player"),
        enemy = monster(if (type == EncounterType.Anonymous) "Unknown" else "Enemy"),
        enemyTrainerName = "Alex",
        playerTrainerSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_player_trainer),
        enemyTrainerSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_enemy_trainer)
    )

    private fun monster(name: String) = BattleMonster(
        name = name,
        level = 5,
        hp = 20,
        maxHp = 20,
        frontSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_enemy_monster),
        backSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_player_monster)
    )

    private fun coordinator(scope: CoroutineScope, timing: BattleTiming) = BattleSequenceCoordinator(scope, timing, ::string)

    private fun string(resource: Int, arguments: Array<out Any>) = when (resource) {
        R.string.unknown -> "Unknown"
        R.string.battle_prompt -> "What will you do?"
        R.string.battle_player_send_out -> "Go! ${arguments[0]}!"
        R.string.battle_trainer_challenge -> "Trainer ${arguments[0]} wants to battle!"
        R.string.battle_trainer_sent_out -> "Trainer ${arguments[0]} sent out ${arguments[1]}!"
        R.string.battle_wild_radiant_appeared -> "Wild radiant ${arguments[0]} appeared!"
        R.string.battle_wild_appeared -> "Wild ${arguments[0]} appeared!"
        else -> error("Unexpected resource $resource")
    }
}
