package dev.alenajam.monsterdialer.packs.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Reads installed, enabled packs and exposes their characters by the roles they permit.
 * Corrupt or incomplete installed packs are ignored so an incoming call never crashes.
 */
class CharacterPackRepository(
    private val storageRoot: File,
    private val catalog: CharacterPackCatalog
) {
    fun observeAssignableCharacters(
        role: CharacterAssignmentTarget,
        type: CharacterType? = null
    ): Flow<List<InstalledPackCharacter>> =
        catalog.packs
            .map { charactersAssignableTo(role, type) }

    fun find(
        reference: CharacterReference,
        role: CharacterAssignmentTarget,
        type: CharacterType
    ): InstalledPackCharacter? =
        charactersAssignableTo(role, type).firstOrNull {
            it.packId == reference.packId && it.character.id == reference.characterId
        }

    fun charactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType? = null
    ): List<InstalledPackCharacter> =
        catalog.list()
            .asSequence()
            .filter { it.enabled }
            .flatMap { record -> readPack(record.id, record.name).asSequence() }
            .filter { role in it.character.assignableTo }
            .filter { type == null || it.character.type == type }
            .sortedWith(compareBy({ it.packName.lowercase() }, { it.character.name.lowercase() }))
            .toList()

    /** Returns a pack's characters for management UIs, whether the pack is enabled or not. */
    fun charactersInPack(packId: String, packName: String): List<InstalledPackCharacter> =
        readPack(packId, packName)

    private fun readPack(packId: String, packName: String): List<InstalledPackCharacter> {
        val directory = File(File(storageRoot, packId), ActiveDirectory)
        val manifestFile = File(directory, CharacterPackValidator.ManifestPath)
        if (!manifestFile.isFile) return emptyList()
        return runCatching {
            val manifest = CharacterPackManifestCodec.decode(manifestFile.readText())
            CharacterPackValidator.validate(manifest)
            if (manifest.id != packId) return emptyList()
            manifest.characters
                .filter { character ->
                    (character.frontImage == null || File(directory, character.frontImage).isFile) &&
                        (character.backImage == null || File(directory, character.backImage).isFile)
                }
                .map { character -> InstalledPackCharacter(packId, packName, character, directory) }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val ActiveDirectory = "active"
    }
}
