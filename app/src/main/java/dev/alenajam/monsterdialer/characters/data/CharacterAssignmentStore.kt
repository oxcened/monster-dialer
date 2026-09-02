package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterPackValidationException
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CharacterAssignmentsDocument(
    /** Legacy v1 monster assignment. */
    val player: CharacterReference? = null,
    /** Legacy v1 monster assignments. */
    val contacts: Map<String, CharacterReference> = emptyMap(),
    val playerByType: Map<CharacterType, CharacterReference> = emptyMap(),
    val playerMonsterRoster: List<CharacterReference> = emptyList(),
    val activePlayerMonster: CharacterReference? = null,
    val contactsByType: Map<String, Map<CharacterType, CharacterReference>> = emptyMap(),
    val contactLabels: Map<String, String> = emptyMap(),
    val selectedContact: StoredSelectedContact? = null
)

@Serializable
private data class StoredSelectedContact(
    val label: String,
    val contactKeys: List<String>,
    val contactId: Int? = null,
    val photoUri: String? = null
)

data class ContactCharacterAssignment(
    val contactKey: String,
    val label: String,
    val type: CharacterType,
    val character: CharacterReference
)

data class SelectedContact(
    val label: String,
    val contactKeys: List<String>,
    val contactId: Int?,
    val photoUri: String?
)

/** Persists the user's selected player and contact mappings without putting personal data in packs. */
class CharacterAssignmentStore(
    private val storageRoot: File,
    private val json: Json = Json { ignoreUnknownKeys = false; explicitNulls = false }
) {
    @Synchronized
    fun player(type: CharacterType): CharacterReference? {
        val document = read()
        if (type == CharacterType.Monster) {
            return activeMonster(document)
        }
        return document.playerByType[type] ?: document.player.takeIf { type == CharacterType.Monster }
    }

    fun player(): CharacterReference? = player(CharacterType.Monster)

    @Synchronized
    fun setPlayer(type: CharacterType, character: CharacterReference?) {
        character?.validate()
        val document = read()
        if (type == CharacterType.Monster) {
            val active = activeMonster(document)
            val roster = character?.let { selected ->
                when {
                    active == null || selected == active -> benchRoster(document)
                    else -> benchRoster(document).filterNot { it == selected }.plus(selected).take(MaxBenchSize)
                }
            }.orEmpty()
            write(document.copy(
                player = null,
                playerByType = document.playerByType - CharacterType.Monster,
                playerMonsterRoster = roster,
                activePlayerMonster = character?.let { active ?: it },
            ))
            return
        }
        val updated = document.playerByType.toMutableMap().apply {
            if (character == null) remove(type) else put(type, character)
        }
        write(document.copy(
            player = if (type == CharacterType.Monster) null else document.player,
            playerByType = updated
        ))
    }

    fun setPlayer(character: CharacterReference?) = setPlayer(CharacterType.Monster, character)

    @Synchronized
    fun playerMonsterRoster(): List<CharacterReference> {
        return benchRoster(read())
    }

    @Synchronized
    fun setActivePlayerMonster(character: CharacterReference) {
        character.validate()
        val document = read()
        val active = activeMonster(document)
        val roster = benchRoster(document)
        val selectedIndex = roster.indexOf(character)
        require(character == active || selectedIndex >= 0) { "Monster is not in the roster" }
        val updatedRoster = if (character == active) {
            roster
        } else {
            roster.toMutableList().apply {
                removeAt(selectedIndex)
                active?.let { add(selectedIndex, it) }
            }
        }
        write(document.copy(
            player = null,
            playerByType = document.playerByType - CharacterType.Monster,
            playerMonsterRoster = updatedRoster,
            activePlayerMonster = character,
        ))
    }

    @Synchronized
    fun addPlayerMonsterToRoster(character: CharacterReference) {
        character.validate()
        val document = read()
        val active = activeMonster(document)
        val roster = benchRoster(document)
        if (character == active || character in roster) return
        if (active == null) {
            write(document.copy(
                player = null,
                playerByType = document.playerByType - CharacterType.Monster,
                activePlayerMonster = character,
                playerMonsterRoster = emptyList(),
            ))
        } else {
            require(roster.size < MaxBenchSize) { "Roster is full" }
            setPlayerMonsterRoster(roster + character)
        }
    }

    @Synchronized
    fun setPlayerMonsterRoster(roster: List<CharacterReference>) {
        require(roster.size <= MaxBenchSize) { "Roster is too large" }
        require(roster.distinct().size == roster.size) { "Roster contains duplicate monsters" }
        roster.forEach { reference -> reference.validate() }
        val document = read()
        val active = activeMonster(document)
        require(active !in roster) { "Active monster cannot also be in the roster" }
        write(document.copy(
            player = null,
            playerByType = document.playerByType - CharacterType.Monster,
            playerMonsterRoster = roster,
            activePlayerMonster = active,
        ))
    }

    @Synchronized
    fun removePlayerMonsterFromRoster(character: CharacterReference) {
        setPlayerMonsterRoster(playerMonsterRoster().filterNot { it == character })
    }

    @Synchronized
    fun characterForContact(contactKey: String, type: CharacterType): CharacterReference? {
        val document = read()
        val normalizedKey = normalizeContactKeyOrNull(contactKey) ?: return null
        return document.contactsByType[normalizedKey]?.get(type)
            ?: document.contacts[normalizedKey].takeIf { type == CharacterType.Monster }
    }

    fun characterForContact(contactKey: String): CharacterReference? =
        characterForContact(contactKey, CharacterType.Monster)

    @Synchronized
    fun contactAssignments(): List<ContactCharacterAssignment> {
        val document = read()
        val typed = document.contactsByType.flatMap { (contactKey, assignments) ->
            assignments.map { (type, character) ->
                ContactCharacterAssignment(
                    contactKey = contactKey,
                    label = document.contactLabels[contactKey] ?: contactKey,
                    type = type,
                    character = character
                )
            }
        }.toMutableList()
        document.contacts.forEach { (contactKey, character) ->
            if (typed.none { it.contactKey == contactKey && it.type == CharacterType.Monster }) {
                typed += ContactCharacterAssignment(
                    contactKey = contactKey,
                    label = document.contactLabels[contactKey] ?: contactKey,
                    type = CharacterType.Monster,
                    character = character
                )
            }
        }
        return typed.sortedWith(compareBy({ it.label.lowercase() }, { it.type.name }))
    }

    @Synchronized
    fun assignedContactCount(): Int = contactAssignments()
        .map { it.label }
        .distinct()
        .size

    @Synchronized
    fun selectedContact(): SelectedContact? = read().selectedContact?.let { selected ->
        SelectedContact(selected.label, selected.contactKeys, selected.contactId, selected.photoUri)
    }

    @Synchronized
    fun setSelectedContact(
        label: String,
        contactKeys: List<String>,
        contactId: Int? = null,
        photoUri: String? = null
    ) {
        val normalizedKeys = contactKeys.map(::normalizeContactKey).distinct()
        require(normalizedKeys.isNotEmpty()) { "Selected contact must have a phone number" }
        val selected = StoredSelectedContact(
            label = label.trim().ifBlank { normalizedKeys.first() }.take(MaxContactLabelLength),
            contactKeys = normalizedKeys,
            contactId = contactId,
            photoUri = photoUri
        )
        val document = read()
        write(document.copy(selectedContact = selected))
    }

    @Synchronized
    fun clearSelectedContact() {
        val document = read()
        if (document.selectedContact != null) write(document.copy(selectedContact = null))
    }

    @Synchronized
    fun assignContact(
        contactKey: String,
        type: CharacterType,
        character: CharacterReference?,
        label: String? = null
    ) {
        val normalizedKey = normalizeContactKey(contactKey)
        character?.validate()
        val document = read()
        val assignmentsForContact = document.contactsByType[normalizedKey].orEmpty().toMutableMap().apply {
            if (character == null) remove(type) else put(type, character)
        }
        val updated = document.contactsByType.toMutableMap().apply {
            if (assignmentsForContact.isEmpty()) remove(normalizedKey) else put(normalizedKey, assignmentsForContact)
        }
        val legacyContacts = document.contacts.toMutableMap().apply {
            if (type == CharacterType.Monster) remove(normalizedKey)
        }
        val updatedLabels = document.contactLabels.toMutableMap().apply {
            if (character == null && assignmentsForContact.isEmpty() && normalizedKey !in legacyContacts) {
                remove(normalizedKey)
            } else if (!label.isNullOrBlank()) {
                put(normalizedKey, label.trim().take(MaxContactLabelLength))
            }
        }
        write(document.copy(
            contacts = legacyContacts,
            contactsByType = updated,
            contactLabels = updatedLabels
        ))
    }

    fun assignContact(contactKey: String, character: CharacterReference?, label: String? = null) =
        assignContact(contactKey, CharacterType.Monster, character, label)

    @Synchronized
    fun clearAssignmentsForPack(packId: String) {
        val document = read()
        val updatedPlayerByType = document.playerByType.filterValues { it.packId != packId }
        val updatedRoster = benchRoster(document).filter { it.packId != packId }
        val updatedActiveMonster = activeMonster(document)?.takeIf { it.packId != packId }
            ?: updatedRoster.firstOrNull()
        val updatedBench = updatedRoster.filterNot { it == updatedActiveMonster }
        
        val updatedContactsByType = document.contactsByType.mapValues { (_, assignments) ->
            assignments.filterValues { it.packId != packId }
        }.filterValues { it.isNotEmpty() }

        val legacyContacts = document.contacts.filterValues { it.packId != packId }
        
        val updatedLabels = document.contactLabels.filterKeys { key ->
            key in updatedContactsByType || key in legacyContacts
        }

        write(document.copy(
            player = if (document.player?.packId == packId) null else document.player,
            contacts = legacyContacts,
            playerByType = updatedPlayerByType,
            playerMonsterRoster = updatedBench,
            activePlayerMonster = updatedActiveMonster,
            contactsByType = updatedContactsByType,
            contactLabels = updatedLabels
        ))
    }

    @Synchronized
    fun clearAssignmentsForCharacter(reference: CharacterReference) {
        val document = read()
        val updatedPlayerByType = document.playerByType.filterValues { !it.sameCharacterAs(reference) }
        val updatedRoster = benchRoster(document).filter { !it.sameCharacterAs(reference) }
        val updatedActiveMonster = activeMonster(document)?.takeUnless { it.sameCharacterAs(reference) }
            ?: updatedRoster.firstOrNull()
        val updatedBench = updatedRoster.filterNot { it == updatedActiveMonster }
        
        val updatedContactsByType = document.contactsByType.mapValues { (_, assignments) ->
            assignments.filterValues { !it.sameCharacterAs(reference) }
        }.filterValues { it.isNotEmpty() }

        val legacyContacts = document.contacts.filterValues { !it.sameCharacterAs(reference) }
        
        val updatedLabels = document.contactLabels.filterKeys { key ->
            key in updatedContactsByType || key in legacyContacts
        }

        write(document.copy(
            player = if (document.player?.sameCharacterAs(reference) == true) null else document.player,
            contacts = legacyContacts,
            playerByType = updatedPlayerByType,
            playerMonsterRoster = updatedBench,
            activePlayerMonster = updatedActiveMonster,
            contactsByType = updatedContactsByType,
            contactLabels = updatedLabels
        ))
    }

    private fun CharacterReference.validate() {
        require(packId.isNotBlank() && packId.length <= MaxIdentifierLength) { "Pack id is invalid" }
        require(characterId.isNotBlank() && characterId.length <= MaxIdentifierLength) { "Character id is invalid" }
    }

    private fun CharacterReference.sameCharacterAs(other: CharacterReference): Boolean =
        packId == other.packId && characterId == other.characterId

    /** Converts the released single-monster assignment and the earlier roster format on read. */
    private fun activeMonster(document: CharacterAssignmentsDocument): CharacterReference? =
        document.activePlayerMonster
            ?: document.playerByType[CharacterType.Monster]
            ?: document.player
            ?: document.playerMonsterRoster.firstOrNull()

    /** The roster holds the team members that are not currently active. */
    private fun benchRoster(document: CharacterAssignmentsDocument): List<CharacterReference> =
        document.playerMonsterRoster
            .filterNot { it == activeMonster(document) }
            .distinct()
            .take(MaxBenchSize)

    private fun normalizeContactKey(value: String): String {
        return requireNotNull(normalizeContactKeyOrNull(value)) { "Contact key is invalid" }
    }

    /**
     * Incoming call providers can expose labels such as "Private number" instead of a phone
     * number. Those values cannot have an assignment and must not crash the in-call UI.
     */
    private fun normalizeContactKeyOrNull(value: String): String? {
        val trimmed = value.trim()
        val normalized = buildString {
            trimmed.forEachIndexed { index, character ->
                if (character.isDigit() || (character == '+' && index == 0)) append(character)
            }
        }
        return normalized.takeIf { it.isNotBlank() && it.length <= MaxContactKeyLength }
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
        const val MaxRosterSize = 6
        const val MaxBenchSize = MaxRosterSize - 1
        const val FileName = "character-assignments.json"
        const val MaxIdentifierLength = 128
        const val MaxContactKeyLength = 512
        const val MaxContactLabelLength = 120
    }
}
