package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.DrawableRes
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.PlayerProfileStatsStore
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import java.io.File
import dev.alenajam.monsterdialer.packs.data.CharacterType
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CharacterSettingsSummaryViewModel @Inject constructor(
    private val assignmentRepository: CharacterAssignmentRepository,
    private val charactersRepository: CharactersRepository,
    profileStatsStore: PlayerProfileStatsStore,
    radiantUnlocks: RadiantVariantUnlockStore,
) : ViewModel() {

    val playerProfile: StateFlow<PlayerProfile> = assignmentRepository.assignmentVersion
        .map { playerProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultPlayerProfile())

    val playerCharacterNames: StateFlow<List<String>> = assignmentRepository.assignmentVersion
        .map {
            CharacterType.entries.mapNotNull { type ->
                val reference = assignmentRepository.getPlayerCharacter(type) ?: return@mapNotNull null
                charactersRepository.findCharacter(
                    reference = reference,
                    role = CharacterAssignmentTarget.Player,
                    type = type
                )?.character?.name
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val assignedContactCount: StateFlow<Int> = assignmentRepository.assignmentVersion
        .map { assignmentRepository.assignedContactCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val profileMetrics: StateFlow<ProfileMetrics> = combine(
        profileStatsStore.callsBattled,
        charactersRepository.observeCharactersAssignableTo(CharacterAssignmentTarget.Player),
        radiantUnlocks.unlocked,
    ) { callsBattled, characters, unlockedRadiants ->
        ProfileMetrics(
            callsBattled = callsBattled,
            charactersCollected = characters
                .map { character -> character.packId to character.character.id }
                .distinct()
                .size,
            radiantsFound = unlockedRadiants.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileMetrics())

    fun reorderPlayerMonsterRoster(roster: List<CharacterReference>) {
        viewModelScope.launch {
            assignmentRepository.setPlayerMonsterRoster(roster)
        }
    }

    fun removePlayerMonsterFromRoster(reference: CharacterReference) {
        viewModelScope.launch {
            assignmentRepository.removePlayerMonsterFromRoster(reference)
        }
    }

    private suspend fun playerProfile(): PlayerProfile {
        val activeMonsterReference = assignmentRepository.getPlayerCharacter(CharacterType.Monster)
        val activeMonster = activeMonsterReference
            ?.let { playerProfileCharacter(CharacterType.Monster, it) }
            ?: builtInProfileCharacter(CharacterType.Monster)
        // The roster is the full team, active monster first, so the active monster also
        // appears in it at index 0.
        val roster = assignmentRepository.getPlayerMonsterRoster()
            .mapNotNull { reference ->
                playerProfileCharacter(CharacterType.Monster, reference)?.let { character ->
                    PlayerRosterMonster(
                        character = character,
                        reference = reference,
                        isActive = reference == activeMonsterReference,
                    )
                }
            }
        return PlayerProfile(
            trainer = playerProfileCharacter(CharacterType.Trainer),
            monster = activeMonster,
            roster = roster,
        )
    }

    private suspend fun playerProfileCharacter(type: CharacterType): PlayerProfileCharacter {
        val reference = assignmentRepository.getPlayerCharacter(type) ?: return builtInProfileCharacter(type)
        return playerProfileCharacter(type, reference) ?: builtInProfileCharacter(type)
    }

    private suspend fun playerProfileCharacter(
        type: CharacterType,
        reference: CharacterReference,
    ): PlayerProfileCharacter? {
        val installed = charactersRepository.findCharacter(
            reference = reference,
            role = CharacterAssignmentTarget.Player,
            type = type
        ) ?: return null
        val artwork = installed.character.variant(reference.variantId)?.let { variant ->
            variant.frontImage ?: variant.backImage
        } ?: return null
        return PlayerProfileCharacter(
            name = installed.character.name,
            artwork = installed.imageFile(artwork),
            level = installed.character.level,
        )
    }

    private fun defaultPlayerProfile() = PlayerProfile(
        trainer = builtInProfileCharacter(CharacterType.Trainer),
        monster = builtInProfileCharacter(CharacterType.Monster),
        roster = emptyList(),
    )

    private fun builtInProfileCharacter(type: CharacterType): PlayerProfileCharacter = when (type) {
        CharacterType.Trainer -> PlayerProfileCharacter(
            name = BuiltInCharacters.trainer.name,
            fallbackArtwork = BuiltInCharacters.trainer.contactArtwork.resource
        )
        CharacterType.Monster -> PlayerProfileCharacter(
            name = BuiltInCharacters.monster.character.name,
            fallbackArtwork = BuiltInCharacters.monster.character.contactArtwork.resource,
            level = BuiltInCharacters.monster.level,
        )
    }
}

data class PlayerProfile(
    val trainer: PlayerProfileCharacter,
    val monster: PlayerProfileCharacter,
    val roster: List<PlayerRosterMonster>,
)

data class ProfileMetrics(
    val callsBattled: Int = 0,
    val charactersCollected: Int = 0,
    val radiantsFound: Int = 0,
)

data class PlayerRosterMonster(
    val character: PlayerProfileCharacter,
    val reference: CharacterReference?,
    val isActive: Boolean,
)

data class PlayerProfileCharacter(
    val name: String,
    val artwork: File? = null,
    @param:DrawableRes val fallbackArtwork: Int? = null,
    val level: Int? = null,
)
