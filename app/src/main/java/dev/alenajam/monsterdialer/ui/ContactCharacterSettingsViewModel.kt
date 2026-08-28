package dev.alenajam.monsterdialer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.data.characters.CharactersRepository
import dev.alenajam.monsterdialer.data.characters.MonsterContact
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ContactCharacterSettingsViewModel @Inject constructor(
    private val repository: CharactersRepository
) : ViewModel() {

    var contact by mutableStateOf<MonsterContact?>(null)
        private set

    var assignedTrainer by mutableStateOf<CharacterReference?>(null)
        private set

    var assignedMonster by mutableStateOf<CharacterReference?>(null)
        private set

    val trainers: List<InstalledPackCharacter> = repository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Contact, CharacterType.Trainer
    )

    val monsters: List<InstalledPackCharacter> = repository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Contact, CharacterType.Monster
    )

    init {
        restoreSelectedContact()
    }

    fun restoreSelectedContact() {
        viewModelScope.launch {
            val restored = repository.getSelectedContact()
            contact = restored
            
            assignedTrainer = restored?.contactKeys()?.firstNotNullOfOrNull {
                repository.getAssignedCharacter(it, CharacterType.Trainer)
            }
            assignedMonster = restored?.contactKeys()?.firstNotNullOfOrNull {
                repository.getAssignedCharacter(it, CharacterType.Monster)
            }
        }
    }

    fun onContactSelected(selectedContact: DialerContactSummary) {
        viewModelScope.launch {
            repository.setSelectedContact(selectedContact)
            restoreSelectedContact()
        }
    }

    fun assignTrainer(reference: CharacterReference?) {
        val selected = contact ?: return
        viewModelScope.launch {
            selected.numbers.forEach {
                repository.assignCharacter(
                    it,
                    CharacterType.Trainer,
                    reference,
                    selected.name
                )
            }
            assignedTrainer = reference
        }
    }

    fun assignMonster(reference: CharacterReference?) {
        val selected = contact ?: return
        viewModelScope.launch {
            selected.numbers.forEach {
                repository.assignCharacter(
                    it,
                    CharacterType.Monster,
                    reference,
                    selected.name
                )
            }
            assignedMonster = reference
        }
    }

    private fun MonsterContact.contactKeys(): List<String> = numbers
}
