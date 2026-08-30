package dev.alenajam.monsterdialer.characters.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackValidator
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddCharacterViewModel @Inject constructor(
    private val repository: CustomCharacterRepository
) : ViewModel() {

    private var editingCharacterId: String? = null
    private var preferredAssignmentTarget: CharacterAssignmentTarget? = null

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _type = MutableStateFlow(CharacterType.Monster)
    val type = _type.asStateFlow()

    private val _frontImageUri = MutableStateFlow<Uri?>(null)
    val frontImageUri = _frontImageUri.asStateFlow()

    private val _backImageUri = MutableStateFlow<Uri?>(null)
    val backImageUri = _backImageUri.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _isLimitReached = MutableStateFlow(false)
    val isLimitReached = _isLimitReached.asStateFlow()

    private val _creationResult = MutableStateFlow<CharacterReference?>(null)
    val creationResult = _creationResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _existingFrontImageFile = MutableStateFlow<java.io.File?>(null)
    val existingFrontImageFile = _existingFrontImageFile.asStateFlow()

    private val _existingBackImageFile = MutableStateFlow<java.io.File?>(null)
    val existingBackImageFile = _existingBackImageFile.asStateFlow()

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
                _existingFrontImageFile.value = withContext(Dispatchers.IO) { repository.getCharacterFrontImageFile(characterId) }
                _existingBackImageFile.value = withContext(Dispatchers.IO) { repository.getCharacterBackImageFile(characterId) }
                _isLimitReached.value = false // Can always edit even if limit reached
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
    }

    fun onFrontImageSelected(uri: Uri?) {
        _frontImageUri.value = uri
    }

    fun onBackImageSelected(uri: Uri?) {
        _backImageUri.value = uri
    }

    fun clearFrontImage() {
        _frontImageUri.value = null
        _existingFrontImageFile.value = null
    }

    fun clearBackImage() {
        _backImageUri.value = null
        _existingBackImageFile.value = null
    }

    fun save() {
        val currentName = _name.value
        val currentType = _type.value
        val frontImage = _frontImageUri.value
        val backImage = _backImageUri.value
        val existingFront = _existingFrontImageFile.value
        val existingBack = _existingBackImageFile.value
        val characterId = editingCharacterId

        val hasFront = frontImage != null || existingFront != null
        val hasBack = backImage != null || existingBack != null

        // Validation based on preferred target
        val isValid = when (preferredAssignmentTarget) {
            CharacterAssignmentTarget.Contact -> hasFront
            CharacterAssignmentTarget.Player -> hasBack
            else -> hasFront || hasBack
        }

        if (currentName.isBlank() || !isValid) return

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                if (characterId != null) {
                    repository.updateCharacter(characterId, currentName, frontImage, backImage)
                    _creationResult.value = CharacterReference(CustomCharacterRepository.CUSTOM_PACK_ID, characterId)
                } else {
                    val result = repository.addCharacter(currentName, currentType, frontImage, backImage)
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
