package dev.alenajam.monsterdialer.packs.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.MonsterPack
import dev.alenajam.monsterdialer.packs.data.PacksRepository
import dev.alenajam.monsterdialer.packs.data.PacksRepositoryImpl
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackImportDiagnostic
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class CharacterPackSettingsViewModel @Inject constructor(
    private val packsRepository: PacksRepository,
    private val charactersRepository: CharactersRepository
) : ViewModel() {

    val packs: StateFlow<List<MonsterPack>> = packsRepository.getPacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importDiagnostic = MutableStateFlow<CharacterPackImportDiagnostic?>(null)
    val importDiagnostic: StateFlow<CharacterPackImportDiagnostic?> = _importDiagnostic.asStateFlow()

    fun importPack(context: Context, uri: Uri) {
        val fileName = context.displayNameFor(uri)
        viewModelScope.launch {
            val result = (packsRepository as PacksRepositoryImpl).importPackFromUri(uri)
            result.onSuccess {
                _message.value = null
            }.onFailure { error ->
                _importDiagnostic.value = CharacterPackImportDiagnostic.from(fileName, error)
            }
        }
    }

    fun togglePack(packId: String, enabled: Boolean) {
        viewModelScope.launch {
            packsRepository.togglePack(packId, enabled)
        }
    }

    fun deletePack(packId: String, successMessage: String, failureMessage: String) {
        viewModelScope.launch {
            val result = runCatching { packsRepository.deletePack(packId) }
            _message.value = if (result.isSuccess) successMessage else failureMessage
        }
    }

    fun dismissDiagnostic() {
        _importDiagnostic.value = null
    }

    fun getPreviewCharacter(packId: String, packName: String): InstalledPackCharacter? {
        return charactersRepository.getCharactersInPack(packId, packName).firstOrNull()
    }

    private fun Context.displayNameFor(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameColumn)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
    }
}
