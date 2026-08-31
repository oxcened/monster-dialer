package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CharacterVariant
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
                ?.let { reference -> charactersRepository.findCharacter(reference, CharacterAssignmentTarget.Player, CharacterType.Monster)?.asPlayerBattleMonster(fallback.player, reference.variant) }
                ?: fallback.player
            
            val enemy = if (isAnonymous) {
                fallback.enemy
            } else {
                assignmentRepository.getAssignedCharacter(contactKey, CharacterType.Monster)
                    ?.let { reference -> charactersRepository.findCharacter(reference, CharacterAssignmentTarget.Contact, CharacterType.Monster)?.asContactBattleMonster(fallback.enemy ?: fallback.player, reference.variant) }
                    ?: fallback.enemy
            }
            
            val playerTrainer = assignmentRepository.getPlayerCharacter(CharacterType.Trainer)
                ?.let { charactersRepository.findCharacter(it, CharacterAssignmentTarget.Player, CharacterType.Trainer) }
                ?.playerTrainerSprite(fallback.playerTrainerSprite)
                ?: fallback.playerTrainerSprite
            
            val enemyTrainer = if (isAnonymous) {
                fallback.enemyTrainerSprite
            } else {
                assignmentRepository.getAssignedCharacter(contactKey, CharacterType.Trainer)
                    ?.let { charactersRepository.findCharacter(it, CharacterAssignmentTarget.Contact, CharacterType.Trainer) }
                    ?.contactTrainerSprite(fallback.enemyTrainerSprite)
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

    private fun InstalledPackCharacter.asPlayerBattleMonster(fallback: BattleMonster, variant: CharacterVariant): BattleMonster {
        val packFront = imageFor(variant, character.frontImage, character.radiantFrontImage)
        val packBack = imageFor(variant, character.backImage, character.radiantBackImage)
        
        return asBattleMonster(
            frontSprite = packFront ?: fallback.frontSprite,
            backSprite = packBack ?: fallback.backSprite,
            variant = variant,
        )
    }

    private fun InstalledPackCharacter.asContactBattleMonster(fallback: BattleMonster, variant: CharacterVariant): BattleMonster {
        val packFront = imageFor(variant, character.frontImage, character.radiantFrontImage)
        val packBack = imageFor(variant, character.backImage, character.radiantBackImage)
        
        return asBattleMonster(
            frontSprite = packFront ?: fallback.frontSprite,
            backSprite = packBack ?: fallback.backSprite,
            variant = variant,
        )
    }

    private fun InstalledPackCharacter.asBattleMonster(
        frontSprite: BattleVisualAsset,
        backSprite: BattleVisualAsset?,
        variant: CharacterVariant,
    ) = BattleMonster(
        name = character.name,
        level = character.level ?: DefaultLevel,
        hp = character.maxHp ?: DefaultMaxHp,
        maxHp = character.maxHp ?: DefaultMaxHp,
        frontSprite = frontSprite,
        backSprite = backSprite,
        isRadiant = character.isRadiant || variant == CharacterVariant.Radiant
    )

    private fun InstalledPackCharacter.imageFor(variant: CharacterVariant, regular: String?, radiant: String?): BattleVisualAsset? =
        (if (variant == CharacterVariant.Radiant) radiant else regular)
            ?.let { BattleVisualAsset.LocalFile(imageFile(it).path) }

    private fun InstalledPackCharacter.playerTrainerSprite(fallback: BattleVisualAsset) =
        character.backImage?.let { BattleVisualAsset.LocalFile(imageFile(it).path) } ?: fallback

    private fun InstalledPackCharacter.contactTrainerSprite(fallback: BattleVisualAsset) =
        character.frontImage?.let { BattleVisualAsset.LocalFile(imageFile(it).path) } ?: fallback

    private companion object {
        const val DefaultLevel = 5
        const val DefaultMaxHp = 20
    }
}
