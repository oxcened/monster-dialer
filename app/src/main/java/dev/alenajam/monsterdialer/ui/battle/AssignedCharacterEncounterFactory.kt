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
            ?.asBattlePokemon()
            ?: fallback.player
        val enemy = if (isAnonymous) {
            fallback.enemy
        } else {
            assignments.characterForContact(contactKey, CharacterType.Monster)
                ?.let { characters.find(it, CharacterAssignmentTarget.Contact, CharacterType.Monster) }
                ?.asBattlePokemon()
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

    private fun InstalledPackCharacter.asBattlePokemon() = BattlePokemon(
        name = character.name,
        level = 5,
        hp = 20,
        maxHp = 20,
        frontSprite = BattleVisualAsset.LocalFile(imageFile(character.frontImage).path),
        backSprite = BattleVisualAsset.LocalFile(imageFile(character.backImage ?: character.frontImage).path)
    )

    private fun InstalledPackCharacter.playerTrainerSprite() =
        BattleVisualAsset.LocalFile(imageFile(requireNotNull(character.backImage)).path)

    private fun InstalledPackCharacter.contactTrainerSprite() =
        BattleVisualAsset.LocalFile(imageFile(character.frontImage).path)
}
