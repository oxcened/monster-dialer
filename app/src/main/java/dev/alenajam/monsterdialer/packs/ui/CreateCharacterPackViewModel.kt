package dev.alenajam.monsterdialer.packs.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.packs.data.CharacterPackExportRepository
import dev.alenajam.monsterdialer.packs.data.CharacterPackExportRequest
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CreateCharacterPackViewModel @Inject constructor(
    private val customCharacters: CustomCharacterRepository,
    private val exporter: CharacterPackExportRepository,
) : ViewModel() {
    private val idSuffix = UUID.randomUUID().toString().substringBefore('-')
    val characters = customCharacters.getExportableCharacters()
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()
    private val _sharedFile = MutableStateFlow<Uri?>(null)
    val sharedFile: StateFlow<Uri?> = _sharedFile.asStateFlow()

    fun toggle(characterId: String) {
        _selectedIds.value = _selectedIds.value.let { selected ->
            if (characterId in selected) selected - characterId else selected + characterId
        }
    }

    fun packIdFor(name: String): String {
        val slug = name
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(55)
            .trimEnd('-', '.', '_')
            .ifBlank { "character-pack" }
        return "$slug-$idSuffix".take(64)
    }

    fun export(request: CharacterPackExportRequest, destination: Uri, context: Context) {
        viewModelScope.launch {
            context.contentResolver.openOutputStream(destination)?.use { exporter.export(request, it) }
        }
    }

    fun exportForSharing(request: CharacterPackExportRequest, context: Context, fileName: String) {
        viewModelScope.launch {
            val directory = File(context.cacheDir, "shared-packs").apply { mkdirs() }
            val file = File(directory, fileName.replace(Regex("[/\\\\]"), "_"))
            file.outputStream().use { exporter.export(request, it) }
            _sharedFile.value = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    fun consumeSharedFile() { _sharedFile.value = null }
}
