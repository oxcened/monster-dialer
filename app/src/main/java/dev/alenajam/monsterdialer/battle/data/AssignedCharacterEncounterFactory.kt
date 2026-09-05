package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.BuildConfig
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.ContactCharacterMode
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.DefaultMonsterLevel
import dev.alenajam.monsterdialer.characters.data.PlayerProfileStatsStore
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CharacterVisualVariant
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineOpponentResolver
import dev.alenajam.monsterdialer.onlineprofiles.data.RemoteBattleOpponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/** Builds a call encounter from local assignments, retaining the bundled fallback at all times. */
@Singleton
class AssignedCharacterEncounterFactory @Inject constructor(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository,
    private val radiantUnlocks: RadiantVariantUnlockStore,
    private val activeEncounterStore: ActiveBattleEncounterStore,
    private val profileStatsStore: PlayerProfileStatsStore,
    private val battleJournalStore: BattleJournalStore,
    private val onlineOpponentResolver: OnlineOpponentResolver? = null,
) {
    private var random: Random = Random.Default

    constructor(
        charactersRepository: CharactersRepository,
        assignmentRepository: CharacterAssignmentRepository,
        radiantUnlocks: RadiantVariantUnlockStore,
        activeEncounterStore: ActiveBattleEncounterStore,
        profileStatsStore: PlayerProfileStatsStore,
        battleJournalStore: BattleJournalStore,
        random: Random,
    ) : this(charactersRepository, assignmentRepository, radiantUnlocks, activeEncounterStore, profileStatsStore, battleJournalStore) {
        this.random = random
    }

    private var cachedCall: ActiveCallKey? = null
    private var cachedEncounter: BattleEncounter? = null

    /** Ends the current call session so the next call receives a fresh encounter roll. */
    fun clearCachedEncounter() {
        cachedCall = null
        cachedEncounter = null
        activeEncounterStore.clear()
    }

    fun forCall(callId: String, contactKey: String, callerName: String, isAnonymous: Boolean): BattleEncounter {
        val call = ActiveCallKey(callId, contactKey, callerName, isAnonymous)
        if (cachedCall == call) {
            return requireNotNull(cachedEncounter).withOnlineOpponent(
                if (isAnonymous) null else onlineOpponentResolver?.cachedOpponentForNumber(contactKey)
            )
        }

        val fallback = BattleEncounterFactory.forCall(callId, callerName, isAnonymous)
        val onlineOpponent = if (isAnonymous) null else onlineOpponentResolver?.cachedOpponentForNumber(contactKey)
        
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
                contactCharacterReference(contactKey, CharacterType.Monster)
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
                contactCharacterReference(contactKey, CharacterType.Trainer)
                    ?.let { reference ->
                        charactersRepository.findCharacter(reference, CharacterAssignmentTarget.Contact, CharacterType.Trainer)
                            ?.contactTrainerSprite(fallback.enemyTrainerSprite, reference.variantId)
                    }
                ?: fallback.enemyTrainerSprite
            }

            val regularEncounter = fallback.copy(
                player = player,
                enemy = enemy,
                playerTrainerSprite = playerTrainer,
                enemyTrainerSprite = enemyTrainer,
            )
            activeEncounterStore.restore(call)?.let { savedEncounter ->
                val reference = savedEncounter.radiantReference ?: return@runBlocking regularEncounter
                val character = charactersRepository.findCharacter(
                    reference,
                    CharacterAssignmentTarget.Contact,
                    CharacterType.Monster,
                ) ?: return@runBlocking regularEncounter
                val variant = character.character.variant(reference.variantId)?.takeIf(CharacterVisualVariant::isRadiant)
                    ?: return@runBlocking regularEncounter
                return@runBlocking radiantWildEncounter(
                    fallback = fallback,
                    player = player,
                    playerTrainer = playerTrainer,
                    character = character,
                    variant = variant,
                    wasUnlocked = false,
                )
            }

            val radiantWild = randomRadiantWild()
            if (radiantWild != null && (shouldForceRadiantEncounter() || random.nextInt(RadiantEncounterDenominator) == 0)) {
                val (wildCharacter, variant) = radiantWild
                val reference = CharacterReference(
                    wildCharacter.packId,
                    wildCharacter.character.id,
                    variant.id,
                )
                val wasUnlocked = radiantUnlocks.unlock(
                    reference,
                )
                val encounter = radiantWildEncounter(
                    fallback = fallback,
                    player = player,
                    playerTrainer = playerTrainer,
                    character = wildCharacter,
                    variant = variant,
                    wasUnlocked = wasUnlocked,
                )
                saveEncounter(
                    call,
                    reference,
                    encounter,
                    isRadiantDiscovery = wasUnlocked,
                )
                return@runBlocking encounter
            }

            val resolvedRegularEncounter = regularEncounter.withOnlineOpponent(onlineOpponent)
            saveEncounter(
                call,
                radiantReference = null,
                encounter = resolvedRegularEncounter,
                isRadiantDiscovery = false,
            )
            resolvedRegularEncounter
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

    private suspend fun contactCharacterReference(contactKey: String, type: CharacterType): CharacterReference? {
        val selection = assignmentRepository.getContactCharacterSelection(contactKey, type)
        if (selection.character != null) return selection.character
        if (selection.mode != ContactCharacterMode.Random) return null
        return randomContactCharacter(type)
    }

    private suspend fun randomContactCharacter(type: CharacterType): CharacterReference? {
        val configuredPool = assignmentRepository.getContactRandomPool(type)?.toSet()
        val candidates = charactersRepository
            .getCharactersAssignableTo(CharacterAssignmentTarget.Contact, type)
            .flatMap { character ->
                character.character.visualVariants
                    .filterNot(CharacterVisualVariant::isRadiant)
                    .map { variant -> CharacterReference(character.packId, character.character.id, variant.id) }
            }
            .filter { configuredPool == null || it in configuredPool }
        return candidates.randomOrNull(random)
    }

    private fun shouldForceRadiantEncounter(): Boolean =
        BuildConfig.DEBUG && BuildConfig.FORCE_RADIANT_ENCOUNTERS

    private fun saveEncounter(
        call: ActiveCallKey,
        radiantReference: CharacterReference?,
        encounter: BattleEncounter,
        isRadiantDiscovery: Boolean,
    ) {
        activeEncounterStore.save(call, radiantReference)
        profileStatsStore.recordBattle()
        battleJournalStore.record(encounter, isRadiantDiscovery)
    }

    private fun radiantWildEncounter(
        fallback: BattleEncounter,
        player: BattleMonster,
        playerTrainer: BattleVisualAsset,
        character: InstalledPackCharacter,
        variant: CharacterVisualVariant,
        wasUnlocked: Boolean,
    ): BattleEncounter {
        val wildFrontSprite = requireNotNull(character.imageFor(variant.frontImage))
        val wildMonster = character.asBattleMonster(
            frontSprite = wildFrontSprite,
            backSprite = null,
            variant = variant,
        )
        return fallback.copy(
            type = EncounterType.RadiantWild,
            player = player,
            enemy = wildMonster,
            // The radiant monster is wild, but the journal should still retain the caller that
            // triggered this discovery. The battle renderer does not display this as a trainer.
            enemyTrainerName = fallback.enemyTrainerName,
            playerTrainerSprite = playerTrainer,
            enemyTrainerSprite = wildMonster.frontSprite,
            unlockedRadiantName = character.character.name.takeIf { wasUnlocked },
            unlockedRadiantFrontSpritePath = (wildFrontSprite as? BattleVisualAsset.LocalFile)?.path,
        )
    }

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
        level = character.level ?: DefaultMonsterLevel,
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

    private fun InstalledPackCharacter.contactTrainerSprite(fallback: BattleVisualAsset, variantId: String) =
        character.variant(variantId)?.frontImage?.let { BattleVisualAsset.LocalFile(imageFile(it).path) } ?: fallback

    private companion object {
        const val RadiantEncounterDenominator = 64
        const val DefaultMaxHp = 20
    }

}
