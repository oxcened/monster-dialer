package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.runBlocking

/** Builds a call encounter from local assignments, retaining the bundled fallback at all times. */
class AssignedCharacterEncounterFactory(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository
) {
    fun forCall(callId: String, contactKey: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val fallback = BattleEncounterFactory.forCall(callId, callerName, isAnonymous)
        
        // Note: Using runBlocking here because forCall is called from Composable composition/remember 
        // and currently the repository uses suspend functions. In a full architecture, 
        // the encounter might be part of the ViewModel state.
        return runBlocking {
            val player = assignmentRepository.getPlayerCharacter(CharacterType.Monster)
                ?.let { charactersRepository.findCharacter(it, CharacterAssignmentTarget.Player, CharacterType.Monster) }
                ?.asPlayerBattleMonster()
                ?: fallback.player
            
            val enemy = if (isAnonymous) {
                fallback.enemy
            } else {
                assignmentRepository.getAssignedCharacter(contactKey, CharacterType.Monster)
                    ?.let { charactersRepository.findCharacter(it, CharacterAssignmentTarget.Contact, CharacterType.Monster) }
                    ?.asContactBattleMonster()
                    ?: fallback.enemy
            }
            
            val playerTrainer = assignmentRepository.getPlayerCharacter(CharacterType.Trainer)
                ?.let { charactersRepository.findCharacter(it, CharacterAssignmentTarget.Player, CharacterType.Trainer) }
                ?.playerTrainerSprite()
                ?: fallback.playerTrainerSprite
            
            val enemyTrainer = if (isAnonymous) {
                fallback.enemyTrainerSprite
            } else {
                assignmentRepository.getAssignedCharacter(contactKey, CharacterType.Trainer)
                    ?.let { charactersRepository.findCharacter(it, CharacterAssignmentTarget.Contact, CharacterType.Trainer) }
                    ?.contactTrainerSprite()
                    ?: fallback.enemyTrainerSprite
            }
            
            fallback.copy(
                player = player,
                enemy = enemy,
                playerTrainerSprite = playerTrainer,
                enemyTrainerSprite = enemyTrainer
            )
        }
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
        backSprite = BattleVisualAsset.LocalFile(imageFile(backImage).path),
        isRadiant = character.isRadiant
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
