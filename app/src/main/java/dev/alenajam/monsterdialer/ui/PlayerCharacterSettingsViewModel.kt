package dev.alenajam.monsterdialer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.data.characters.CharactersRepository
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class PlayerCharacterSettingsViewModel @Inject constructor(
    private val repository: CharactersRepository
) : ViewModel() {

    var assignedTrainer by mutableStateOf<CharacterReference?>(null)
        private set

    var assignedMonster by mutableStateOf<CharacterReference?>(null)
        private set

    val trainers: List<InstalledPackCharacter> = repository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Trainer
    )

    val monsters: List<InstalledPackCharacter> = repository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Monster
    )

    init {
        viewModelScope.launch {
            assignedTrainer = repository.getPlayerCharacter(CharacterType.Trainer)
            assignedMonster = repository.getPlayerCharacter(CharacterType.Monster)
        }
    }

    fun assignTrainer(reference: CharacterReference?) {
        viewModelScope.launch {
            repository.setPlayerCharacter(CharacterType.Trainer, reference)
            assignedTrainer = reference
        }
    }

    fun assignMonster(reference: CharacterReference?) {
        viewModelScope.launch {
            repository.setPlayerCharacter(CharacterType.Monster, reference)
            assignedMonster = reference
        }
    }
}
