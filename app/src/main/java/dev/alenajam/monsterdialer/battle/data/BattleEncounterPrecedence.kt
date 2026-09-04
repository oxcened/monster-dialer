package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.onlineprofiles.data.RemoteBattleOpponent

/**
 * Applies an online profile only to standard trainer encounters.
 *
 * Encounter type is the authoritative precedence decision: radiant wild and anonymous
 * encounters own their opponent, artwork, dialog, and battle-journal semantics.
 */
internal fun BattleEncounter.withOnlineOpponent(opponent: RemoteBattleOpponent?): BattleEncounter {
    if (type != EncounterType.Trainer || opponent == null) return this
    return copy(
        enemy = opponent.monster,
        enemyTrainerSprite = opponent.trainerSprite,
    )
}
