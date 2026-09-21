package dev.alenajam.monsterdialer.packs.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CatalogSource(val url: String, val addedAtMillis: Long)

@Serializable
private data class CatalogSourcesDocument(val sources: List<CatalogSource> = emptyList())

/** Persists only catalog addresses; downloaded catalogs and packs are never treated as trusted. */
@Singleton
class CatalogSourceRepository @Inject constructor(
    @CharacterPacksDir private val storageRoot: File,
) {
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    private val _sources = MutableStateFlow(read().sources)
    val sources: StateFlow<List<CatalogSource>> = _sources.asStateFlow()

    @Synchronized
    fun add(url: String) {
        val canonicalUrl = CatalogUrlPolicy.requireHttps(url.trim()).toString()
        if (_sources.value.any { it.url == canonicalUrl }) {
            throw CharacterPackValidationException("Catalog is already added")
        }
        val updated = _sources.value + CatalogSource(canonicalUrl, System.currentTimeMillis())
        write(CatalogSourcesDocument(updated))
        _sources.value = updated
    }

    @Synchronized
    fun remove(url: String) {
        val updated = _sources.value.filterNot { it.url == url }
        if (updated.size == _sources.value.size) throw CharacterPackValidationException("Catalog is not added")
        write(CatalogSourcesDocument(updated))
        _sources.value = updated
    }

    private fun read(): CatalogSourcesDocument {
        val file = File(storageRoot, FileName)
        if (!file.isFile) return CatalogSourcesDocument()
        return try {
            json.decodeFromString<CatalogSourcesDocument>(file.readText()).also { document ->
                document.sources.forEach { CatalogUrlPolicy.requireHttps(it.url) }
            }
        } catch (exception: CharacterPackValidationException) {
            throw exception
        } catch (exception: Exception) {
            throw CharacterPackValidationException("Catalog sources are unreadable: ${exception.message}")
        }
    }

    private fun write(document: CatalogSourcesDocument) {
        storageRoot.mkdirs()
        val destination = File(storageRoot, FileName)
        val temporary = File(storageRoot, ".$FileName-${UUID.randomUUID()}")
        try {
            temporary.writeText(json.encodeToString(document))
            if (!temporary.renameTo(destination)) throw CharacterPackValidationException("Could not update catalog sources")
        } finally {
            temporary.delete()
        }
    }

    private companion object { const val FileName = "catalog-sources.json" }
}
