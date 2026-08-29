package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharacterLayoutPreferences
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class PlayerCharacterSettingsViewModel @Inject constructor(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository,
    private val layoutPreferences: CharacterLayoutPreferences
) : ViewModel() {

    val trainers: List<InstalledPackCharacter> = charactersRepository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Trainer
    )

    val monsters: List<InstalledPackCharacter> = charactersRepository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Monster
    )

    private val _assignedTrainer = MutableStateFlow<CharacterReference?>(null)
    val assignedTrainer: StateFlow<CharacterReference?> = _assignedTrainer.asStateFlow()

    private val _assignedMonster = MutableStateFlow<CharacterReference?>(null)
    val assignedMonster: StateFlow<CharacterReference?> = _assignedMonster.asStateFlow()

    private val _layout = MutableStateFlow(
        if (layoutPreferences.isGridLayout() && (trainers.isNotEmpty() || monsters.isNotEmpty())) {
            CharacterLayout.Grid
        } else {
            CharacterLayout.List
        }
    )
    val layout: StateFlow<CharacterLayout> = _layout.asStateFlow()

    init {
        viewModelScope.launch {
            _assignedTrainer.value = assignmentRepository.getPlayerCharacter(CharacterType.Trainer)
            _assignedMonster.value = assignmentRepository.getPlayerCharacter(CharacterType.Monster)
        }
    }

    fun assignTrainer(reference: CharacterReference?) {
        viewModelScope.launch {
            assignmentRepository.setPlayerCharacter(CharacterType.Trainer, reference)
            _assignedTrainer.value = reference
        }
    }

    fun assignMonster(reference: CharacterReference?) {
        viewModelScope.launch {
            assignmentRepository.setPlayerCharacter(CharacterType.Monster, reference)
            _assignedMonster.value = reference
        }
    }

    fun setLayout(layout: CharacterLayout) {
        layoutPreferences.setGridLayout(layout == CharacterLayout.Grid)
        _layout.value = layout
    }
}
