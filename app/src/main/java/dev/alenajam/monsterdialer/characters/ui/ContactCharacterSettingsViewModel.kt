package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharacterLayoutPreferences
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.contacts.data.ContactSelectionRepository
import dev.alenajam.monsterdialer.contacts.data.MonsterContact
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ContactCharacterSettingsViewModel @Inject constructor(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository,
    private val selectionRepository: ContactSelectionRepository,
    private val layoutPreferences: CharacterLayoutPreferences,
    private val packsRepository: dev.alenajam.monsterdialer.packs.data.PacksRepository
) : ViewModel() {

    val trainers: StateFlow<List<InstalledPackCharacter>> = charactersRepository.observeCharactersAssignableTo(
        CharacterAssignmentTarget.Contact, CharacterType.Trainer
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monsters: StateFlow<List<InstalledPackCharacter>> = charactersRepository.observeCharactersAssignableTo(
        CharacterAssignmentTarget.Contact, CharacterType.Monster
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLimitReached: StateFlow<Boolean> = packsRepository.getPacks()
        .map { packs ->
            val customPack = packs.find { it.id == CustomCharacterRepository.CUSTOM_PACK_ID }
            (customPack?.characterCount ?: 0) >= dev.alenajam.monsterdialer.packs.data.CharacterPackValidator.MaxCharacters
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _contact = MutableStateFlow<MonsterContact?>(null)
    val contact: StateFlow<MonsterContact?> = _contact.asStateFlow()

    private val _assignedTrainer = MutableStateFlow<CharacterReference?>(null)
    val assignedTrainer: StateFlow<CharacterReference?> = _assignedTrainer.asStateFlow()

    private val _assignedMonster = MutableStateFlow<CharacterReference?>(null)
    val assignedMonster: StateFlow<CharacterReference?> = _assignedMonster.asStateFlow()

    private val _layout = MutableStateFlow(
        if (layoutPreferences.isGridLayout()) {
            CharacterLayout.Grid
        } else {
            CharacterLayout.List
        }
    )
    val layout: StateFlow<CharacterLayout> = _layout.asStateFlow()

    private val _contactSelectionVersion = MutableStateFlow(0)
    val contactSelectionVersion: StateFlow<Int> = _contactSelectionVersion.asStateFlow()

    init {
        restoreSelectedContact()
    }

    fun restoreSelectedContact() {
        viewModelScope.launch {
            restoreSelectedContactState()
        }
    }

    fun onContactSelected(selectedContact: DialerContactSummary) {
        viewModelScope.launch {
            selectionRepository.setSelectedContact(selectedContact)
            restoreSelectedContactState()
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

    fun setLayout(layout: CharacterLayout) {
        layoutPreferences.setGridLayout(layout == CharacterLayout.Grid)
        _layout.value = layout
    }

    fun deleteCustomCharacter(characterId: String) {
        viewModelScope.launch {
            charactersRepository.deleteCustomCharacter(characterId)
        }
    }

    private fun MonsterContact.contactKeys(): List<String> = numbers

    private suspend fun restoreSelectedContactState() {
        val restored = selectionRepository.getSelectedContact()
        _contact.value = restored

        _assignedTrainer.value = restored?.contactKeys()?.firstNotNullOfOrNull {
            assignmentRepository.getAssignedCharacter(it, CharacterType.Trainer)
        }
        _assignedMonster.value = restored?.contactKeys()?.firstNotNullOfOrNull {
            assignmentRepository.getAssignedCharacter(it, CharacterType.Monster)
        }
        _contactSelectionVersion.value += 1
    }
}
