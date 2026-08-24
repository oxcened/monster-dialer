package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.R

object BattleEncounterFactory {
    fun forCall(callId: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val player = BattlePokemon(
            name = "Bulbasaur",
            level = 5,
            hp = 20,
            maxHp = 20,
            frontSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_enemy_pokemon, "battle_enemy_pokemon"),
            backSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_player_pokemon, "battle_player_pokemon")
        )
        val enemy = if (isAnonymous) {
            BattlePokemon(
                name = "MissingNo.",
                level = 236,
                hp = 33,
                maxHp = 33,
                frontSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_missing_no, "battle_missing_no"),
                backSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_missing_no, "battle_missing_no")
            )
        } else {
            player.copy(name = "Bulbasaur", level = 5)
        }
        return BattleEncounter(
            id = callId,
            type = if (isAnonymous) EncounterType.Anonymous else EncounterType.Trainer,
            player = player,
            enemy = enemy,
            enemyTrainerName = callerName.takeUnless { isAnonymous },
            playerTrainerSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_player_trainer, "battle_player_trainer"),
            enemyTrainerSprite = BattleVisualAsset.AppDrawable(
                if (isAnonymous) R.drawable.battle_missing_no else R.drawable.battle_enemy_trainer,
                if (isAnonymous) "battle_missing_no" else "battle_enemy_trainer"
            )
        )
    }

    fun preview(type: EncounterType): BattleEncounter {
        val encounter = forCall(type.name, "Red", type == EncounterType.Anonymous)
        return if (type == EncounterType.ShinyWild) {
            encounter.copy(
                type = type,
                enemyTrainerName = null,
                enemy = encounter.enemy?.copy(name = "Bulbasaur", isShiny = true),
                enemyTrainerSprite = BattleVisualAsset.AppDrawable(R.drawable.battle_enemy_pokemon, "battle_enemy_pokemon")
            )
        } else encounter.copy(type = type)
    }
}
