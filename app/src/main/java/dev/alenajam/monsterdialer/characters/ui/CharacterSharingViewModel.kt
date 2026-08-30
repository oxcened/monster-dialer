package dev.alenajam.monsterdialer.characters.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.CharacterSharingRepository
import dev.alenajam.monsterdialer.characters.data.SharedCharacterImport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterSharingViewModel @Inject constructor(
    private val repository: CharacterSharingRepository
) : ViewModel() {
    private val _preview = MutableStateFlow<SharedCharacterImport?>(null)
    val preview: StateFlow<SharedCharacterImport?> = _preview.asStateFlow()
    private val _hasImportError = MutableStateFlow(false)
    val hasImportError: StateFlow<Boolean> = _hasImportError.asStateFlow()

    fun export(context: Context, characterId: String, creator: String, license: String, destination: Uri) {
        viewModelScope.launch {
            context.contentResolver.openOutputStream(destination)?.use {
                repository.export(characterId, creator, license, it)
            }
        }
    }

    fun preview(context: Context, source: Uri) {
        viewModelScope.launch {
            try {
                _preview.value = context.contentResolver.openInputStream(source)?.use(repository::preview)
                _hasImportError.value = _preview.value == null
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _preview.value = null
                _hasImportError.value = true
            }
        }
    }

    fun importPreview() {
        _preview.value?.let { shared -> viewModelScope.launch { repository.import(shared); _preview.value = null } }
    }

    fun dismissPreview() { _preview.value = null }

    fun dismissImportError() { _hasImportError.value = false }
}
