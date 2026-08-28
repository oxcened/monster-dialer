package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.contacts.data.ContactSelectionRepository
import dev.alenajam.monsterdialer.contacts.data.MonsterContact
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ContactCharacterSettingsViewModel @Inject constructor(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository,
    private val selectionRepository: ContactSelectionRepository
) : ViewModel() {

    private val _contact = MutableStateFlow<MonsterContact?>(null)
    val contact: StateFlow<MonsterContact?> = _contact.asStateFlow()

    private val _assignedTrainer = MutableStateFlow<CharacterReference?>(null)
    val assignedTrainer: StateFlow<CharacterReference?> = _assignedTrainer.asStateFlow()

    private val _assignedMonster = MutableStateFlow<CharacterReference?>(null)
    val assignedMonster: StateFlow<CharacterReference?> = _assignedMonster.asStateFlow()

    val trainers: List<InstalledPackCharacter> = charactersRepository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Contact, CharacterType.Trainer
    )

    val monsters: List<InstalledPackCharacter> = charactersRepository.getCharactersAssignableTo(
        CharacterAssignmentTarget.Contact, CharacterType.Monster
    )

    init {
        restoreSelectedContact()
    }

    fun restoreSelectedContact() {
        viewModelScope.launch {
            val restored = selectionRepository.getSelectedContact()
            _contact.value = restored
            
            _assignedTrainer.value = restored?.contactKeys()?.firstNotNullOfOrNull {
                assignmentRepository.getAssignedCharacter(it, CharacterType.Trainer)
            }
            _assignedMonster.value = restored?.contactKeys()?.firstNotNullOfOrNull {
                assignmentRepository.getAssignedCharacter(it, CharacterType.Monster)
            }
        }
    }

    fun onContactSelected(selectedContact: DialerContactSummary) {
        viewModelScope.launch {
            selectionRepository.setSelectedContact(selectedContact)
            restoreSelectedContact()
        }
    }

    fun assignTrainer(reference: CharacterReference?) {
        val selected = _contact.value ?: return
        viewModelScope.launch {
            selected.numbers.forEach {
                assignmentRepository.assignCharacter(
                    it,
                    CharacterType.Trainer,
                    reference,
                    selected.name
                )
            }
            _assignedTrainer.value = reference
        }
    }

    fun assignMonster(reference: CharacterReference?) {
        val selected = _contact.value ?: return
        viewModelScope.launch {
            selected.numbers.forEach {
                assignmentRepository.assignCharacter(
                    it,
                    CharacterType.Monster,
                    reference,
                    selected.name
                )
            }
            _assignedMonster.value = reference
        }
    }

    private fun MonsterContact.contactKeys(): List<String> = numbers
}
