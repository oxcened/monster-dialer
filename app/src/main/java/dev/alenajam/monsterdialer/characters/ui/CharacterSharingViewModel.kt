package dev.alenajam.monsterdialer.characters.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.CharacterSharingRepository
import dev.alenajam.monsterdialer.characters.data.SharedCharacterImport
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

    fun export(context: Context, characterId: String, creator: String, license: String, destination: Uri) {
        viewModelScope.launch {
            context.contentResolver.openOutputStream(destination)?.use {
                repository.export(characterId, creator, license, it)
            }
        }
    }

    fun preview(context: Context, source: Uri) {
        viewModelScope.launch {
            _preview.value = context.contentResolver.openInputStream(source)?.use(repository::preview)
        }
    }

    fun importPreview() {
        _preview.value?.let { shared -> viewModelScope.launch { repository.import(shared); _preview.value = null } }
    }

    fun dismissPreview() { _preview.value = null }
}
