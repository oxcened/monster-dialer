package dev.alenajam.monsterdialer.packs.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.MonsterPack
import dev.alenajam.monsterdialer.packs.data.PacksRepository
import dev.alenajam.monsterdialer.packs.data.PacksRepositoryImpl
import dev.alenajam.monsterdialer.packs.data.CharacterPackImportDiagnostic
import dev.alenajam.monsterdialer.packs.data.CharacterPackArchive
import dev.alenajam.monsterdialer.packs.data.CharacterPackPreview
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
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
class CharacterPackSettingsViewModel @Inject constructor(
    private val packsRepository: PacksRepository,
    private val charactersRepository: CharactersRepository
) : ViewModel() {

    data class ImportPreview(
        val uri: Uri,
        val fileName: String,
        val pack: CharacterPackPreview,
    )

    val packs: StateFlow<List<MonsterPack>> = packsRepository.getPacks()
        .map { packs -> packs.filter { it.id != CustomCharacterRepository.CUSTOM_PACK_ID } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canCreatePack: StateFlow<Boolean> = packsRepository.getPacks()
        .map { packs -> packs.any { it.id == CustomCharacterRepository.CUSTOM_PACK_ID && it.characterCount > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importDiagnostic = MutableStateFlow<CharacterPackImportDiagnostic?>(null)
    val importDiagnostic: StateFlow<CharacterPackImportDiagnostic?> = _importDiagnostic.asStateFlow()
    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    fun previewPack(context: Context, uri: Uri) {
        val fileName = context.displayNameFor(uri)
        if (!CharacterPackArchive.hasSupportedExtension(fileName)) {
            _importDiagnostic.value = CharacterPackImportDiagnostic.from(
                fileName,
                IllegalArgumentException(context.getString(R.string.character_pack_extension_required))
            )
            return
        }
        viewModelScope.launch {
            runCatching { (packsRepository as PacksRepositoryImpl).previewPackFromUri(uri) }
                .onSuccess { pack -> _importPreview.value = ImportPreview(uri, fileName, pack) }
                .onFailure { error ->
                _importDiagnostic.value = CharacterPackImportDiagnostic.from(fileName, error)
            }
        }
    }

    fun importPreview() {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            val result = (packsRepository as PacksRepositoryImpl).importPackFromUri(preview.uri)
            result.onSuccess {
                _message.value = null
                _importPreview.value = null
            }.onFailure { error ->
                _importDiagnostic.value = CharacterPackImportDiagnostic.from(preview.fileName, error)
            }
        }
    }

    fun dismissPreview() { _importPreview.value = null }

    fun togglePack(packId: String, enabled: Boolean) {
        viewModelScope.launch {
            packsRepository.togglePack(packId, enabled)
        }
    }

    suspend fun isPackInUse(packId: String): Boolean {
        return charactersRepository.isPackInUse(packId)
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

    fun dismissMessage() {
        _message.value = null
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
