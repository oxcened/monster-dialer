package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.ContactCharacterMode
import dev.alenajam.monsterdialer.characters.data.CharacterLayoutPreferences
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.contacts.data.ContactSelectionRepository
import dev.alenajam.monsterdialer.contacts.data.MonsterContact
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineOpponentResolver
import dev.alenajam.monsterdialer.onlineprofiles.data.PublicProfileId
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
    private val packsRepository: dev.alenajam.monsterdialer.packs.data.PacksRepository,
    radiantUnlocks: RadiantVariantUnlockStore,
    private val onlineOpponentResolver: OnlineOpponentResolver,
) : ViewModel() {

    val unlockedVariants = radiantUnlocks.unlocked

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

    private val _trainerMode = MutableStateFlow(ContactCharacterMode.Random)
    val trainerMode: StateFlow<ContactCharacterMode> = _trainerMode.asStateFlow()

    private val _monsterMode = MutableStateFlow(ContactCharacterMode.Random)
    val monsterMode: StateFlow<ContactCharacterMode> = _monsterMode.asStateFlow()

    private val _layout = MutableStateFlow(
        if (layoutPreferences.isGridLayout()) {
            CharacterLayout.Grid
        } else {
            CharacterLayout.List
        }
    )
    val layout: StateFlow<CharacterLayout> = _layout.asStateFlow()

    private val _selectedTab = MutableStateFlow(layoutPreferences.getSelectedTab())
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _contactSelectionVersion = MutableStateFlow(0)
    val contactSelectionVersion: StateFlow<Int> = _contactSelectionVersion.asStateFlow()

    private val _pendingOnlineProfileId = MutableStateFlow<String?>(null)
    val pendingOnlineProfileId: StateFlow<String?> = _pendingOnlineProfileId.asStateFlow()

    private val _linkedOnlineProfileId = MutableStateFlow<String?>(null)
    val linkedOnlineProfileId: StateFlow<String?> = _linkedOnlineProfileId.asStateFlow()

    init {
        restoreSelectedContact()
    }

    fun restoreSelectedContact() {
        viewModelScope.launch {
            restoreSelectedContactState()
        }
    }

    fun onContactSelected(
        selectedContact: DialerContactSummary,
        onSelected: () -> Unit,
        onRejected: () -> Unit,
    ) {
        viewModelScope.launch {
            if (selectContact(selectedContact)) onSelected() else onRejected()
        }
    }

    suspend fun selectContact(selectedContact: DialerContactSummary): Boolean {
        if (!selectionRepository.setSelectedContact(selectedContact)) return false
        restoreSelectedContactState()
        val profileId = _pendingOnlineProfileId.value ?: return true
        val contact = _contact.value ?: return false
        return onlineOpponentResolver.link(contact.numbers, profileId).also { linked ->
            if (linked) _pendingOnlineProfileId.value = null
        }
    }

    /** Selects a contact and links the pending Online Profile, if one was opened from a shared URI. */
    suspend fun selectContactForPendingOnlineProfile(selectedContact: DialerContactSummary): Boolean {
        if (!selectionRepository.setSelectedContact(selectedContact)) return false
        restoreSelectedContactState()
        val profileId = _pendingOnlineProfileId.value ?: return true
        val contact = _contact.value ?: return false
        return onlineOpponentResolver.link(contact.numbers, profileId).also { linked ->
            if (linked) _pendingOnlineProfileId.value = null
        }
    }

    /** Called only after a validated, opt-in profile URI has been opened. */
    fun prepareOnlineProfileLink(publicProfileId: String) {
        if (PublicProfileId.isValid(publicProfileId)) _pendingOnlineProfileId.value = publicProfileId
    }

    fun clearPendingOnlineProfile() {
        _pendingOnlineProfileId.value = null
    }

    fun unlinkOnlineProfile() {
        _contact.value?.numbers?.let {
            onlineOpponentResolver.unlink(it)
            _linkedOnlineProfileId.value = null
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
            _trainerMode.value = ContactCharacterMode.Default
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
            _monsterMode.value = ContactCharacterMode.Default
        }
    }

    fun randomizeTrainer() = setRandomMode(CharacterType.Trainer)

    fun randomizeMonster() = setRandomMode(CharacterType.Monster)

    private fun setRandomMode(type: CharacterType) {
        val selected = _contact.value ?: return
        viewModelScope.launch {
            selected.numbers.forEach {
                assignmentRepository.randomizeCharacter(it, type, selected.name)
            }
            if (type == CharacterType.Trainer) {
                _assignedTrainer.value = null
                _trainerMode.value = ContactCharacterMode.Random
            } else {
                _assignedMonster.value = null
                _monsterMode.value = ContactCharacterMode.Random
            }
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

    fun setSelectedTab(index: Int) {
        layoutPreferences.setSelectedTab(index)
        _selectedTab.value = index
    }

    suspend fun isCharacterInUse(characterId: String): Boolean {
        val reference = CharacterReference(CustomCharacterRepository.CUSTOM_PACK_ID, characterId)
        return charactersRepository.isCharacterInUse(reference)
    }

    private fun MonsterContact.contactKeys(): List<String> = numbers

    private suspend fun restoreSelectedContactState() {
        val restored = selectionRepository.getSelectedContact()
        _contact.value = restored
        _linkedOnlineProfileId.value = restored?.numbers?.let(onlineOpponentResolver::linkedProfileId)

        val trainerSelection = restored?.contactKeys()?.firstOrNull()?.let {
            assignmentRepository.getContactCharacterSelection(it, CharacterType.Trainer)
        }
        val monsterSelection = restored?.contactKeys()?.firstOrNull()?.let {
            assignmentRepository.getContactCharacterSelection(it, CharacterType.Monster)
        }
        _assignedTrainer.value = trainerSelection?.character
        _trainerMode.value = trainerSelection?.mode ?: ContactCharacterMode.Random
        _assignedMonster.value = monsterSelection?.character
        _monsterMode.value = monsterSelection?.mode ?: ContactCharacterMode.Random
        _contactSelectionVersion.value += 1
    }
}
