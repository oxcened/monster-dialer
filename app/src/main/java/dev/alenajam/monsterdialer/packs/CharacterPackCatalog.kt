package dev.alenajam.monsterdialer.packs

import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class InstalledCharacterPackRecord(
    val id: String,
    val name: String,
    val version: String,
    val creator: String? = null,
    val license: String,
    val characterCount: Int,
    val enabled: Boolean = true,
    val installedAtMillis: Long
)

@Serializable
private data class CharacterPackCatalogDocument(
    val packs: List<InstalledCharacterPackRecord> = emptyList()
)

/** A small, atomic catalog for packs already copied to app-private storage. */
class CharacterPackCatalog(
    private val storageRoot: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val json: Json = Json { ignoreUnknownKeys = false; explicitNulls = false }
) {
    @Synchronized
    fun list(): List<InstalledCharacterPackRecord> = read().packs.sortedBy { it.name.lowercase() }

    @Synchronized
    fun recordInstallation(manifest: CharacterPackManifest): InstalledCharacterPackRecord {
        val existing = read().packs
        val previous = existing.firstOrNull { it.id == manifest.id }
        val record = InstalledCharacterPackRecord(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            creator = manifest.creator,
            license = manifest.license,
            characterCount = manifest.characters.size,
            enabled = previous?.enabled ?: true,
            installedAtMillis = clock()
        )
        write(CharacterPackCatalogDocument(existing.filterNot { it.id == manifest.id } + record))
        return record
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) {
        val document = read()
        val updated = document.packs.map { pack -> if (pack.id == id) pack.copy(enabled = enabled) else pack }
        if (updated == document.packs) throw CharacterPackValidationException("Pack is not installed")
        write(CharacterPackCatalogDocument(updated))
    }

    @Synchronized
    fun remove(id: String) {
        val document = read()
        val updated = document.packs.filterNot { it.id == id }
        if (updated.size == document.packs.size) throw CharacterPackValidationException("Pack is not installed")
        write(CharacterPackCatalogDocument(updated))
    }

    private fun read(): CharacterPackCatalogDocument {
        val file = File(storageRoot, CatalogFileName)
        if (!file.exists()) return CharacterPackCatalogDocument()
        return try {
            json.decodeFromString<CharacterPackCatalogDocument>(file.readText())
        } catch (exception: Exception) {
            throw CharacterPackValidationException("Installed pack catalog is unreadable: ${exception.message}")
        }
    }

    private fun write(document: CharacterPackCatalogDocument) {
        storageRoot.mkdirs()
        val destination = File(storageRoot, CatalogFileName)
        val temporary = File(storageRoot, ".$CatalogFileName-${UUID.randomUUID()}")
        val backup = File(storageRoot, ".$CatalogFileName-backup-${UUID.randomUUID()}")
        try {
            temporary.writeText(json.encodeToString(document))
            if (destination.exists() && !destination.renameTo(backup)) fail("Could not update installed pack catalog")
            if (!temporary.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                fail("Could not update installed pack catalog")
            }
            backup.delete()
        } finally {
            temporary.delete()
        }
    }

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private companion object {
        const val CatalogFileName = "catalog.json"
    }
}
