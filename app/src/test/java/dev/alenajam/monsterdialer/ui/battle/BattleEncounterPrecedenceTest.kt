package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.battle.data.BattleEncounterFactory
import dev.alenajam.monsterdialer.battle.data.BattleMonster
import dev.alenajam.monsterdialer.battle.data.BattleVisualAsset
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.battle.data.withOnlineOpponent
import dev.alenajam.monsterdialer.onlineprofiles.data.RemoteBattleOpponent
import org.junit.Assert.assertEquals
import org.junit.Test

class BattleEncounterPrecedenceTest {
    @Test
    fun onlineOpponentDoesNotReplaceRadiantWildEncounter() {
        val radiantEncounter = BattleEncounterFactory.preview(EncounterType.RadiantWild)

        val resolvedEncounter = radiantEncounter.withOnlineOpponent(onlineOpponent())

        assertEquals(EncounterType.RadiantWild, resolvedEncounter.type)
        assertEquals(radiantEncounter.enemy, resolvedEncounter.enemy)
        assertEquals(radiantEncounter.enemyTrainerSprite, resolvedEncounter.enemyTrainerSprite)
    }

    @Test
    fun onlineOpponentDoesNotReplaceAnonymousEncounter() {
        val anonymousEncounter = BattleEncounterFactory.preview(EncounterType.Anonymous)

        val resolvedEncounter = anonymousEncounter.withOnlineOpponent(onlineOpponent())

        assertEquals(anonymousEncounter.enemy, resolvedEncounter.enemy)
        assertEquals(anonymousEncounter.enemyTrainerSprite, resolvedEncounter.enemyTrainerSprite)
    }

    @Test
    fun onlineOpponentReplacesStandardTrainerEncounter() {
        val opponent = onlineOpponent()

        val resolvedEncounter = BattleEncounterFactory.preview(EncounterType.Trainer)
            .withOnlineOpponent(opponent)

        assertEquals(opponent.monster, resolvedEncounter.enemy)
        assertEquals(opponent.trainerSprite, resolvedEncounter.enemyTrainerSprite)
    }

    private fun onlineOpponent() = RemoteBattleOpponent(
        profileId = "profile-id",
        trainerName = "Online Trainer",
        trainerSprite = BattleVisualAsset.LocalFile("online-trainer.png"),
        monster = BattleMonster(
            name = "Online Monster",
            level = 12,
            hp = 20,
            maxHp = 20,
            frontSprite = BattleVisualAsset.LocalFile("online-monster.png"),
        ),
    )
}
