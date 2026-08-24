package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.R

object BattleEncounterFactory {
    fun forCall(callId: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val player = BattleMonster(
            name = DefaultMonsterName,
            level = 5,
            hp = 20,
            maxHp = 20,
            frontSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_enemy_monster, "battle_enemy_monster"),
            backSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_player_monster, "battle_player_monster")
        )
        val enemy = if (isAnonymous) {
            BattleMonster(
                name = "Unknown",
                level = 236,
                hp = 33,
                maxHp = 33,
                frontSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_unknown_monster, "battle_unknown_monster"),
                backSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_unknown_monster, "battle_unknown_monster")
            )
        } else {
            player.copy(name = DefaultMonsterName, level = 5)
        }
        return BattleEncounter(
            id = callId,
            type = if (isAnonymous) EncounterType.Anonymous else EncounterType.Trainer,
            player = player,
            enemy = enemy,
            enemyTrainerName = callerName.takeUnless { isAnonymous },
            playerTrainerSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_player_trainer, "battle_player_trainer"),
            enemyTrainerSprite = BattleVisualAsset.AppDrawable(
                if (isAnonymous) R.drawable.battle_unknown_monster else R.drawable.battle_enemy_trainer,
                if (isAnonymous) "battle_unknown_monster" else "battle_enemy_trainer"
            )
        )
    }

    fun preview(type: EncounterType): BattleEncounter {
        val encounter = forCall(type.name, "Alex", type == EncounterType.Anonymous)
        return if (type == EncounterType.ShinyWild) {
            encounter.copy(
                type = type,
                enemyTrainerName = null,
                enemy = encounter.enemy?.copy(name = DefaultMonsterName, isShiny = true),
                enemyTrainerSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_enemy_monster, "battle_enemy_monster")
            )
        } else encounter.copy(type = type)
    }

    private const val DefaultMonsterName = "Shelkurl"
}
