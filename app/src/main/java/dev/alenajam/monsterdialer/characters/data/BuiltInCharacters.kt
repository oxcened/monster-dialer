package dev.alenajam.monsterdialer.characters.data

import androidx.annotation.DrawableRes
import dev.alenajam.monsterdialer.R

/** Metadata for a bundled character and its perspective-specific artwork. */
data class BuiltInCharacter(
    val name: String,
    val playerArtwork: BuiltInArtwork,
    val contactArtwork: BuiltInArtwork
)

data class BuiltInMonster(
    val character: BuiltInCharacter,
    val level: Int,
    val maxHp: Int
)

/** A bundled monster that can only appear as the opposing character. */
data class BuiltInEnemyMonster(
    val name: String,
    val level: Int,
    val maxHp: Int,
    val enemyArtwork: BuiltInArtwork
)

data class BuiltInArtwork(
    @param:DrawableRes val resource: Int,
    val resourceName: String
)

/** The app's fallback characters, used whenever no character pack assignment applies. */
object BuiltInCharacters {
    val trainer = BuiltInCharacter(
        name = "Kael",
        playerArtwork = BuiltInArtwork(R.drawable.battle_player_trainer, "battle_player_trainer"),
        contactArtwork = BuiltInArtwork(R.drawable.battle_enemy_trainer, "battle_enemy_trainer")
    )

    val monster = BuiltInMonster(
        character = BuiltInCharacter(
            name = "Plumguard",
            playerArtwork = BuiltInArtwork(R.drawable.battle_player_monster, "battle_player_monster"),
            contactArtwork = BuiltInArtwork(R.drawable.battle_enemy_monster, "battle_enemy_monster")
        ),
        level = 5,
        maxHp = 20
    )

    val anonymousMonster = BuiltInEnemyMonster(
        name = "Nullith",
        level = 236,
        maxHp = 33,
        enemyArtwork = BuiltInArtwork(R.drawable.battle_unknown_monster, "battle_unknown_monster")
    )
}
