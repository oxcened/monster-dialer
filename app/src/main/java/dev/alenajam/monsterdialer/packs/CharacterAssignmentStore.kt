package dev.alenajam.monsterdialer.packs

import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CharacterAssignmentsDocument(
    val player: CharacterReference? = null,
    val contacts: Map<String, CharacterReference> = emptyMap()
)

/** Persists the user's selected player and contact mappings without putting personal data in packs. */
class CharacterAssignmentStore(
    private val storageRoot: File,
    private val json: Json = Json { ignoreUnknownKeys = false; explicitNulls = false }
) {
    @Synchronized
    fun player(): CharacterReference? = read().player

    @Synchronized
    fun setPlayer(character: CharacterReference?) {
        character?.validate()
        val document = read()
        write(document.copy(player = character))
    }

    @Synchronized
    fun characterForContact(contactKey: String): CharacterReference? = read().contacts[contactKey]

    @Synchronized
    fun assignContact(contactKey: String, character: CharacterReference?) {
        require(contactKey.isNotBlank() && contactKey.length <= MaxContactKeyLength) { "Contact key is invalid" }
        character?.validate()
        val document = read()
        val updated = document.contacts.toMutableMap().apply {
            if (character == null) remove(contactKey) else put(contactKey, character)
        }
        write(document.copy(contacts = updated))
    }

    private fun CharacterReference.validate() {
        require(packId.isNotBlank() && packId.length <= MaxIdentifierLength) { "Pack id is invalid" }
        require(characterId.isNotBlank() && characterId.length <= MaxIdentifierLength) { "Character id is invalid" }
    }

    private fun read(): CharacterAssignmentsDocument {
        val file = File(storageRoot, FileName)
        if (!file.exists()) return CharacterAssignmentsDocument()
        return try {
            json.decodeFromString<CharacterAssignmentsDocument>(file.readText())
        } catch (exception: Exception) {
            throw CharacterPackValidationException("Character assignments are unreadable: ${exception.message}")
        }
    }

    private fun write(document: CharacterAssignmentsDocument) {
        storageRoot.mkdirs()
        val destination = File(storageRoot, FileName)
        val temporary = File(storageRoot, ".$FileName-${UUID.randomUUID()}")
        val backup = File(storageRoot, ".$FileName-backup-${UUID.randomUUID()}")
        try {
            temporary.writeText(json.encodeToString(document))
            if (destination.exists() && !destination.renameTo(backup)) fail("Could not update character assignments")
            if (!temporary.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                fail("Could not update character assignments")
            }
            backup.delete()
        } finally {
            temporary.delete()
        }
    }

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private companion object {
        const val FileName = "character-assignments.json"
        const val MaxIdentifierLength = 128
        const val MaxContactKeyLength = 512
    }
}
