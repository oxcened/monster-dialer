package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
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
}
