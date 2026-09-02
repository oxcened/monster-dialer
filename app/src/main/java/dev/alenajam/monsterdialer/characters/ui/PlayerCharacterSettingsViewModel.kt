package dev.alenajam.monsterdialer.characters.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharacterLayoutPreferences
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
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
class PlayerCharacterSettingsViewModel @Inject constructor(
    private val charactersRepository: CharactersRepository,
    private val assignmentRepository: CharacterAssignmentRepository,
    private val layoutPreferences: CharacterLayoutPreferences,
    private val packsRepository: dev.alenajam.monsterdialer.packs.data.PacksRepository,
    radiantUnlocks: RadiantVariantUnlockStore,
) : ViewModel() {

    val unlockedVariants = radiantUnlocks.unlocked

    val trainers: StateFlow<List<InstalledPackCharacter>> = charactersRepository.observeCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Trainer
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monsters: StateFlow<List<InstalledPackCharacter>> = charactersRepository.observeCharactersAssignableTo(
        CharacterAssignmentTarget.Player, CharacterType.Monster
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLimitReached: StateFlow<Boolean> = packsRepository.getPacks()
        .map { packs ->
            val customPack = packs.find { it.id == CustomCharacterRepository.CUSTOM_PACK_ID }
            (customPack?.characterCount ?: 0) >= dev.alenajam.monsterdialer.packs.data.CharacterPackValidator.MaxCharacters
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _assignedTrainer = MutableStateFlow<CharacterReference?>(null)
    val assignedTrainer: StateFlow<CharacterReference?> = _assignedTrainer.asStateFlow()

    private val _assignedMonster = MutableStateFlow<CharacterReference?>(null)
    val assignedMonster: StateFlow<CharacterReference?> = _assignedMonster.asStateFlow()

    private val _monsterRoster = MutableStateFlow<List<CharacterReference>>(emptyList())
    val monsterRoster: StateFlow<List<CharacterReference>> = _monsterRoster.asStateFlow()

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

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    init {
        viewModelScope.launch {
            _assignedTrainer.value = assignmentRepository.getPlayerCharacter(CharacterType.Trainer)
            _assignedMonster.value = assignmentRepository.getPlayerCharacter(CharacterType.Monster)
            _monsterRoster.value = assignmentRepository.getPlayerMonsterRoster()
            _dataVersion.value += 1
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
            if (reference == null) {
                assignmentRepository.setPlayerCharacter(CharacterType.Monster, null)
            } else {
                assignmentRepository.addPlayerMonsterToRoster(reference)
            }
            _assignedMonster.value = assignmentRepository.getPlayerCharacter(CharacterType.Monster)
            _monsterRoster.value = assignmentRepository.getPlayerMonsterRoster()
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
}
