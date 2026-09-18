package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.ContactCharacterMode
import dev.alenajam.monsterdialer.characters.data.ContactCharacterSelection
import dev.alenajam.monsterdialer.characters.data.ContactCharacterOverview
import dev.alenajam.monsterdialer.characters.data.ContactCharacterAssignmentUpdate
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
import dev.alenajam.opendialer.data.contacts.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val contactsRepository: ContactsRepository,
) : ViewModel() {
    private val selectedContactMutex = Mutex()
    private var overviewEntered = false
    private var assignmentPickerRequested = false
    private var pendingGuidedTrainer: CharacterReference? = null
    private var pendingGuidedTrainerRandom = false
    private var pendingGuidedTrainerPool: Set<CharacterReference> = emptySet()
    private val _lastAddedContactLabel = MutableStateFlow<String?>(null)
    val lastAddedContactLabel: StateFlow<String?> = _lastAddedContactLabel.asStateFlow()

    private val _filter = MutableStateFlow(MonsterFilter.All)
    val filter: StateFlow<MonsterFilter> = _filter.asStateFlow()

    fun setFilter(filter: MonsterFilter) {
        _filter.value = filter
    }

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

    private val _rosterCursorContactKey = MutableStateFlow<String?>(null)
    val rosterCursorContactKey: StateFlow<String?> = _rosterCursorContactKey.asStateFlow()

    private val _assignedTrainer = MutableStateFlow<CharacterReference?>(null)
    val assignedTrainer: StateFlow<CharacterReference?> = _assignedTrainer.asStateFlow()

    private val _assignedMonster = MutableStateFlow<CharacterReference?>(null)
    val assignedMonster: StateFlow<CharacterReference?> = _assignedMonster.asStateFlow()

    private val _trainerMode = MutableStateFlow(ContactCharacterMode.Random)
    val trainerMode: StateFlow<ContactCharacterMode> = _trainerMode.asStateFlow()

    private val _trainerUsesGlobalDefaults = MutableStateFlow(true)
    val trainerUsesGlobalDefaults: StateFlow<Boolean> = _trainerUsesGlobalDefaults.asStateFlow()

    private val _monsterMode = MutableStateFlow(ContactCharacterMode.Random)
    val monsterMode: StateFlow<ContactCharacterMode> = _monsterMode.asStateFlow()

    private val _monsterUsesGlobalDefaults = MutableStateFlow(true)
    val monsterUsesGlobalDefaults: StateFlow<Boolean> = _monsterUsesGlobalDefaults.asStateFlow()

    private val _contactRandomPools = MutableStateFlow<Map<CharacterType, Set<CharacterReference>>>(emptyMap())
    val contactRandomPools: StateFlow<Map<CharacterType, Set<CharacterReference>>> = _contactRandomPools.asStateFlow()

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

    val contactDefaults = assignmentRepository.assignmentVersion
        .map { assignmentRepository.getContactCharacterDefaults() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            dev.alenajam.monsterdialer.characters.data.ContactCharacterDefaults(emptyMap(), emptyMap()),
        )

    val customizedContacts: StateFlow<List<ContactCharacterOverview>> = assignmentRepository.assignmentVersion
        .map { assignmentRepository.getContactCharacterOverviews() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customizedContactIds: StateFlow<Set<Int>> = combine(
        contactsRepository.getContacts(),
        assignmentRepository.assignmentVersion,
    ) { contacts, _ -> contacts }
        .mapLatest { contacts ->
            buildSet {
                contacts.forEach { contact ->
                    if (assignmentRepository.hasContactOverride(contactsRepository.getContactNumbers(contact.id))) {
                        add(contact.id)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun allContactPoolReferences(type: CharacterType): Set<CharacterReference> {
        val builtInReference = if (type == CharacterType.Trainer) {
            BuiltInCharacters.defaultTrainerReference
        } else {
            BuiltInCharacters.defaultMonsterReference
        }
        val characters = if (type == CharacterType.Trainer) trainers.value else monsters.value
        return buildSet {
            add(builtInReference)
            addAll(characters.flatMap { installed ->
                installed.character.visualVariants.filter { variant ->
                    !variant.isRadiant || CharacterReference(installed.packId, installed.character.id, variant.id) in unlockedVariants.value
                }.map { variant ->
                    CharacterReference(installed.packId, installed.character.id, variant.id)
                }
            })
        }
    }

    fun selectedContactPool(
        type: CharacterType,
        allReferences: Set<CharacterReference>,
    ): Set<CharacterReference> = contactDefaults.value.randomPools[type]?.toSet() ?: allReferences

    fun selectedContactSpecificPool(
        type: CharacterType,
        allReferences: Set<CharacterReference>,
    ): Set<CharacterReference> = _contactRandomPools.value[type] ?: selectedContactPool(type, allReferences)

    fun setContactSpecificRandomPool(type: CharacterType, references: Set<CharacterReference>) {
        val selected = _contact.value ?: return
        viewModelScope.launch {
            selected.numbers.forEach { assignmentRepository.setContactRandomPool(it, type, references.toList()) }
            _contactRandomPools.value = _contactRandomPools.value + (type to references)
        }
    }

    fun clearContactSpecificRandomPool(type: CharacterType) {
        val selected = _contact.value ?: return
        viewModelScope.launch {
            selected.numbers.forEach { assignmentRepository.clearContactRandomPool(it, type) }
            _contactRandomPools.value = _contactRandomPools.value - type
        }
    }

    init {
        restoreSelectedContact()
    }

    fun enterOverview() {
        if (overviewEntered) return
        overviewEntered = true
        viewModelScope.launch {
            selectedContactMutex.withLock {
                selectionRepository.clearSelectedContact()
                restoreSelectedContactState()
            }
        }
    }

    fun setContactDefault(type: CharacterType, reference: CharacterReference?) {
        viewModelScope.launch {
            assignmentRepository.setContactDefault(type, reference)
            restoreSelectedContactState()
        }
    }

    fun setContactRandomPool(type: CharacterType, references: List<CharacterReference>) {
        viewModelScope.launch {
            assignmentRepository.setContactRandomPool(type, references)
        }
    }

    fun resetContactRandomPool(type: CharacterType) {
        viewModelScope.launch {
            assignmentRepository.clearContactRandomPool(type)
        }
    }

    fun restoreSelectedContact() {
        viewModelScope.launch {
            selectedContactMutex.withLock {
                restoreSelectedContactState()
            }
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

    fun selectCustomizedContact(overview: ContactCharacterOverview, type: CharacterType) {
        viewModelScope.launch {
            selectedContactMutex.withLock {
                if (selectionRepository.setSelectedContact(overview.label, overview.contactKeys)) {
                    assignmentPickerRequested = false
                    pendingGuidedTrainer = null
                    pendingGuidedTrainerRandom = false
                    pendingGuidedTrainerPool = emptySet()
                    _selectedTab.value = if (type == CharacterType.Trainer) 0 else 1
                    layoutPreferences.setSelectedTab(_selectedTab.value)
                    restoreSelectedContactState()
                }
            }
        }
    }

    fun setRosterCursor(contactKey: String) {
        _rosterCursorContactKey.value = contactKey
    }

    fun clearRosterCursor() {
        _rosterCursorContactKey.value = null
    }

    fun removeContactFromRoster(overview: ContactCharacterOverview) {
        viewModelScope.launch {
            assignmentRepository.clearContactAssignments(overview.contactKeys)
        }
    }

    fun clearSelectedContact() {
        viewModelScope.launch {
            selectedContactMutex.withLock {
                assignmentPickerRequested = false
                pendingGuidedTrainer = null
                pendingGuidedTrainerRandom = false
                pendingGuidedTrainerPool = emptySet()
                selectionRepository.clearSelectedContact()
                restoreSelectedContactState()
            }
        }
    }

    fun prepareContactAssignment() {
        assignmentPickerRequested = true
        pendingGuidedTrainer = null
        pendingGuidedTrainerRandom = false
        pendingGuidedTrainerPool = emptySet()
        _lastAddedContactLabel.value = null
        _selectedTab.value = 0
        layoutPreferences.setSelectedTab(0)
        _trainerUsesGlobalDefaults.value = false
        _monsterUsesGlobalDefaults.value = false
    }

    val isGuidedAssignmentActive: Boolean
        get() = assignmentPickerRequested

    val guidedTrainerSelection: CharacterReference?
        get() = pendingGuidedTrainer

    fun stageGuidedTrainer(reference: CharacterReference?) {
        pendingGuidedTrainer = reference
        pendingGuidedTrainerRandom = false
        pendingGuidedTrainerPool = emptySet()
        setSelectedTab(1)
    }

    fun stageGuidedTrainerRandom(pool: Set<CharacterReference>) {
        pendingGuidedTrainer = null
        pendingGuidedTrainerRandom = true
        pendingGuidedTrainerPool = pool
        setSelectedTab(1)
    }

    fun completeGuidedAssignment(
        monster: CharacterReference?,
        monsterRandom: Boolean = false,
        monsterPool: Set<CharacterReference> = emptySet(),
        onCompleted: () -> Unit,
    ) {
        val selected = _contact.value ?: return
        val trainer = pendingGuidedTrainer
        val trainerRandom = pendingGuidedTrainerRandom
        viewModelScope.launch {
            assignmentRepository.updateContactAssignments(
                contactKeys = selected.numbers,
                label = selected.name,
                trainer = ContactCharacterAssignmentUpdate(
                    character = trainer,
                    randomPool = pendingGuidedTrainerPool.takeIf { trainerRandom }?.toList(),
                ),
                monster = ContactCharacterAssignmentUpdate(
                    character = monster,
                    randomPool = monsterPool.takeIf { monsterRandom }?.toList(),
                ),
            )
            if (!pendingGuidedTrainerRandom && trainer == null) {
                selected.numbers.forEach { assignmentRepository.clearContactOverride(it, CharacterType.Trainer) }
            }
            if (!monsterRandom && monster == null) {
                selected.numbers.forEach { assignmentRepository.clearContactOverride(it, CharacterType.Monster) }
            }
            _assignedTrainer.value = trainer
            _assignedMonster.value = monster
            _trainerMode.value = if (trainerRandom) ContactCharacterMode.Random else ContactCharacterMode.Default
            _monsterMode.value = if (monsterRandom) ContactCharacterMode.Random else ContactCharacterMode.Default
            _trainerUsesGlobalDefaults.value = false
            _monsterUsesGlobalDefaults.value = false
            assignmentPickerRequested = false
            pendingGuidedTrainer = null
            pendingGuidedTrainerRandom = false
            pendingGuidedTrainerPool = emptySet()
            _lastAddedContactLabel.value = selected.name.takeIf {
                trainerRandom || monsterRandom || trainer != null || monster != null
            }
            onCompleted()
        }
    }

    fun clearRosterStatusMessage() {
        _lastAddedContactLabel.value = null
    }

    suspend fun selectContact(selectedContact: DialerContactSummary): Boolean {
        return selectedContactMutex.withLock {
            if (!selectionRepository.setSelectedContact(selectedContact)) return@withLock false
            restoreSelectedContactState()
            val profileId = _pendingOnlineProfileId.value ?: return@withLock true
            val contact = _contact.value ?: return@withLock false
            onlineOpponentResolver.link(contact.numbers, profileId).also { linked ->
                if (linked) _pendingOnlineProfileId.value = null
            }
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

    fun assignTrainer(reference: CharacterReference?, onCompleted: () -> Unit = {}) {
        if (reference == null) {
            setUsesGlobalDefaults(CharacterType.Trainer, true, onCompleted)
        } else {
            updateExistingContactAssignment(
                type = CharacterType.Trainer,
                update = ContactCharacterAssignmentUpdate(character = reference),
                onCompleted = onCompleted,
            )
        }
    }

    fun assignMonster(reference: CharacterReference?, onCompleted: () -> Unit = {}) {
        if (reference == null) {
            setUsesGlobalDefaults(CharacterType.Monster, true, onCompleted)
        } else {
            updateExistingContactAssignment(
                type = CharacterType.Monster,
                update = ContactCharacterAssignmentUpdate(character = reference),
                onCompleted = onCompleted,
            )
        }
    }

    fun randomizeTrainer() = setRandomMode(CharacterType.Trainer)

    fun randomizeMonster() = setRandomMode(CharacterType.Monster)

    fun configureRandomMode(
        type: CharacterType,
        pool: Set<CharacterReference>,
        onCompleted: () -> Unit = {},
    ) = updateExistingContactAssignment(
        type = type,
        update = ContactCharacterAssignmentUpdate(randomPool = pool.toList()),
        onCompleted = onCompleted,
    )

    fun setUsesGlobalDefaults(
        type: CharacterType,
        usesGlobalDefaults: Boolean,
        onCompleted: () -> Unit = {},
    ) {
        val selected = _contact.value ?: return
        viewModelScope.launch {
            selected.numbers.forEach { number ->
                if (usesGlobalDefaults) {
                    assignmentRepository.clearContactOverride(number, type)
                } else if (
                    !assignmentRepository.hasContactOverride(number, type) ||
                    assignmentRepository.getContactCharacterSelection(number, type).mode == ContactCharacterMode.Random
                ) {
                    assignmentRepository.assignCharacter(
                        number,
                        type,
                        if (type == CharacterType.Trainer) {
                            BuiltInCharacters.defaultTrainerReference
                        } else {
                            BuiltInCharacters.defaultMonsterReference
                        },
                        selected.name,
                    )
                }
            }
            selected.numbers.forEach { assignmentRepository.clearContactRandomPool(it, type) }
            _contactRandomPools.value = _contactRandomPools.value - type
            restoreSelectedContactState()
            onCompleted()
        }
    }

    private fun setRandomMode(type: CharacterType) {
        val initialPool = contactDefaults.value.randomPools[type]?.toSet()
            ?: allContactPoolReferences(type)
        configureRandomMode(type, initialPool)
    }

    private fun updateExistingContactAssignment(
        type: CharacterType,
        update: ContactCharacterAssignmentUpdate,
        onCompleted: () -> Unit,
    ) {
        val selected = _contact.value ?: return
        val existingTrainer = currentContactAssignment(CharacterType.Trainer)
        val existingMonster = currentContactAssignment(CharacterType.Monster)
        val trainer = if (type == CharacterType.Trainer) update else existingTrainer
        val monster = if (type == CharacterType.Monster) update else existingMonster
        viewModelScope.launch {
            assignmentRepository.updateContactAssignments(
                contactKeys = selected.numbers,
                label = selected.name,
                trainer = trainer,
                monster = monster,
            )
            _assignedTrainer.value = trainer.character
            _trainerMode.value = if (trainer.randomPool != null) ContactCharacterMode.Random else ContactCharacterMode.Default
            _assignedMonster.value = monster.character
            _monsterMode.value = if (monster.randomPool != null) ContactCharacterMode.Random else ContactCharacterMode.Default
            _contactRandomPools.value = buildMap {
                trainer.randomPool?.let { put(CharacterType.Trainer, it.toSet()) }
                monster.randomPool?.let { put(CharacterType.Monster, it.toSet()) }
            }
            _trainerUsesGlobalDefaults.value = false
            _monsterUsesGlobalDefaults.value = false
            onCompleted()
        }
    }

    private fun currentContactAssignment(type: CharacterType): ContactCharacterAssignmentUpdate {
        val randomPool = _contactRandomPools.value[type]
        return if ((if (type == CharacterType.Trainer) _trainerMode.value else _monsterMode.value) == ContactCharacterMode.Random) {
            ContactCharacterAssignmentUpdate(randomPool = randomPool?.toList().orEmpty())
        } else {
            ContactCharacterAssignmentUpdate(
                character = if (type == CharacterType.Trainer) _assignedTrainer.value else _assignedMonster.value,
            )
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
        // Hide the previous picker until every field for the next contact is ready.
        _contact.value = null
        val restored = selectionRepository.getSelectedContact()
        val contactKeys = restored?.contactKeys().orEmpty()
        val trainerSelection = contactKeys.commonSelection(CharacterType.Trainer)
        val monsterSelection = contactKeys.commonSelection(CharacterType.Monster)
        val randomPools = CharacterType.entries.mapNotNull { type ->
            val pool = contactKeys.map { assignmentRepository.getContactRandomPool(it, type) }.distinct().singleOrNull()
            pool?.let { type to it.toSet() }
        }.toMap()

        _assignedTrainer.value = trainerSelection?.character
        _trainerMode.value = trainerSelection?.mode ?: ContactCharacterMode.Random
        _trainerUsesGlobalDefaults.value = !assignmentPickerRequested && contactKeys.none {
            assignmentRepository.hasContactOverride(it, CharacterType.Trainer)
        }
        _assignedMonster.value = monsterSelection?.character
        _monsterMode.value = monsterSelection?.mode ?: ContactCharacterMode.Random
        _monsterUsesGlobalDefaults.value = !assignmentPickerRequested && contactKeys.none {
            assignmentRepository.hasContactOverride(it, CharacterType.Monster)
        }
        _contactRandomPools.value = randomPools
        _linkedOnlineProfileId.value = restored?.numbers?.let(onlineOpponentResolver::linkedProfileId)
        _contact.value = restored
        _contactSelectionVersion.value += 1
    }

    private suspend fun List<String>.commonSelection(type: CharacterType): ContactCharacterSelection? =
        map { assignmentRepository.getContactCharacterSelection(it, type) }
            .distinct()
            .singleOrNull()
}
