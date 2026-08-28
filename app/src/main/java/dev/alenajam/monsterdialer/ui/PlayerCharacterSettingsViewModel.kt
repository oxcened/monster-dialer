package dev.alenajam.monsterdialer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.data.characters.CharactersRepository
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class PlayerCharacterSettingsViewModel @Inject constructor(
    private val repository: CharactersRepository
) : ViewModel() {

    private val _assignedTrainer = MutableStateFlow<CharacterReference?>(null)
    val assignedTrainer: StateFlow<CharacterReference?> = _assignedTrainer.asStateFlow()

    private val _assignedMonster = MutableStateFlow<CharacterReference?>(null)
    val assignedMonster: StateFlow<CharacterReference?> = _assignedMonster.asStateFlow()

    val trainers: List<InstalledPackCharacter> = repository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Trainer
    )

    val monsters: List<InstalledPackCharacter> = repository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Monster
    )

    init {
        viewModelScope.launch {
            _assignedTrainer.value = repository.getPlayerCharacter(CharacterType.Trainer)
            _assignedMonster.value = repository.getPlayerCharacter(CharacterType.Monster)
        }
    }

    fun assignTrainer(reference: CharacterReference?) {
        viewModelScope.launch {
            repository.setPlayerCharacter(CharacterType.Trainer, reference)
            _assignedTrainer.value = reference
        }
    }

    fun assignMonster(reference: CharacterReference?) {
        viewModelScope.launch {
            repository.setPlayerCharacter(CharacterType.Monster, reference)
            _assignedMonster.value = reference
        }
    }
}
