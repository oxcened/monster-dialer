package dev.alenajam.monsterdialer.characters.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackValidator
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddCharacterViewModel @Inject constructor(
    private val repository: CustomCharacterRepository,
    private val assignmentRepository: CharacterAssignmentRepository
) : ViewModel() {

    private var editingCharacterId: String? = null
    private var preferredAssignmentTarget: CharacterAssignmentTarget? = null
    private var isLegacyRadiant = false

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _type = MutableStateFlow(CharacterType.Monster)
    val type = _type.asStateFlow()

    private val _isRadiant = MutableStateFlow(false)
    val isRadiant = _isRadiant.asStateFlow()

    private val _level = MutableStateFlow("")
    val level = _level.asStateFlow()

    private val _maxHp = MutableStateFlow("")
    val maxHp = _maxHp.asStateFlow()

    private val _frontImageUri = MutableStateFlow<Uri?>(null)
    val frontImageUri = _frontImageUri.asStateFlow()

    private val _backImageUri = MutableStateFlow<Uri?>(null)
    val backImageUri = _backImageUri.asStateFlow()

    private val _radiantFrontImageUri = MutableStateFlow<Uri?>(null)
    val radiantFrontImageUri = _radiantFrontImageUri.asStateFlow()

    private val _radiantBackImageUri = MutableStateFlow<Uri?>(null)
    val radiantBackImageUri = _radiantBackImageUri.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _isLimitReached = MutableStateFlow(false)
    val isLimitReached = _isLimitReached.asStateFlow()

    private val _isAssignedToPlayer = MutableStateFlow(false)
    val isAssignedToPlayer = _isAssignedToPlayer.asStateFlow()

    private val _isAssignedToContact = MutableStateFlow(false)
    val isAssignedToContact = _isAssignedToContact.asStateFlow()

    private val _creationResult = MutableStateFlow<CharacterReference?>(null)
    val creationResult = _creationResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _existingFrontImageFile = MutableStateFlow<java.io.File?>(null)
    val existingFrontImageFile = _existingFrontImageFile.asStateFlow()

    private val _existingBackImageFile = MutableStateFlow<java.io.File?>(null)
    val existingBackImageFile = _existingBackImageFile.asStateFlow()

    private val _existingRadiantFrontImageFile = MutableStateFlow<java.io.File?>(null)
    val existingRadiantFrontImageFile = _existingRadiantFrontImageFile.asStateFlow()

    private val _existingRadiantBackImageFile = MutableStateFlow<java.io.File?>(null)
    val existingRadiantBackImageFile = _existingRadiantBackImageFile.asStateFlow()

    init {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { repository.getCharacterCount() }
            _isLimitReached.value = count >= CharacterPackValidator.MaxCharacters
        }
    }

    fun loadCharacter(characterId: String) {
        editingCharacterId = characterId
        viewModelScope.launch {
            val character = withContext(Dispatchers.IO) { repository.getCharacter(characterId) }
            if (character != null) {
                _name.value = character.name
                _type.value = character.type
                isLegacyRadiant = character.isRadiant && !character.hasRadiantVariant
                _isRadiant.value = character.hasRadiantVariant
                _level.value = character.level?.toString() ?: ""
                _maxHp.value = character.maxHp?.toString() ?: ""
                _existingFrontImageFile.value = withContext(Dispatchers.IO) { repository.getCharacterFrontImageFile(characterId) }
                _existingBackImageFile.value = withContext(Dispatchers.IO) { repository.getCharacterBackImageFile(characterId) }
                _existingRadiantFrontImageFile.value = withContext(Dispatchers.IO) { repository.getCharacterRadiantFrontImageFile(characterId) }
                _existingRadiantBackImageFile.value = withContext(Dispatchers.IO) { repository.getCharacterRadiantBackImageFile(characterId) }
                _isLimitReached.value = false // Can always edit even if limit reached

                val reference = CharacterReference(CustomCharacterRepository.CUSTOM_PACK_ID, characterId)
                _isAssignedToPlayer.value = assignmentRepository.isCharacterAssignedToPlayer(reference)
                _isAssignedToContact.value = assignmentRepository.isCharacterAssignedToAnyContact(reference)
            }
        }
    }

    fun setPreferredAssignmentTarget(target: CharacterAssignmentTarget?) {
        preferredAssignmentTarget = target
    }

    fun onNameChanged(newName: String) {
        if (newName.length <= CharacterPackValidator.MaxNameLength) {
            _name.value = newName
        }
    }

    fun onTypeChanged(newType: CharacterType) {
        _type.value = newType
        if (newType != CharacterType.Monster) {
            _isRadiant.value = false
        }
    }

    fun onRadiantChanged(isRadiant: Boolean) {
        if (isRadiant) isLegacyRadiant = false
        _isRadiant.value = isRadiant
    }

    fun onLevelChanged(newLevel: String) {
        if (newLevel.isEmpty() || newLevel.toIntOrNull() != null) {
            _level.value = newLevel
        }
    }

    fun onMaxHpChanged(newMaxHp: String) {
        if (newMaxHp.isEmpty() || newMaxHp.toIntOrNull() != null) {
            _maxHp.value = newMaxHp
        }
    }

    fun onFrontImageSelected(uri: Uri?) {
        _frontImageUri.value = uri
    }

    fun onBackImageSelected(uri: Uri?) {
        _backImageUri.value = uri
    }

    fun onRadiantFrontImageSelected(uri: Uri?) {
        _radiantFrontImageUri.value = uri
    }

    fun onRadiantBackImageSelected(uri: Uri?) {
        _radiantBackImageUri.value = uri
    }

    fun clearFrontImage() {
        _frontImageUri.value = null
        _existingFrontImageFile.value = null
    }

    fun clearBackImage() {
        _backImageUri.value = null
        _existingBackImageFile.value = null
    }

    fun clearRadiantFrontImage() {
        _radiantFrontImageUri.value = null
        _existingRadiantFrontImageFile.value = null
    }

    fun clearRadiantBackImage() {
        _radiantBackImageUri.value = null
        _existingRadiantBackImageFile.value = null
    }

    fun save() {
        val currentName = _name.value
        val currentType = _type.value
        val currentIsRadiant = _isRadiant.value
        val currentLevel = _level.value.toIntOrNull()
        val currentMaxHp = _maxHp.value.toIntOrNull()
        val frontImage = _frontImageUri.value
        val backImage = _backImageUri.value
        val radiantFrontImage = _radiantFrontImageUri.value
        val radiantBackImage = _radiantBackImageUri.value
        val existingFront = _existingFrontImageFile.value
        val existingBack = _existingBackImageFile.value
        val existingRadiantFront = _existingRadiantFrontImageFile.value
        val existingRadiantBack = _existingRadiantBackImageFile.value
        val characterId = editingCharacterId

        val hasFront = frontImage != null || existingFront != null
        val hasBack = backImage != null || existingBack != null
        val hasRadiantFront = radiantFrontImage != null || existingRadiantFront != null
        val hasRadiantBack = radiantBackImage != null || existingRadiantBack != null
        val hasCompleteRadiantVariant = !currentIsRadiant ||
            (!hasFront || hasRadiantFront) && (!hasBack || hasRadiantBack)

        // Validation based on preferred target
        val isValid = when (preferredAssignmentTarget) {
            CharacterAssignmentTarget.Contact -> hasFront
            CharacterAssignmentTarget.Player -> hasBack
            else -> hasFront || hasBack
        }

        if (currentName.isBlank() || !isValid || !hasCompleteRadiantVariant) return

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                if (characterId != null) {
                    repository.updateCharacter(
                        characterId = characterId,
                        name = currentName,
                        frontImageUri = frontImage,
                        keepExistingFront = existingFront != null,
                        backImageUri = backImage,
                        keepExistingBack = existingBack != null,
                        radiantFrontImageUri = radiantFrontImage,
                        keepExistingRadiantFront = currentIsRadiant && existingRadiantFront != null,
                        radiantBackImageUri = radiantBackImage,
                        keepExistingRadiantBack = currentIsRadiant && existingRadiantBack != null,
                        isRadiant = isLegacyRadiant,
                        level = currentLevel,
                        maxHp = currentMaxHp
                    )
                    _creationResult.value = CharacterReference(CustomCharacterRepository.CUSTOM_PACK_ID, characterId)
                } else {
                    val result = repository.addCharacter(
                        name = currentName,
                        type = currentType,
                        frontImageUri = frontImage,
                        backImageUri = backImage,
                        radiantFrontImageUri = radiantFrontImage.takeIf { currentIsRadiant },
                        radiantBackImageUri = radiantBackImage.takeIf { currentIsRadiant },
                        level = currentLevel,
                        maxHp = currentMaxHp,
                    )
                    _creationResult.value = result
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
