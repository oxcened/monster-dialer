package dev.alenajam.monsterdialer.characters.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCharacterViewModel @Inject constructor(
    private val repository: CustomCharacterRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _type = MutableStateFlow(CharacterType.Monster)
    val type = _type.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _creationResult = MutableStateFlow<CharacterReference?>(null)
    val creationResult = _creationResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun onNameChanged(newName: String) {
        _name.value = newName
    }

    fun onTypeChanged(newType: CharacterType) {
        _type.value = newType
    }

    fun onImageSelected(uri: Uri?) {
        _imageUri.value = uri
    }

    fun save() {
        val currentName = _name.value
        val currentType = _type.value
        val currentImage = _imageUri.value

        if (currentName.isBlank() || currentImage == null) return

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                val result = repository.addCharacter(currentName, currentType, currentImage)
                _creationResult.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
