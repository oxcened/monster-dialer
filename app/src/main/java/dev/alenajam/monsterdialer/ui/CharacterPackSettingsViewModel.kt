package dev.alenajam.monsterdialer.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alenajam.monsterdialer.data.characters.CharactersRepository
import dev.alenajam.monsterdialer.data.characters.MonsterPack
import dev.alenajam.monsterdialer.data.characters.PacksRepository
import dev.alenajam.monsterdialer.data.characters.PacksRepositoryImpl
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackImportDiagnostic
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    var message by mutableStateOf<String?>(null)
        private set

    var importDiagnostic by mutableStateOf<CharacterPackImportDiagnostic?>(null)
        private set

    fun importPack(context: Context, uri: Uri) {
        val fileName = context.displayNameFor(uri)
        viewModelScope.launch {
            val result = (packsRepository as PacksRepositoryImpl).importPackFromUri(uri)
            result.onSuccess {
                message = null
            }.onFailure { error ->
                importDiagnostic = CharacterPackImportDiagnostic.from(fileName, error)
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
            message = if (result.isSuccess) successMessage else failureMessage
        }
    }

    fun dismissDiagnostic() {
        importDiagnostic = null
    }

    fun getPreviewCharacter(packId: String, packName: String): InstalledPackCharacter? {
        return charactersRepository.getCharactersAssignableTo(CharacterAssignmentTarget.Contact)
            .firstOrNull { it.packId == packId }
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
