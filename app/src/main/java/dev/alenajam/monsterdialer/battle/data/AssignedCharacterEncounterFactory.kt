package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.BuildConfig
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CharacterVisualVariant
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/** Builds a call encounter from local assignments, retaining the bundled fallback at all times. */
class AssignedCharacterEncounterFactory(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository,
    private val radiantUnlocks: RadiantVariantUnlockStore,
    private val random: Random = Random.Default,
) {
    private var cachedCall: CallKey? = null
    private var cachedEncounter: BattleEncounter? = null

    fun forCall(callId: String, contactKey: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val call = CallKey(callId, contactKey, callerName, isAnonymous)
        if (cachedCall == call) return requireNotNull(cachedEncounter)

        val fallback = BattleEncounterFactory.forCall(callId, callerName, isAnonymous)
        
        // Note: Using runBlocking here because forCall is called from Composable composition/remember 
        // and currently the repository uses suspend functions. In a full architecture, 
        // the encounter might be part of the ViewModel state.
        return runBlocking {
            val player = assignmentRepository.getPlayerCharacter(CharacterType.Monster)
                ?.let { reference -> charactersRepository.findCharacter(reference, CharacterAssignmentTarget.Player, CharacterType.Monster)?.asPlayerBattleMonster(fallback.player, reference.variantId) }
                ?: fallback.player
            
            val enemy = if (isAnonymous) {
                fallback.enemy
            } else {
                assignmentRepository.getAssignedCharacter(contactKey, CharacterType.Monster)
                    ?.let { reference -> charactersRepository.findCharacter(reference, CharacterAssignmentTarget.Contact, CharacterType.Monster)?.asContactBattleMonster(fallback.enemy ?: fallback.player, reference.variantId) }
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

            val radiantWild = randomRadiantWild()
            if (radiantWild != null && (shouldForceRadiantEncounter() || random.nextInt(RadiantEncounterDenominator) == 0)) {
                val (wildCharacter, variant) = radiantWild
                val wildMonster = wildCharacter.asBattleMonster(
                    frontSprite = requireNotNull(wildCharacter.imageFor(variant.frontImage)),
                    backSprite = null,
                    variant = variant,
                )
                val wasUnlocked = radiantUnlocks.unlock(
                    dev.alenajam.monsterdialer.packs.data.CharacterReference(
                        wildCharacter.packId,
                        wildCharacter.character.id,
                        variant.id,
                    )
                )
                return@runBlocking fallback.copy(
                    type = EncounterType.RadiantWild,
                    player = player,
                    enemy = wildMonster,
                    enemyTrainerName = null,
                    playerTrainerSprite = playerTrainer,
                    enemyTrainerSprite = wildMonster.frontSprite,
                    unlockedRadiantName = wildCharacter.character.name.takeIf { wasUnlocked },
                )
            }
            
            fallback.copy(
                player = player,
                enemy = enemy,
                playerTrainerSprite = playerTrainer,
                enemyTrainerSprite = enemyTrainer
            )
        }.also { encounter ->
            cachedCall = call
            cachedEncounter = encounter
        }
    }

    private fun randomRadiantWild(): Pair<InstalledPackCharacter, CharacterVisualVariant>? {
        val candidates = charactersRepository
            .getCharactersAssignableTo(CharacterAssignmentTarget.Contact, CharacterType.Monster)
            .flatMap { character ->
                character.character.visualVariants
                    .filter(CharacterVisualVariant::isRadiant)
                    .map { variant -> character to variant }
            }
        return candidates.randomOrNull(random)
    }

    private fun shouldForceRadiantEncounter(): Boolean =
        BuildConfig.DEBUG && BuildConfig.FORCE_RADIANT_ENCOUNTERS

    private fun InstalledPackCharacter.asPlayerBattleMonster(fallback: BattleMonster, variantId: String): BattleMonster {
        val variant = character.variant(variantId) ?: return fallback
        val packFront = imageFor(variant.frontImage)
        val packBack = imageFor(variant.backImage)
        
        return asBattleMonster(
            frontSprite = packFront ?: fallback.frontSprite,
            backSprite = packBack ?: fallback.backSprite,
            variant = variant,
        )
    }

    private fun InstalledPackCharacter.asContactBattleMonster(fallback: BattleMonster, variantId: String): BattleMonster {
        val variant = character.variant(variantId) ?: return fallback
        val packFront = imageFor(variant.frontImage)
        val packBack = imageFor(variant.backImage)
        
        return asBattleMonster(
            frontSprite = packFront ?: fallback.frontSprite,
            backSprite = packBack ?: fallback.backSprite,
            variant = variant,
        )
    }

    private fun InstalledPackCharacter.asBattleMonster(
        frontSprite: BattleVisualAsset,
        backSprite: BattleVisualAsset?,
        variant: CharacterVisualVariant,
    ) = BattleMonster(
        name = character.name,
        level = character.level ?: DefaultLevel,
        hp = character.maxHp ?: DefaultMaxHp,
        maxHp = character.maxHp ?: DefaultMaxHp,
        frontSprite = frontSprite,
        backSprite = backSprite,
        isRadiant = variant.isRadiant
    )

    private fun InstalledPackCharacter.imageFor(path: String?): BattleVisualAsset? =
        path?.let { BattleVisualAsset.LocalFile(imageFile(it).path) }

    private fun InstalledPackCharacter.playerTrainerSprite(fallback: BattleVisualAsset) =
        character.visualVariants.first().backImage?.let { BattleVisualAsset.LocalFile(imageFile(it).path) } ?: fallback

    private fun InstalledPackCharacter.contactTrainerSprite(fallback: BattleVisualAsset) =
        character.visualVariants.first().frontImage?.let { BattleVisualAsset.LocalFile(imageFile(it).path) } ?: fallback

    private companion object {
        const val RadiantEncounterDenominator = 64
        const val DefaultLevel = 5
        const val DefaultMaxHp = 20
    }

    private data class CallKey(
        val callId: String,
        val contactKey: String,
        val callerName: String,
        val isAnonymous: Boolean,
    )
}
