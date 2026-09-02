package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.DrawableRes
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import java.io.File
import dev.alenajam.monsterdialer.packs.data.CharacterType
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CharacterSettingsSummaryViewModel @Inject constructor(
    private val assignmentRepository: CharacterAssignmentRepository,
    private val charactersRepository: CharactersRepository
) : ViewModel() {

    val playerProfile: StateFlow<PlayerProfile> = assignmentRepository.assignmentVersion
        .map {
            PlayerProfile(
                trainer = playerProfileCharacter(CharacterType.Trainer),
                monster = playerProfileCharacter(CharacterType.Monster)
            )
        }
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

    private suspend fun playerProfileCharacter(type: CharacterType): PlayerProfileCharacter {
        val reference = assignmentRepository.getPlayerCharacter(type) ?: return builtInProfileCharacter(type)
        val installed = charactersRepository.findCharacter(
            reference = reference,
            role = CharacterAssignmentTarget.Player,
            type = type
        ) ?: return builtInProfileCharacter(type)
        val artwork = installed.character.variant(reference.variantId)?.let { variant ->
            variant.frontImage ?: variant.backImage
        } ?: return builtInProfileCharacter(type)
        return PlayerProfileCharacter(
            name = installed.character.name,
            artwork = installed.imageFile(artwork),
            level = installed.character.level,
        )
    }

    private fun defaultPlayerProfile() = PlayerProfile(
        trainer = builtInProfileCharacter(CharacterType.Trainer),
        monster = builtInProfileCharacter(CharacterType.Monster)
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
)

data class PlayerProfileCharacter(
    val name: String,
    val artwork: File? = null,
    @param:DrawableRes val fallbackArtwork: Int? = null,
    val level: Int? = null,
)
