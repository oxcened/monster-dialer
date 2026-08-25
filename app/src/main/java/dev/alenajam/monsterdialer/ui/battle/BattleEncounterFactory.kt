package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.characters.BuiltInArtwork
import dev.alenajam.monsterdialer.characters.BuiltInCharacters

object BattleEncounterFactory {
    fun forCall(callId: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val defaultMonster = BuiltInCharacters.monster
        val anonymousMonster = BuiltInCharacters.anonymousMonster
        val player = BattleMonster(
            name = defaultMonster.character.name,
            level = defaultMonster.level,
            hp = defaultMonster.maxHp,
            maxHp = defaultMonster.maxHp,
            frontSprite = defaultMonster.character.contactArtwork.asBattleVisualAsset(),
            backSprite = defaultMonster.character.playerArtwork.asBattleVisualAsset()
        )
        val enemy = if (isAnonymous) {
            BattleMonster(
                name = anonymousMonster.name,
                level = anonymousMonster.level,
                hp = anonymousMonster.maxHp,
                maxHp = anonymousMonster.maxHp,
                frontSprite = anonymousMonster.enemyArtwork.asBattleVisualAsset()
            )
        } else {
            player.copy(name = defaultMonster.character.name, level = defaultMonster.level)
        }
        return BattleEncounter(
            id = callId,
            type = if (isAnonymous) EncounterType.Anonymous else EncounterType.Trainer,
            player = player,
            enemy = enemy,
            enemyTrainerName = callerName.takeUnless { isAnonymous },
            playerTrainerSprite = BuiltInCharacters.trainer.playerArtwork.asBattleVisualAsset(),
            enemyTrainerSprite = if (isAnonymous) {
                anonymousMonster.enemyArtwork.asBattleVisualAsset()
            } else {
                BuiltInCharacters.trainer.contactArtwork.asBattleVisualAsset()
            }
        )
    }

    fun preview(type: EncounterType): BattleEncounter {
        val encounter = forCall(type.name, "Alex", type == EncounterType.Anonymous)
        return if (type == EncounterType.ShinyWild) {
            encounter.copy(
                type = type,
                enemyTrainerName = null,
                enemy = encounter.enemy?.copy(name = BuiltInCharacters.monster.character.name, isShiny = true),
                enemyTrainerSprite = BuiltInCharacters.monster.character.contactArtwork.asBattleVisualAsset()
            )
        } else encounter.copy(type = type)
    }

    private fun BuiltInArtwork.asBattleVisualAsset() =
        BattleVisualAsset.AppDrawable(resource, resourceName)
}
