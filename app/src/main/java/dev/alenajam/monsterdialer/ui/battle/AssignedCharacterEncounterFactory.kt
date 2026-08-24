package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter

/** Builds a call encounter from local assignments, retaining the bundled fallback at all times. */
class AssignedCharacterEncounterFactory(
    private val assignments: CharacterAssignmentStore,
    private val characters: CharacterPackRepository
) {
    fun forCall(callId: String, contactKey: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val fallback = BattleEncounterFactory.forCall(callId, callerName, isAnonymous)
        val player = assignments.player(CharacterType.Monster)
            ?.let { characters.find(it, CharacterAssignmentTarget.Player, CharacterType.Monster) }
            ?.asPlayerBattleMonster()
            ?: fallback.player
        val enemy = if (isAnonymous) {
            fallback.enemy
        } else {
            assignments.characterForContact(contactKey, CharacterType.Monster)
                ?.let { characters.find(it, CharacterAssignmentTarget.Contact, CharacterType.Monster) }
                ?.asContactBattleMonster()
                ?: fallback.enemy
        }
        val playerTrainer = assignments.player(CharacterType.Trainer)
            ?.let { characters.find(it, CharacterAssignmentTarget.Player, CharacterType.Trainer) }
            ?.playerTrainerSprite()
            ?: fallback.playerTrainerSprite
        val enemyTrainer = if (isAnonymous) {
            fallback.enemyTrainerSprite
        } else {
            assignments.characterForContact(contactKey, CharacterType.Trainer)
                ?.let { characters.find(it, CharacterAssignmentTarget.Contact, CharacterType.Trainer) }
                ?.contactTrainerSprite()
                ?: fallback.enemyTrainerSprite
        }
        return fallback.copy(
            player = player,
            enemy = enemy,
            playerTrainerSprite = playerTrainer,
            enemyTrainerSprite = enemyTrainer
        )
    }

    private fun InstalledPackCharacter.asPlayerBattleMonster(): BattleMonster {
        val backImage = requireNotNull(character.backImage)
        return asBattleMonster(
            frontImage = character.frontImage ?: backImage,
            backImage = backImage
        )
    }

    private fun InstalledPackCharacter.asContactBattleMonster(): BattleMonster {
        val frontImage = requireNotNull(character.frontImage)
        return asBattleMonster(
            frontImage = frontImage,
            backImage = character.backImage ?: frontImage
        )
    }

    private fun InstalledPackCharacter.asBattleMonster(
        frontImage: String,
        backImage: String
    ) = BattleMonster(
        name = character.name,
        level = character.level ?: DefaultLevel,
        hp = character.maxHp ?: DefaultMaxHp,
        maxHp = character.maxHp ?: DefaultMaxHp,
        frontSprite = BattleVisualAsset.LocalFile(imageFile(frontImage).path),
        backSprite = BattleVisualAsset.LocalFile(imageFile(backImage).path)
    )

    private fun InstalledPackCharacter.playerTrainerSprite() =
        BattleVisualAsset.LocalFile(imageFile(requireNotNull(character.backImage)).path)

    private fun InstalledPackCharacter.contactTrainerSprite() =
        BattleVisualAsset.LocalFile(imageFile(requireNotNull(character.frontImage)).path)

    private companion object {
        const val DefaultLevel = 5
        const val DefaultMaxHp = 20
    }
}
